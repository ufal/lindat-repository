/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
importClass(Packages.java.lang.Class);
importClass(Packages.java.lang.ClassLoader);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.apache.cocoon.environment.http.HttpEnvironment);
importClass(Packages.org.apache.cocoon.servlet.multipart.Part);

importClass(Packages.org.dspace.handle.HandleManager);
importClass(Packages.org.dspace.content.Item);
importClass(Packages.org.dspace.core.Constants);
importClass(Packages.org.dspace.workflow.WorkflowItem);
importClass(Packages.org.dspace.workflow.WorkflowManager);
importClass(Packages.org.dspace.content.WorkspaceItem);
importClass(Packages.org.dspace.authorize.AuthorizeManager);

importClass(Packages.cz.cuni.mff.ufal.dspace.app.xmlui.aspect.workflow.FlowWorkflowUtils);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.org.dspace.app.xmlui.cocoon.HttpServletRequestCocoonWrapper);
importClass(Packages.org.dspace.app.xmlui.aspect.workflow.FlowUtils);

/* Global variable which stores a comma-separated list of all fields
 * which errored out during processing of the last step.
 */
var ERROR_FIELDS = null;


/**
 * Simple access method to access the current cocoon object model.
 */
function getObjectModel()
{
  return FlowscriptUtils.getObjectModel(cocoon);
}

/**
 * Return the DSpace context for this request since each HTTP request generates
 * a new context this object should never be stored and instead always accessed
 * through this method so you are ensured that it is the correct one.
 */
function getDSContext()
{
    return ContextUtil.obtainContext(getObjectModel());
}


/**
 * Return the HTTP Request object for this request
 */
function getHttpRequest()
{
    //return getObjectModel().get(HttpEnvironment.HTTP_REQUEST_OBJECT)

    // Cocoon's request object handles form encoding, thus if the users enters
    // non-ascii characters such as those found in foreign languages they will
    // come through corrupted if they are not obtained through the cocoon request
    // object. However, since the dspace-api is built to accept only HttpServletRequest
    // a wrapper class HttpServletRequestCocoonWrapper has bee built to translate
    // the cocoon request back into a servlet request. This is not a fully complete
    // translation as some methods are unimplemented. But it is enough for our
    // purposes here.
    return new HttpServletRequestCocoonWrapper(getObjectModel());
}

/**
 * Return the HTTP Response object for the response
 * (used for compatibility with DSpace configurable submission system)
 */
function getHttpResponse()
{
    return getObjectModel().get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
}

/**
 * Send the current page and wait for the flow to be continued. Use this method to add
 * a flow=true parameter. This allows the sitemap to filter out all flow related requests
 * from non flow related requests.
 */
function sendPageAndWait(uri,bizData)
{
    if (bizData == null)
        bizData = {};

    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPageAndWait(uri,bizData);
}

/*
 * UFAL - Move this item to another collection
 */
function doMoveWorkflowItem(handle, workflowID)
{
    sendPageAndWait("workflow/item/move", {"handle": handle, "workflowID": workflowID});

    if (cocoon.request.get("submit_cancel"))
    {
        return null;
    }
    else if (cocoon.request.get("submit_move"))
    {
        var collectionID = cocoon.request.get("collectionID");
        if (!collectionID)
        {
            throw "Unable to find collection, no collection id supplied.";
        }

        return FlowWorkflowUtils.processMoveWorkflowItem(getDSContext(), workflowID, collectionID);
    }
}


/**
 * Send the given page and DO NOT wait for the flow to be continued. Execution will
 * proceed as normal. Use this method to add a flow=true parameter. This allows the
 * sitemap to filter out all flow related requests from non flow related requests.
 */
function sendPage(uri,bizData)
{
    if (bizData == null)
        bizData = {};

    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPage(uri,bizData);
}


/**
 * This is the starting point for all workflow tasks. The id of the workflow
 * is expected to be passed in as a request parameter. The user will be able
 * to view the item and perform the necessary actions on the task such as:
 * accept, reject, or edit the item's metadata.
 */
