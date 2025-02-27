/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
importClass(Packages.javax.mail.internet.AddressException);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);

importClass(Packages.org.dspace.core.ConfigurationManager);
importClass(Packages.org.dspace.core.Context);
importClass(Packages.org.dspace.content.Collection);
importClass(Packages.org.dspace.eperson.EPerson);
importClass(Packages.org.dspace.eperson.AccountManager);
importClass(Packages.org.dspace.eperson.Subscribe);
importClass(Packages.org.dspace.authorize.AuthorizeException);

importClass(Packages.org.dspace.app.xmlui.utils.AuthenticationUtil);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);

importClass(Packages.java.lang.String);

importClass(Packages.cz.cuni.mff.ufal.DSpaceApi);

importClass(Packages.org.dspace.content.Item);
importClass(Packages.org.dspace.app.xmlui.aspect.administrative.FlowItemUtils);

/**
 * This class defines the workflows for three flows within the EPerson aspect.
 * 
 * FIXME: add more documentation
 * 
 * based on file by Scott Phillips
 * modified for LINDAT/CLARIN
 */
 
/** These functions should be common to all Manakin Flow scripts */
function getObjectModel() 
{
  return FlowscriptUtils.getObjectModel(cocoon);
}

function getDSContext()
{
	return ContextUtil.obtainContext(getObjectModel());
}

function getEPerson() 
{
    return getDSContext().getCurrentUser();
}



/**
 * Perform a new user registration. 
 */
function doRegister() 
{ 
    //Make sure that user registration is enabled
    if (!(ConfigurationManager.getBooleanProperty("xmlui.user.registration", true)))
    {
        // We're configured to not allow user registration
        // User should only have gotten here by manually typing /register in URL, so we'll throw an error.
        throw new AuthorizeException("User registration is disabled");
    }
    
    var token = cocoon.request.get("token");
    
    if (token == null) 
    {
        // We have no token, this is the initial form. First ask the user for their email address
        // and then send them a token.
        var accountExists = false;
        var errors = new Array();
        do {
            var email = cocoon.request.getParameter("email");
        			
            cocoon.sendPageAndWait("register/start",{"email" : email, "errors" : errors.join(','), "accountExists" : accountExists});
            var errors = new Array();
            accountExists = false;
            
            var submit_forgot = cocoon.request.getParameter("submit_forgot");
            
            if (submit_forgot != null)
            {
                // The user attempted to register with an email address that all ready exists then they clicked
                // the "I forgot my password" button. In this case, we send them a forgot password token.
                AccountManager.sendForgotPasswordInfo(getDSContext(),email);
                getDSContext().commit();

                cocoon.sendPage("forgot/verify", {"email":email});
                return;
            }
            
            email = cocoon.request.getParameter("email");
            email = email.toLowerCase(); // all emails should be lowercase
            var epersonFound = (EPerson.findByEmail(getDSContext(),email) != null);
            
            if (epersonFound) 
            {
                accountExists = true;
                continue;
            }
            
            var canRegister = AuthenticationUtil.canSelfRegister(getObjectModel(), email);
           
            if (canRegister) 
            {
                try 
                {
                    // May throw the AddressException or a varity of SMTP errors.
                    AccountManager.sendRegistrationInfo(getDSContext(),email);
                    getDSContext().commit();
                } 
                catch (error) 
                {
                    // If any errors occure while trying to send the email set the field in error.
                    errors = new Array("email");
                    continue;
                }
                
                cocoon.sendPage("register/verify", { "email":email, "forgot":"false" });
                return; 
            } 
            else 
            {
                cocoon.sendPage("register/cannot", { "email" : email});
                return;
            }
           
        } while (accountExists || errors.length > 0)
    } 
    else 
    {
        // We have a token. Find out who the it's for
        var email = AccountManager.getEmail(getDSContext(), token);
        
        if (email == null) 
        {
            cocoon.sendPage("register/invalid-token");
            return;
        }
        
        var setPassword = AuthenticationUtil.allowSetPassword(getObjectModel(),email);
        
        var errors = new Array();
        do {
            cocoon.sendPageAndWait("register/profile",{"email" : email, "allowSetPassword":setPassword , "errors" : errors.join(',')});
            
            // If the user had to retry the form a user may allready be created.
            var eperson = EPerson.findByEmail(getDSContext(),email);
            if (eperson == null)
            {
                eperson = AuthenticationUtil.createNewEperson(getObjectModel(),email);
            }
            
            // Log the user in so that they can update their own information.
            getDSContext().setCurrentUser(eperson);
            
            errors = updateInformation(eperson);
            
            if (setPassword) 
            {
                var passwordErrors = updatePassword(eperson);
                errors = errors.concat(passwordErrors);
            }
            
            // Log the user back out.
            getDSContext().setCurrentUser(null);
        } while (errors.length > 0) 
        
        // Log the newly created user in.
        AuthenticationUtil.logIn(getObjectModel(),eperson);
        AccountManager.deleteToken(getDSContext(), token);
        getDSContext().commit();
        
        cocoon.sendPage("register/finished");
        return;
    }
}
  

