/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;

import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

/**
 * Display basic meta-meta information about the item and allow the user to change 
 * it's state such as withdraw or reinstate, possibily even completely deleting the item!
 * 
 * based on class by Jay Paz and Scott Phillips
 * modified for LINDAT/CLARIN
 */

public class EditItemStatusForm extends AbstractDSpaceTransformer {
	
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");

	
	private static final Message T_title = message("xmlui.administrative.item.EditItemStatusForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditItemStatusForm.trail");
	private static final Message T_para1 = message("xmlui.administrative.item.EditItemStatusForm.para1");
	private static final Message T_label_id = message("xmlui.administrative.item.EditItemStatusForm.label_id");
	private static final Message T_label_handle = message("xmlui.administrative.item.EditItemStatusForm.label_handle");
	private static final Message T_label_modified = message("xmlui.administrative.item.EditItemStatusForm.label_modified");
	private static final Message T_label_in = message("xmlui.administrative.item.EditItemStatusForm.label_in");
	private static final Message T_label_page = message("xmlui.administrative.item.EditItemStatusForm.label_page");
	private static final Message T_label_embargo = message("xmlui.administrative.item.EditItemStatusForm.label_embargo");
	private static final Message T_label_auth = message("xmlui.administrative.item.EditItemStatusForm.label_auth");
	private static final Message T_label_withdraw = message("xmlui.administrative.item.EditItemStatusForm.label_withdraw");
	private static final Message T_label_reinstate = message("xmlui.administrative.item.EditItemStatusForm.label_reinstate");
    private static final Message T_label_move = message("xmlui.administrative.item.EditItemStatusForm.label_move");
	private static final Message T_label_delete = message("xmlui.administrative.item.EditItemStatusForm.label_delete");
	private static final Message T_submit_authorizations = message("xmlui.administrative.item.EditItemStatusForm.submit_authorizations");
	private static final Message T_submit_withdraw = message("xmlui.administrative.item.EditItemStatusForm.submit_withdraw");
	private static final Message T_submit_reinstate = message("xmlui.administrative.item.EditItemStatusForm.submit_reinstate");
    private static final Message T_submit_move = message("xmlui.administrative.item.EditItemStatusForm.submit_move");
	private static final Message T_submit_delete = message("xmlui.administrative.item.EditItemStatusForm.submit_delete");
	private static final Message T_na = message("xmlui.administrative.item.EditItemStatusForm.na");
	