function doWorkflow()
{
    var workflowID = cocoon.request.get("workflowID");

    if (workflowID == null)
    {
        throw "Unable to find workflow, no workflow id supplied.";
    }

    // Specify that we are working with workflows.
    //(specify "W" for workflow item, for FlowUtils.findSubmission())
    if(workflowID.substr(0,1) != "W")
    {
        workflowID = "W"+workflowID;
    }
    // Get the collection handle for this item.
    var handle = FlowUtils.findWorkflow(getDSContext(), workflowID).getCollection().getHandle();

    do
    {
        //Ensure that the currently logged in user can perform the task
        FlowUtils.authorizeWorkflowItem(getDSContext(), workflowID);
        sendPageAndWait("handle/"+handle+"/workflow/performTaskStep",{"id":workflowID,"step":"0"});

        if (cocoon.request.get("submit_leave"))
        {
            // Just exit workflow with out doing anything
            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/submissions",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_approve"))
        {
            // Approve this task and exit the workflow
            var archived = FlowUtils.processApproveTask(getDSContext(),workflowID);

            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/submissions",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_return"))
        {
            // Return this task to the pool and exit the workflow
            FlowUtils.processUnclaimTask(getDSContext(),workflowID);

            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/submissions",true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_take_task"))
        {
            // Take the task and stay on this workflow
            FlowUtils.processClaimTask(getDSContext(),workflowID);

        }
        else if (cocoon.request.get("submit_reject"))
        {
            var rejected = workflowStepReject(handle,workflowID);

            if (rejected == true)
            {
                // the user really rejected the item
                var contextPath = cocoon.request.getContextPath();
                cocoon.redirectTo(contextPath+"/submissions",true);
                getDSContext().complete();
                cocoon.exit();
            }
        }
        else if (cocoon.request.get("submit_edit"))
        {

            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/handle/"+handle+"/workflow_edit_metadata?"+"workflowID="+workflowID, true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_curate"))
        {

            var contextPath = cocoon.request.getContextPath();
            cocoon.redirectTo(contextPath+"/admin/curate?handle="+handle+"&workflowID="+workflowID, true);
            getDSContext().complete();
            cocoon.exit();
        }
        else if (cocoon.request.get("submit_move"))
        {
            // UFAL - Move this workflow item to another collection
            var reclaimed = doMoveWorkflowItem(handle, workflowID);

            if (reclaimed == null)
            {
                // cancelled
            }
            else if (reclaimed)
            {
                var workflowItem = FlowUtils.findWorkflow(getDSContext(), workflowID)
                if(workflowItem == null)
                {
                    // item submitted upon moving to collection with no workflow steps
                    var contextPath = cocoon.request.getContextPath();
                    cocoon.redirectTo(contextPath+"/submissions",true);
                    getDSContext().complete();
                    cocoon.exit();
                }
                else
                {
                    handle = workflowItem.getCollection().getHandle();
                    var contextPath = cocoon.request.getContextPath();
                    cocoon.redirectTo(contextPath+"/handle/"+handle+"/workflow?workflowID="+workflowID,true);
                    getDSContext().complete();
                    cocoon.exit();
                }
            }
            else
            {
                var contextPath = cocoon.request.getContextPath();
                cocoon.redirectTo(contextPath+"/submissions",true);
                getDSContext().complete();
                cocoon.exit();
            }
        }

    } while (1==1)


}

/**
 * This step is used when the user wants to reject a workflow item, at this step they
 * are asked to enter a reason for the rejection.
 */
function workflowStepReject(handle,workflowID)
{
    var error_fields;
    do {

        sendPageAndWait("handle/"+handle+"/workflow/rejectTaskStep",{"id":workflowID, "step":"0", "error_fields":error_fields});

        if (cocoon.request.get("submit_reject"))
        {
            error_fields = FlowUtils.processRejectTask(getDSContext(),workflowID,cocoon.request);

            if (error_fields == null)
            {
                // Only exit if rejection succeeded, otherwise ask for a reason again.
                return true;
            }
        }
        else if (cocoon.request.get("submit_cancel"))
        {
            // just go back to the view workflow screen.
            return false;
        }
    } while (1 == 1)

}