/**
 * Preform a forgot password processes.
 */
function doForgotPassword() 
{ 
    var token = cocoon.request.get("token");

    if (token == null) 
    {
        // We have no token, this is the initial form. First ask the user for their email address
        // and then send them a token.
        
        var email = cocoon.request.getParameter("email");
        
        var errors = new Array();
        do {
            cocoon.sendPageAndWait("forgot/start",{"email" : email, "errors" : errors.join(',')});
  
            email = cocoon.request.getParameter("email");
            errors = new Array();

            var epersonFound = (EPerson.findByEmail(getDSContext(),email) != null);

            if (!epersonFound)
            {
                // No eperson found for the given address, set the field in error and let 
                // the user try again.
                errors = new Array("email");
                continue;
            }

            // An Eperson was found for the given email, so use the forgot password 
            // mechanism. This may throw a AddressException if the email is ill-formed.
            AccountManager.sendForgotPasswordInfo(getDSContext(),email);
            getDSContext().commit();
        } while (errors.length > 0)
        
        cocoon.sendPage("forgot/verify", {"email":email});
    } 
    else 
    {
        // We have a token. Find out who the it's for
        var email = AccountManager.getEmail(getDSContext(), token);

        if (email == null) 
        {
            cocoon.sendPage("forgot/invalid-token");
            return;
        }

        var epersonFound = (AccountManager.getEPerson(getDSContext(), token) != null);

        if (!epersonFound)
        {
            cocoon.sendPage("forgot/invalid-token");
            return;
        }

        var errors = new Array();

        do {
            cocoon.sendPageAndWait("forgot/reset", { "email" : email, "errors" : errors.join(',') });

            // Get the eperson associated with the password change
            var eperson = AccountManager.getEPerson(getDSContext(), token);

            // Temporaraly log the user in so that they can update their password.
            getDSContext().setCurrentUser(eperson);

            errors = updatePassword(eperson);

            getDSContext().setCurrentUser(null);

        } while (errors.length > 0)

        // Log the user in and remove the token.
        AuthenticationUtil.logIn(getObjectModel(),eperson);
        AccountManager.deleteToken(getDSContext(), token);
        getDSContext().commit();

        cocoon.sendPage("forgot/finished");
    }
}
  
/**
 * Flow function to update a user's profile. This flow will iterate 
 * over the profile/update form untill the user has provided correct 
 * data (i.e. filled in the required fields and meet the minimum 
 * password requirements).
 */
function doUpdateProfile()
{
    var retry = false;
    
    // check that the user is logged in.
    if (getEPerson() == null)
    {
        var contextPath = cocoon.request.getContextPath();
        cocoon.redirectTo(contextPath + "/login",true);
        getDSContext().complete();
        cocoon.exit();
    }
    
    // Do we allow the user to change their password or does 
    // it not make sense for their authentication mechanism?
    var setPassword = AuthenticationUtil.allowSetPassword(getObjectModel(),getEPerson().getEmail());
    
    // List of errors encountered.
    var errors = new Array();
    do {
        cocoon.sendPageAndWait("profile/update", {"allowSetPassword" : setPassword, "errors" : errors.join(',') } );
        
        
        if (cocoon.request.get("submit"))
        {    
            // Update the user's info and password.
            errors = updateInformation(getEPerson());
            
            if (setPassword) 
            {
                // check if they entered a new password:
                var password = cocoon.request.getParameter("password");
                
                if (password != null && !password.equals(""))
                { 
                    var passwordErrors = updatePassword(getEPerson());
                    
                    errors = errors.concat(passwordErrors);
                } 
            }
        }
        else if (cocoon.request.get("submit_subscriptions_add"))
        {
            // Add the a new subscription
            var collection = Collection.find(getDSContext(),cocoon.request.get("subscriptions"));
            if (collection != null)
            {
                Subscribe.subscribe(getDSContext(),getEPerson(),collection);
                getDSContext().commit();
            }
        }
        else if (cocoon.request.get("submit_subscriptions_delete"))
        {
            // Remove any selected subscriptions
            var names = cocoon.request.getParameterValues("subscriptions_selected");
            if (names != null)
            {
	            for (var i = 0; i < names.length; i++)
	            {
	            	var collectionID = cocoon.request.get(names[i]);
	                var collection = Collection.find(getDSContext(),collectionID);
	                if (collection != null)
	                    Subscribe.unsubscribe(getDSContext(),getEPerson(),collection);
	            }
            }
            getDSContext().commit();
        }
            
    } while (errors.length > 0 || !cocoon.request.get("submit")) 
  
    cocoon.sendPage("profile/updated");
}
  
  
/**
 * Update the eperson's profile information. Some fields, such as 
 * last_name & first_name are required.
 *
 * Missing or mailformed field names will be returned in an array. 
 * If the user's profile information was updated successfully then 
 * an empty array will be returned. 
 */
