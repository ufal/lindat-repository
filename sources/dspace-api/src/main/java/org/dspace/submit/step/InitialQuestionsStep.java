/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

/**
 * Initial Submission servlet for DSpace. Handles the initial questions which
 * are asked to users to gather information regarding what metadata needs to be
 * gathered.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * based on class by Tim Donohue
 * modified for LINDAT/CLARIN
 * @version $Revision$
 */
public class InitialQuestionsStep extends AbstractProcessingStep
{
    /***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     **************************************************************************/
    // pruning of metadata needs to take place
    public static final int STATUS_VERIFY_PRUNE = 1;

    // pruning was cancelled by user
    public static final int STATUS_CANCEL_PRUNE = 2;

    // user attempted to upload a thesis, when theses are not accepted
    public static final int STATUS_THESIS_REJECTED = 3;

    /**
     * Global flags to determine if we need to prune anything
     */
    protected boolean willRemoveTitles = false;

    protected boolean willRemoveDate = false;

    protected boolean willRemoveFiles = false;

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required)
     * <P>
     * It is this method's job to save any data to the underlying database, as
     * necessary, and return error messages (if any) which can then be processed
     * by the appropriate user interface (JSP-UI or XML-UI)
     * <P>
     * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
     * it should perform *all* of its processing in this method!
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @return Status or error flag which will be processed by
     *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Get the values from the initial questions form
        boolean multipleTitles = Util.getBoolParameter(request,
                "multiple_titles");
        boolean publishedBefore = true;/*Util.getBoolParameter(request,
                "published_before");*/
        boolean multipleFiles = Util.getBoolParameter(request,
                "multiple_files");
        boolean isThesis = ConfigurationManager
                .getBooleanProperty("webui.submit.blocktheses")
                && Util.getBoolParameter(request, "is_thesis");

        if (subInfo.isInWorkflow())
        {
            // Thesis question does not appear in workflow mode..
            isThesis = false;

            // Pretend "multiple files" is true in workflow mode
            // (There will always be the license file)
            multipleFiles = true;
        }

        // First and foremost - if it's a thesis, reject the submission
        if (isThesis)
        {
            WorkspaceItem wi = (WorkspaceItem) subInfo.getSubmissionItem();
            wi.deleteAll();
            subInfo.setSubmissionItem(null);

            // Remember that we've removed a thesis in the session
            request.getSession().setAttribute("removed_thesis",
                    Boolean.TRUE);

            return STATUS_THESIS_REJECTED; // since theses are disabled, throw
                                            // an error!
        }

        // Next, check if we are pruning some existing metadata
        if (request.getParameter("do_not_prune") != null)
        {
            return STATUS_CANCEL_PRUNE; // cancelled pruning!
        }
        else if (request.getParameter("prune") != null)
        {
            processVerifyPrune(context, request, response, subInfo,
                    multipleTitles, publishedBefore, multipleFiles);
        }
        else
        // otherwise, check if pruning is necessary
        {
            // Now check to see if the changes will remove any values
            // (i.e. multiple files, titles or an issue date.)

            if (subInfo.getSubmissionItem() != null)
            {
                // shouldn't need to check if submission is null, but just in case!
                if (!multipleTitles)
                {
                    DCValue[] altTitles = subInfo.getSubmissionItem().getItem()
                            .getDC("title", "alternative", Item.ANY);

                    willRemoveTitles = altTitles.length > 0;
                }

                if (!publishedBefore)
                {
                    DCValue[] dateIssued = subInfo.getSubmissionItem().getItem()
                            .getDC("date", "issued", Item.ANY);
                    DCValue[] citation = subInfo.getSubmissionItem().getItem()
                            .getDC("identifier", "citation", Item.ANY);
                    DCValue[] publisher = subInfo.getSubmissionItem().getItem()
                            .getDC("publisher", null, Item.ANY);

                    willRemoveDate = (dateIssued.length > 0)
                            || (citation.length > 0) || (publisher.length > 0);
                }

                if (!multipleFiles)
                {
                    // see if number of bitstreams in "ORIGINAL" bundle > 1
                    // FIXME: Assumes multiple bundles, clean up someday...
                    Bundle[] bundles = subInfo.getSubmissionItem().getItem()
                            .getBundles("ORIGINAL");

                    if (bundles.length > 0)
                    {
                        Bitstream[] bitstreams = bundles[0].getBitstreams();

                        willRemoveFiles = bitstreams.length > 1;
                    }
                }
            }

            // If anything is going to be removed from the item as a result
            // of changing the answer to one of the questions, we need
            // to inform the user and make sure that's OK, before saving!
            if (willRemoveTitles || willRemoveDate || willRemoveFiles)
            {
                //save what we will need to prune to request (for UI to process)
                request.setAttribute("will.remove.titles", Boolean.valueOf(willRemoveTitles));
                request.setAttribute("will.remove.date", Boolean.valueOf(willRemoveDate));
                request.setAttribute("will.remove.files", Boolean.valueOf(willRemoveFiles));
                
                return STATUS_VERIFY_PRUNE; // we will need to do pruning!
            }
        }

        // If step is complete, save the changes
        subInfo.getSubmissionItem().setMultipleTitles(multipleTitles);
        subInfo.getSubmissionItem().setPublishedBefore(publishedBefore);

        // "Multiple files" irrelevant in workflow mode
        if (!subInfo.isInWorkflow())
        {
            subInfo.getSubmissionItem().setMultipleFiles(multipleFiles);
        }

        // commit all changes to DB
        subInfo.getSubmissionItem().update();
        context.commit();

        return STATUS_COMPLETE; // no errors!
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar.
     * <P>
     * This method may just return 1 for most steps (since most steps consist of
     * a single page). But, it should return a number greater than 1 for any
     * "step" which spans across a number of HTML pages. For example, the
     * configurable "Describe" step (configured using input-forms.xml) overrides
     * this method to return the number of pages that are defined by its
     * configuration file.
     * <P>
     * Steps which are non-interactive (i.e. they do not display an interface to
     * the user) should return a value of 1, so that they are only processed
     * once!
     * 
     * @param request
     *            The HTTP Request
     * @param subInfo
     *            The current submission information object
     * 
     * @return the number of pages in this step
     */
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // always just one page of initial questions
        return 1;
    }

    /**
     * Process input from "verify prune" page
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     * @param multipleTitles
     *            if there is multiple titles
     * @param publishedBefore
     *            if published before
     * @param multipleFiles
     *            if there will be multiple files
     */
    protected void processVerifyPrune(Context context,
            HttpServletRequest request, HttpServletResponse response,
            SubmissionInfo subInfo, boolean multipleTitles,
            boolean publishedBefore, boolean multipleFiles)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // get the item to prune
        Item item = subInfo.getSubmissionItem().getItem();

        if (!multipleTitles && subInfo.getSubmissionItem().hasMultipleTitles())
        {
            item.clearDC("title", "alternative", Item.ANY);
        }

        if (!publishedBefore && subInfo.getSubmissionItem().isPublishedBefore())
        {
            item.clearDC("date", "issued", Item.ANY);
            item.clearDC("identifier", "citation", Item.ANY);
            item.clearDC("publisher", null, Item.ANY);
        }

        if (!multipleFiles && subInfo.getSubmissionItem().hasMultipleFiles())
        {
            // remove all but first bitstream from bundle[0]
            // FIXME: Assumes multiple bundles, clean up someday...
            // (only messes with the first bundle.)
            Bundle[] bundles = item.getBundles("ORIGINAL");

            if (bundles.length > 0)
            {
                Bitstream[] bitstreams = bundles[0].getBitstreams();

                // Remove all but the first bitstream
                for (int i = 1; i < bitstreams.length; i++)
                {
                    bundles[0].removeBitstream(bitstreams[i]);
                }
            }
        }
    }
}

