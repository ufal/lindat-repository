/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery.json;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;

/**
 * Class used to search in the discovery backend and return a json formatted string
 *
 * based on class by:
 * Kevin Van de Velde (kevin at atmire dot com)
 * Mark Diggory (markd at atmire dot com)
 * Ben Bosman (ben at atmire dot com)
 *
 * modified for LINDAT/CLARIN
 */
public class JSONDiscoverySearcher extends AbstractReader implements Recyclable {

    private static Logger log = Logger.getLogger(JSONDiscoverySearcher.class);
    private InputStream JSONStream;


    /** The Cocoon response */
    protected Response response;

    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }


    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        //Retrieve all the given parameters
        Request request = ObjectModelHelper.getRequest(objectModel);
        this.response = ObjectModelHelper.getResponse(objectModel);

        DiscoverQuery queryArgs = new DiscoverQuery();

        String q = request.getParameter("q");
        queryArgs.setQuery(q==null?"*:*":q);


        //Retrieve all our filter queries
        if(request.getParameterValues("fq") != null)
            queryArgs.addFilterQueries(request.getParameterValues("fq"));

        //Retrieve our facet limit (if any)
        int facetLimit;
        if(request.getParameter("facet.limit") != null){
            try{
                facetLimit = Integer.parseInt(request.getParameter("facet.limit"));
            }catch (Exception e){
                //Should an invalid value be supplied use -1
                facetLimit = -1;
            }
        }
        else
        {
            facetLimit = -1;
        }

        //Retrieve our facet fields
        if(request.getParameterValues("facet.field") != null){
            for (int i = 0; i < request.getParameterValues("facet.field").length; i++) {
                //Retrieve our sorting value
                DiscoveryConfigurationParameters.SORT facetSort;
                if(request.getParameter("facet.sort") == null || request.getParameter("facet.sort").equalsIgnoreCase("count")){
                    facetSort = DiscoveryConfigurationParameters.SORT.COUNT;
                }
                else
                    facetSort = DiscoveryConfigurationParameters.SORT.VALUE;


                String facetField = request.getParameterValues("facet.field")[i];
                queryArgs.addFacetField(new DiscoverFacetField(facetField, DiscoveryConfigurationParameters.TYPE_AC, facetLimit, facetSort));
            }
        }


        //Retrieve our facet min count
        int facetMinCount;
        try{
            facetMinCount = Integer.parseInt(request.getParameter("facet.mincount"));
        }catch (Exception e){
            facetMinCount = 1;
        }
        queryArgs.setFacetMinCount(facetMinCount);
        String jsonWrf = request.getParameter("json.wrf");

        try {
            Context context = ContextUtil.obtainContext(objectModel);
            JSONStream = getSearchService().searchJSON(queryArgs, getScope(context, objectModel), jsonWrf);
        } catch (Exception e) {
            log.error("Error while retrieving JSON string for Discovery auto complete", e);
        }

    }

    public void generate() throws IOException, SAXException, ProcessingException {
        if(JSONStream != null){
            byte[] buffer = new byte[8192];

            response.setHeader("Content-Length", String.valueOf(JSONStream.available()));
            int length;
            while ((length = JSONStream.read(buffer)) > -1)
            {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @param context the dspace context
     * @return The current scope.
     */
    private DSpaceObject getScope(Context context, Map objectModel) throws SQLException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String scopeString = request.getParameter("scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
        {
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        }
        else
        {
            // Get the search scope from the location parameter
            dso = HandleManager.resolveToObject(context, scopeString);
        }

        return dso;
    }
}