function updateInformation(eperson) 
{
    if (!(ConfigurationManager.getBooleanProperty("xmlui.user.editmetadata", true)))
    {
        // We're configured to not allow the user to update their metadata so return with no errors.
        return new Array();
    }


	// Get the parameters from the form
	var lastName = cocoon.request.getParameter("last_name");
	var firstName = cocoon.request.getParameter("first_name");
	var phone = cocoon.request.getParameter("phone");
    var language = cocoon.request.getParameter("language");
    var registering = cocoon.request.getParameter("registering");
	var oldFirstName = eperson.getFirstName();
	var oldLastName = eperson.getLastName();

    // first check that each parameter is filled in before setting anything.
	var idx = 0;
	var errors = new Array();
	
	if ((registering || !stringsEqual(firstName, oldFirstName)) && stringsEqual(firstName, ""))
    {
        errors[idx++] = "first_name";
    }
    
    if ((registering || !stringsEqual(lastName, oldLastName)) && stringsEqual(lastName, ""))
	{
	    errors[idx++] = "last_name";
	}
	
	if (idx > 0) 
	{
	    // There were errors
	    return errors;
	}
	
	eperson.setFirstName(firstName);
	eperson.setLastName(lastName);
	
	eperson.setMetadata("phone", phone);
    eperson.setLanguage(language);
	eperson.update();
	
    return new Array();
}

/**
 * Helper function to check if two strings are equal
 * treating null as empty string
 *
 * @param s1 First string
 * @param s2 Second string
 * @returns True is the strings equal, false otherwise
 */
function stringsEqual(s1, s2)
{
	var res = false;
	if(((s1 == null || s1.equals("")) && (s2 == null || s2.equals(""))) ||
		(s1 != null && s2 != null && s1.equals(s2)))
	{
		res = true;
	}
	return res;
}

  
/**
 * Update the eperson's password if it meets the minimum password
 * requirements. 
 *
 * Any fields that are in error will be returned in an array. If
 * the user's password was updated successfull then an empty array
 * will be returned.
 */
function updatePassword(eperson) 
{
    var password = cocoon.request.getParameter("password");
    var passwordConfirm = cocoon.request.getParameter("password_confirm");
    
    // No empty passwords
    if (password == null)
    {
        return new Array("password");
    }
    
    // No short passwords
	if ( password.length() < 6) 
	{
		return new Array("password");
	}  
    
    // No unconfirmed passwords
	if (!password.equals(passwordConfirm)) 
	{
	    return new Array("password_confirm");
	} 
    
	eperson.setPassword(password);
	eperson.update();
	
	return new Array();
}

