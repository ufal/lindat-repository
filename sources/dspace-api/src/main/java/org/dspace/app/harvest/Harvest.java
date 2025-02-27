/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.harvest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.harvest.OAIHarvester;
import org.dspace.harvest.OAIHarvester.HarvestingException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import cz.cuni.mff.ufal.DSpaceApi;

/**
 *  Test class for harvested collections.
 *
 * based on class by Alexey Maslov
 * modified for LINDAT/CLARIN
 */
public class Harvest
{
	private static Context context;
	
    public static void main(String[] argv) throws Exception
    {
    	
        // try loading dspace
        DSpaceApi.load_dspace();
    	
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("p", "purge", false, "delete all items in the collection");
        options.addOption("r", "run", false, "run the standard harvest procedure");
        options.addOption("g", "ping", false, "test the OAI server and set");
        options.addOption("o", "once", false, "run the harvest procedure with specified parameters");
        options.addOption("s", "setup", false, "Set the collection up for harvesting");
        options.addOption("S", "start", false, "start the harvest loop");
        options.addOption("R", "reset", false, "reset harvest status on all collections");
        options.addOption("P", "purge", false, "purge all harvestable collections");
        

        options.addOption("e", "eperson", true, "eperson");
        options.addOption("c", "collection", true, "harvesting collection (handle or id)");
        options.addOption("t", "type", true, "type of harvesting (0 for none)");
        options.addOption("a", "address", true, "address of the OAI-PMH server");
        options.addOption("i", "oai_set_id", true, "id of the PMH set representing the harvested collection");
        options.addOption("m", "metadata_format", true, "the name of the desired metadata format for harvesting, resolved to namespace and crosswalk in dspace.cfg");

        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, argv);

        String command = null; 
        String eperson = null;
        String collection = null;
        String oaiSource = null;
        String oaiSetID = null;
        String metadataKey = null;
        int harvestType = 0;
        
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("Harvest\n", options);
            System.out
    				.println("\nPING OAI server: Harvest -g -s oai_source -i oai_set_id");
            System.out
					.println("RUNONCE harvest with arbitrary options: Harvest -o -e eperson -c collection -t harvest_type -a oai_source -i oai_set_id -m metadata_format");
            System.out
                    .println("SETUP a collection for harvesting: Harvest -s -c collection -t harvest_type -a oai_source -i oai_set_id -m metadata_format");
            System.out
            		.println("RUN harvest once: Harvest -r -e eperson -c collection");
            System.out
    				.println("START harvest scheduler: Harvest -S");
            System.out
					.println("RESET all harvest status: Harvest -R");
            System.out
                    .println("PURGE a collection of items and settings: Harvest -p -e eperson -c collection");
            System.out
					.println("PURGE all harvestable collections: Harvest -P -e eperson");
            
            