	private static final Message T_not_allowed = message("xmlui.administrative.item.EditItemStatusForm.not_allowed");
	private static final Message T_collectionadmins_only = message("xmlui.administrative.item.EditItemStatusForm.collection_admins_only");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}
	
	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		Item item = Item.find(context, itemID);
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();
		
	
		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative edit-item-status");
		main.setHead(T_option_head);
		
		
		
		String tabLink = baseURL + "&submit_status";

		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		ViewItem.add_options(options, baseURL, ViewItem.T_option_status, tabLink);
		
		main = main.addDivision("item-status", "well well-light");
		
		// PARA: Helpfull instructions
		main.addPara(null, "alert").addContent(T_para1);

		
		// LIST: Item meta-meta information
		List itemInfo = main.addList("item-info", null, "table");
		
		itemInfo.addLabel(T_label_id);
		itemInfo.addItem(String.valueOf(item.getID()));
		
		itemInfo.addLabel(T_label_handle);
		itemInfo.addItem(item.getHandle()==null?"None":item.getHandle());
		
		itemInfo.addLabel(T_label_modified);
		itemInfo.addItem(item.getLastModified().toString());
		
		itemInfo.addLabel(T_label_in);
		
		List subList = itemInfo.addList("collections", List.TYPE_SIMPLE);
		Collection[] collections = item.getCollections();
		for(Collection collection : collections) {
			subList.addItem(collection.getMetadata("name"));
		}
		
		itemInfo.addLabel(T_label_page);
		if(item.getHandle()==null){
			itemInfo.addItem(T_na);		
		}
		else{
			itemInfo.addItem().addXref(ConfigurationManager.getProperty("dspace.url") + "/handle/" + item.getHandle(),ConfigurationManager.getProperty("dspace.url") + "/handle/" + item.getHandle());		
		}
		
		// embargoed
		itemInfo.addLabel(T_label_embargo);
		String embargo = null;
		try {
			DCDate edate = item.getEmbargo();
			embargo = edate.toString();
		}catch( IllegalArgumentException e ) {
			embargo = "cannot be interpreted!";
		}catch( Exception e ) {
		}
		if ( embargo != null ) {
			itemInfo.addItem(embargo);
		}else {
			itemInfo.addItem(T_na);
		}
		
		
		itemInfo.addLabel(T_label_auth);
		try
		{
		    AuthorizeUtil.authorizeManageItemPolicy(context, item);
		    itemInfo.addItem().addButton("submit_authorization").setValue(T_submit_authorizations);
		}
		catch (AuthorizeException authex) 
		{
		    addNotAllowedButton(itemInfo.addItem(), "submit_authorization", T_submit_authorizations);
        }
	
		if(!item.isWithdrawn())
		{
			itemInfo.addLabel(T_label_withdraw);
			try
	        {
	            AuthorizeUtil.authorizeWithdrawItem(context, item);
	            itemInfo.addItem().addButton("submit_withdraw").setValue(T_submit_withdraw);
	        }
	        catch (AuthorizeException authex) 
	        {
	            addNotAllowedButton(itemInfo.addItem(), "submit_withdraw", T_submit_withdraw);
	        }
		}
		else
		{	
			itemInfo.addLabel(T_label_reinstate);
			try
            {
                AuthorizeUtil.authorizeReinstateItem(context, item);
                itemInfo.addItem().addButton("submit_reinstate").setValue(T_submit_reinstate);
            }
            catch (AuthorizeException authex) 
            {
                addNotAllowedButton(itemInfo.addItem(), "submit_reinstate", T_submit_reinstate);
            }
		}
		
        itemInfo.addLabel(T_label_move);
        addCollectionAdminOnlyButton(itemInfo.addItem(), item.getOwningCollection(), "submit_move", T_submit_move);
        
		itemInfo.addLabel(T_label_delete);
		if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.DELETE))
		{
		    itemInfo.addItem().addButton("submit_delete").setValue(T_submit_delete);
		}
		else
		{
		    addNotAllowedButton(itemInfo.addItem(), "submit_delete", T_submit_delete);
		}
		
		
		
		
		// PARA: main actions
		main.addPara().addButton("submit_return").setValue(T_submit_return);
		
		main.addHidden("administrative-continue").setValue(knot.getId());
	}

	/**
	 * Add a disabled button with a "not allowed" notice
	 * @param item
	 * @param buttonName
	 * @param buttonLabel
	 * @throws WingException
	 * @throws SQLException
	 */
	private void addNotAllowedButton(org.dspace.app.xmlui.wing.element.Item item, String buttonName, Message buttonLabel) throws WingException, SQLException
	{
    	Button button = item.addButton(buttonName);
    	button.setValue(buttonLabel);
		button.setDisabled();
		item.addHighlight("fade").addContent(T_not_allowed);
	}
    
    private void addCollectionAdminOnlyButton(org.dspace.app.xmlui.wing.element.Item item, Collection collection, String buttonName, Message buttonLabel) throws WingException, SQLException
	{
    	Button button = item.addButton(buttonName);
    	button.setValue(buttonLabel);
        
        
    	if (!AuthorizeManager.isAdmin(context, collection))
    	{
    		// Only admins can create or delete
    		button.setDisabled();
    		item.addHighlight("fade").addContent(T_collectionadmins_only);
    	}
	}
	
}