function askForMail(){
	var ask = true;
    var token = cocoon.request.get("token");
    
	var eperson = getEPerson();
    if (token == null && (eperson == null || eperson.getEmail() != null))
    {
        // User should only have gotten here after shibboleth eperson was created without an email or with a token
        throw new AuthorizeException("User not authorized to set an email");
    }
            
    if (token == null) 
    {
        // We have no token, this is the initial form. First ask the user for their email address
        // and then send them a token.
        var eid = eperson.getID();
        
        //log the user out
        AuthenticationUtil.logOut(getDSContext(), cocoon.request);
        getDSContext().commit();
        
        var accountExists = false;
        var errors = new Array();
        do {
            var email = cocoon.request.getParameter("email");
        			
            cocoon.sendPageAndWait("set-email/start",{"email" : email, "errors" : errors.join(','), "accountExists" : accountExists});
            var errors = new Array();
            accountExists = false;
            
            email = cocoon.request.getParameter("email");
            email = email.toLowerCase(); // all emails should be lowercase
            var epersonFound = (EPerson.findByEmail(getDSContext(),email) != null);
            
            if (epersonFound) 
            {
                accountExists = true;
                errors = new Array("email_used");
                cocoon.log.error("The email " + email +" is already in use.");
                continue;
            }
            
            try 
            {
                // May throw the AddressException or a varity of SMTP errors.
                DSpaceApi.sendRegistrationInfo(getDSContext(),email,eid);              
                getDSContext().commit();
            } 
            catch (error) 
            {
                // If any errors occure while trying to send the email set the field in error.
                errors = new Array("email");
                cocoon.log.error(error);
                continue;
            }
            
            cocoon.sendPage("set-email/verify", { "email":email});
            return; 

                

        } while (accountExists || errors.length > 0)
    } 
    else 
    {
        // We have a token. Find out who the it's for
    	//Also assigns the stored email to the eperson
        eperson = DSpaceApi.getEPersonByToken(getDSContext(), token);
        
        if (eperson == null) 
        {
            cocoon.sendPage("set-email/invalid-token");
            return;
        }
        
        DSpaceApi.deleteToken(token);
        // Log user in.
        AuthenticationUtil.logIn(getObjectModel(),eperson);

        getDSContext().commit();
        
        cocoon.sendPage("set-email/finished");
        return;
    }	
}

function sendPageAndWait(uri,bizData,result)
{
    if (bizData == null)
        bizData = {};


    if (result != null)
    {
        var outcome = result.getOutcome();
        var header = result.getHeader();
        var message = result.getMessage();
        var characters = result.getCharacters();


        if (message != null || characters != null)
        {
            bizData["notice"]     = "true";
            bizData["outcome"]    = outcome;
            bizData["header"]     = header;
            bizData["message"]    = message;
            bizData["characters"] = characters;
        }

        var errors = result.getErrorString();
        if (errors != null)
        {
            bizData["errors"] = errors;
        }
    }

    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPageAndWait(uri,bizData);
}

function sendPage(uri,bizData,result)
{
    if (bizData == null)
        bizData = {};

    if (result != null)
    {
        var outcome = result.getOutcome();
        var header = result.getHeader();
        var message = result.getMessage();
        var characters = result.getCharacters();

        if (message != null || characters != null)
        {
            bizData["notice"]     = "true";
            bizData["outcome"]    = outcome;
            bizData["header"]     = header;
            bizData["message"]    = message;
            bizData["characters"] = characters;
        }

        var errors = result.getErrorString();
        if (errors != null)
        {
            bizData["errors"] = errors;
        }
    }

    // just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPage(uri,bizData);
}

function assertEditMetadata(itemID){
    var item = Item.find(getDSContext(),itemID);

    if ( item == null || ! item.canEditMetadata()) {
        sendPage("edit/item/not-authorized");
        cocoon.exit();
    }	
}

function startEditMetadata() {
	var itemID = cocoon.request.get("itemID");
	var templateCollectionID = -1;

	assertEditMetadata(itemID);

	var result;
	do {
		sendPageAndWait("edit/item/metadata", {
			"itemID" : itemID,
			"templateCollectionID" : templateCollectionID
		}, result);
		assertEditMetadata(itemID);
		result = null;

		if (cocoon.request.get("submit_add")) {
			// Add a new metadata entry
			result = FlowItemUtils.processAddMetadata(getDSContext(), itemID,
					cocoon.request);
		} else if (cocoon.request.get("submit_update")) {
			// Update the item
			result = FlowItemUtils.processEditItemMetadata(getDSContext(), itemID,
					cocoon.request);
		} else if (cocoon.request.get("submit_return"))
        {
            // go back to where ever we came from.
            break;
        }
	} while (true)

	var item = Item.find(getDSContext(), itemID);
	cocoon.redirectTo(cocoon.request.getContextPath() + "/handle/"
			+ item.getHandle(), true);
	getDSContext().complete();
	item = null;
	cocoon.exit();
}