            System.exit(0);
        }

        if (line.hasOption('s')) {
            command = "config";
        }
        if (line.hasOption('p')) {
            command = "purge";
        }
        if (line.hasOption('r')) {
            command = "run";
        }
        if (line.hasOption('g')) {
            command = "ping";
        }
        if (line.hasOption('o')) {
            command = "runOnce";
        }
        if (line.hasOption('S')) {
            command = "start";
        }
        if (line.hasOption('R')) {
            command = "reset";
        }
        if (line.hasOption('P')) {
            command = "purgeAll";
        }

        
        if (line.hasOption('e')) {
            eperson = line.getOptionValue('e');
        }
        if (line.hasOption('c')) {
            collection = line.getOptionValue('c');
        }
        if (line.hasOption('t')) {
            harvestType = Integer.parseInt(line.getOptionValue('t'));
        } else {
        	harvestType = 0;
        }
        if (line.hasOption('a')) {
            oaiSource = line.getOptionValue('a');
        }
        if (line.hasOption('i')) {
            oaiSetID = line.getOptionValue('i');
        }
        if (line.hasOption('m')) {
            metadataKey = line.getOptionValue('m');
        }
        

        // Instantiate our class
        Harvest harvester = new Harvest();
        harvester.context = new Context();
        
        
        // Check our options
        if (command == null)
        {
            System.out
                    .println("Error - no parameters specified (run with -h flag for details)");
            System.exit(1);
        }
        // Run a single harvest cycle on a collection using saved settings.
        else if ("run".equals(command))
        {
            if (collection == null || eperson == null)
            {
                System.out
                        .println("Error - a target collection and eperson must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            
            harvester.runHarvest(collection, eperson);
        }
        // start the harvest loop
        else if ("start".equals(command))
        {
        	startHarvester();
        }
        // reset harvesting status
        else if ("reset".equals(command))
        {
        	resetHarvesting();
        }
        // purge all collections that are set up for harvesting (obviously for testing purposes only)
        else if ("purgeAll".equals(command))
        {
        	if (eperson == null)
            {
                System.out
                        .println("Error - an eperson must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
        	
        	List<Integer> cids = HarvestedCollection.findAll(context);
        	System.out.println("Purging the following collections (deleting items and resetting harvest status): " + cids.toString());
	    	for (Integer cid : cids) 
	    	{
	    		harvester.purgeCollection(cid.toString(), eperson);
	    	}
	    	context.complete();
        }
        // Delete all items in a collection. Useful for testing fresh harvests.
        else if ("purge".equals(command))
        {
            if (collection == null || eperson == null)
            {
                System.out
                        .println("Error - a target collection and eperson must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            
            harvester.purgeCollection(collection, eperson);
            context.complete();
            
            //TODO: implement this... remove all items and remember to unset "last-harvested" settings
        }
        // Configure a collection with the three main settings 
        else if ("config".equals(command))
        {
            if (collection == null)
            {
                System.out.println("Error -  a target collection must be provided");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            if (oaiSource == null || oaiSetID == null)
            {
                System.out.println("Error - both the OAI server address and OAI set id must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
            if (metadataKey == null)
            {
            	System.out.println("Error - a metadata key (commonly the prefix) must be specified for this collection");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);            	
            }
            
            harvester.configureCollection(collection, harvestType, oaiSource, oaiSetID, metadataKey);
        }
        else if ("ping".equals(command))
        {
            if (oaiSource == null || oaiSetID == null)
            {
                System.out.println("Error - both the OAI server address and OAI set id must be specified");
                System.out.println(" (run with -h flag for details)");
                System.exit(1);
            }
        }
    }
    
    /*
     * Resolve the ID into a collection and check to see if its harvesting options are set. If so, return
     * the collection, if not, bail out. 
     */
    private Collection resolveCollection(String collectionID) {
    	
    	DSpaceObject dso;
    	Collection targetCollection = null;
    	
    	try {
	    	// is the ID a handle?
	        if (collectionID != null)
            {
                if (collectionID.indexOf('/') != -1)
                {
                    // string has a / so it must be a handle - try and resolve it
                    dso = HandleManager.resolveToObject(context, collectionID);

                    // resolved, now make sure it's a collection
                    if (dso == null || dso.getType() != Constants.COLLECTION)
                    {
                        targetCollection = null;
                    }
                    else
                    {
                        targetCollection = (Collection) dso;
                    }
                }
                // not a handle, try and treat it as an integer collection
                // database ID
                else
                {
                    System.out.println("Looking up by id: " + collectionID + ", parsed as '" + Integer.parseInt(collectionID) + "', " + "in context: " + context);
                    targetCollection = Collection.find(context, Integer.parseInt(collectionID));
                }
            }
            // was the collection valid?
            if (targetCollection == null)
            {
                System.out.println("Cannot resolve " + collectionID + " to collection");
                System.exit(1);
            }
    	}
    	catch (SQLException se) {
    		se.printStackTrace();
    	}
    	
    	return targetCollection;
    }
    
    
    private void configureCollection(String collectionID, int type, String oaiSource, String oaiSetId, String mdConfigId) {
    	System.out.println("Running: configure collection");
   	
    	Collection collection = resolveCollection(collectionID);
    	System.out.println(collection.getID());
    	    	
    	try {
    		HarvestedCollection hc = HarvestedCollection.find(context, collection.getID());
        	if (hc == null) {
        		hc = HarvestedCollection.create(context, collection.getID());
        	}
    		
    		context.turnOffAuthorisationSystem();
    		hc.setHarvestParams(type, oaiSource, oaiSetId, mdConfigId);
    		hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
    		hc.update();
    		context.restoreAuthSystemState();
    		context.complete();
    	} 
    	catch (Exception e) {
    		System.out.println("Changes could not be committed");
    		e.printStackTrace();
    		System.exit(1);
    	}
    	finally {
            if (context != null)
            {
    		    context.restoreAuthSystemState();
            }
    	}
    }
    
    
    /**
     * Purges a collection of all harvest-related data and settings. All items in the collection will be deleted.
     * 
     * @param collectionID
     * @param email
     */
    private void purgeCollection(String collectionID, String email) {
    	System.out.println("Purging collection of all items and resetting last_harvested and harvest_message: " + collectionID);
    	Collection collection = resolveCollection(collectionID);
   	
    	try 
    	{
    		EPerson eperson = EPerson.findByEmail(context, email);
        	context.setCurrentUser(eperson);
    		context.turnOffAuthorisationSystem();
    		
    		ItemIterator it = collection.getAllItems();
    		IndexBrowse ib = new IndexBrowse(context);
    		int i=0;
    		while (it.hasNext()) {
    			i++;
    			Item item = it.next();
    			System.out.println("Deleting: " + item.getHandle());
    			ib.itemRemoved(item);
    			collection.removeItem(item);
    			// commit every 50 items
    			if (i%50 == 0) {
    				context.commit();
    				i=0;
    			}
    		}
    		
    		HarvestedCollection hc = HarvestedCollection.find(context, collection.getID());
    		if (hc != null) {
	    		hc.setHarvestResult(null,"");
	    		hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
	    		hc.setHarvestStartTime(null);
	    		hc.update();
    		}
    		context.restoreAuthSystemState();    		
    		context.commit();
    	} 
    	catch (Exception e) {
    		System.out.println("Changes could not be committed");
    		e.printStackTrace();
    		System.exit(1);
    	}
    	finally {
    		context.restoreAuthSystemState();
    	}
    }
    
    
    /**
     * Run a single harvest cycle on the specified collection under the authorization of the supplied EPerson 
     */
    private void runHarvest(String collectionID, String email) {
    	System.out.println("Running: a harvest cycle on " + collectionID);
    	
    	System.out.print("Initializing the harvester... ");
    	OAIHarvester harvester = null;
    	try {
    		Collection collection = resolveCollection(collectionID);
        	HarvestedCollection hc = HarvestedCollection.find(context, collection.getID());
    		harvester = new OAIHarvester(context, collection, hc);
    		System.out.println("success. ");
    	}
    	catch (HarvestingException hex) {
    		System.out.print("failed. ");
    		System.out.println(hex.getMessage());
    		throw new IllegalStateException("Unable to harvest", hex);
    	} catch (SQLException se) {
            System.out.print("failed. ");
            System.out.println(se.getMessage());
            throw new IllegalStateException("Unable to access database", se);
		}
    	    	
    	try {
    		// Harvest will not work for an anonymous user
        	EPerson eperson = EPerson.findByEmail(context, email);
        	System.out.println("Harvest started... ");
        	context.setCurrentUser(eperson);
    		harvester.runHarvest();
    		context.complete();
    	}
        catch (SQLException e) {
            throw new IllegalStateException("Failed to run harvester", e);
        }
        catch (AuthorizeException e) {
            throw new IllegalStateException("Failed to run harvester", e);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to run harvester", e);
        }

        System.out.println("Harvest complete. ");
    }
    
    
    /**
     * Resets harvest_status and harvest_start_time flags for all collections that have a row in the harvested_collections table 
     */
    private static void resetHarvesting() {
    	System.out.print("Resetting harvest status flag on all collections... ");
    	
    	try 
    	{
	    	List<Integer> cids = HarvestedCollection.findAll(context);
	    	for (Integer cid : cids) 
	    	{
	    		HarvestedCollection hc = HarvestedCollection.find(context, cid);
	    		//hc.setHarvestResult(null,"");
	    		hc.setHarvestStartTime(null);
	    		hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
	    		hc.update();
	    	}
	    	context.commit();
	    	System.out.println("success. ");
    	}
    	catch (Exception ex) {
    		System.out.println("failed. ");
    		ex.printStackTrace();
    	}    	
    }
    
    
    /**
     * Starts up the harvest scheduler. Terminating this process will stop the scheduler.
     */
    private static void startHarvester() 
    {
	    try 
	    {
	    	System.out.print("Starting harvest loop... ");
	    	OAIHarvester.startNewScheduler();
	    	System.out.println("running. ");
    	}
    	catch (Exception ex) {
    		ex.printStackTrace();
    	}
    }
    
}
