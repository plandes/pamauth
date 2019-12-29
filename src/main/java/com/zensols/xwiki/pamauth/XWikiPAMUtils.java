package com.zensols.xwiki.pamauth;

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

import com.zensols.unix.userauth.UserManager;
import com.zensols.unix.userauth.User;

class XWikiPAMUtils {
    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiPAMUtils.class);

    /**
     * The XWiki space where users are stored.
     */
    private static final String XWIKI_USER_SPACE = "XWiki";

    private XWikiPAMConfig configuration;
    private UserManager userManager;

    public XWikiPAMUtils(XWikiPAMConfig configuration)
    {
	this.configuration = configuration;
	this.userManager = new UserManager();
    }

    /**
     * @param validXWikiUserName a valid XWiki username for which to get a profile document
     * @param context the XWiki context
     * @return a (new) XWiki document for the passed username
     * @throws XWikiException when a problem occurs while retrieving the user profile
     */
    public XWikiDocument getAvailableUserProfile(String validXWikiUserName, XWikiContext context) throws XWikiException
    {
	DocumentReference userReference =
	    new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName);

	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("looking up {} in context={} with ref={}", validXWikiUserName, context, userReference);
	}

	// Check if the default profile document is available
	for (int i = 0; true; ++i) {
	    if (i > 0) {
		userReference =
		    new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName + "_" + i);
	    }

	    XWikiDocument doc = context.getWiki().getDocument(userReference, context);

	    // Don't use non user existing document
	    if (doc.isNew()) {
		LOGGER.debug("returning new document");
		return doc;
	    }
	}
    }

    /**
     * @param validXWikiUserName the valid XWiki name of the user to get the profile for. Used for fast lookup relying
     *            on the document cache before doing a database search.
     * @param userName the user name to get the profile for
     * @param context the XWiki context
     * @return the XWiki document of the user with the passed user name
     * @throws XWikiException when a problem occurs while retrieving the user profile
     */
    public XWikiDocument getUserProfileByUserName(String validXWikiUserName, String userName, XWikiContext context)
	throws XWikiException
    {
	PAMProfileXClass pamXClass = new PAMProfileXClass(context);
	// Try default profile name (generally in the cache)
	XWikiDocument userProfile = null;

	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("profile by validWiki={}, userName={}", validXWikiUserName, userName);
	}

	if (validXWikiUserName != null) {
	    userProfile = context.getWiki()
		.getDocument(new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName), context);
	}

	if ((userProfile == null) || !userName.equalsIgnoreCase(pamXClass.getUserName(userProfile))) {
	    // Search for existing profile with provided userName
	    userProfile = pamXClass.searchDocumentByUserName(userName);

	    if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("searched userName={} -> profile={}", userName, userProfile);
	    }

	    // Resolve default profile patch of an userName
	    if (userProfile == null && validXWikiUserName != null) {
		userProfile = getAvailableUserProfile(validXWikiUserName, context);

		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("resolved user profile={}", userProfile);
		}
	    }
	}

	return userProfile;
    }

    /**
     * Create an XWiki user and set all mapped attributes from PAM to XWiki attributes.
     *
     * @param userProfile the XWiki user profile.
     * @param attributes the attributes.
     * @param pamDN the PAM DN of the user.
     * @param pamUid the PAM unique id of the user.
     * @param context the XWiki context.
     * @throws XWikiException error when creating XWiki user.
     */
    protected void createUserFromPAM(XWikiDocument userProfile, Map<String, String> attributes,
				     String userName, String uid, XWikiContext context)
	throws XWikiException
    {
	Map<String, Object> map = new java.util.HashMap(attributes);

	// Mark user active
	map.put("active", "1");

	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("storing attributes for userName={}, uid={}: {}", userName, uid, map);
	}

	XWikiException createUserError = null;
	try {
	    context.getWiki().createUser(userProfile.getDocumentReference().getName(), map, context);
	} catch (XWikiException e) {
	    createUserError = e;
	}

	XWikiDocument createdUserProfile = context.getWiki().getDocument(userProfile.getDocumentReference(), context);
	if (createdUserProfile.isNew()) {
	    if (createUserError != null) {
		throw createUserError;
	    } else {
		throw new XWikiPAMException("User [" + userProfile.getDocumentReference() + "] hasn't been created for unknown reason");
	    }
	} else if (createUserError != null) {
	    // Whatever crashed the createUser API it was after the actual user creation so let's log an error and
	    // continue
	    LOGGER.error("Unexpected error when creating user [{}]", userProfile.getDocumentReference(),
			 createUserError);
	}

	// Update pam profile object
	PAMProfileXClass pamXClass = new PAMProfileXClass(context);

	if (pamXClass.updatePAMObject(createdUserProfile, userName, uid)) {
	    context.getWiki().saveDocument(createdUserProfile, "Created user profile from PAM server", context);
	}
    }

    /**
     * Sets attributes on the user object based on attribute values provided by the PAM.
     *
     * @param userProfile the XWiki user profile document.
     * @param attributes the attributes of the PAM user to update.
     * @param userName the USERNAME of the PAM user to update
     * @param uid value of the unique identifier for the user to update.
     * @param context the XWiki context.
     * @throws XWikiException error when updating XWiki user.
     */
    protected void updateUserFromPAM(XWikiDocument userProfile, Map<String, String> userMappings,
				     String userName, String uid, XWikiContext context) throws XWikiException
    {
	BaseClass userClass = context.getWiki().getUserClass(context);
	BaseObject userObj = userProfile.getXObject(userClass.getDocumentReference());

	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("Start synchronization of PAM profile with existing user profile based on mapping [{}]",
			 userMappings);
	}

	// Clone the user object
	BaseObject clonedUser = userObj.clone();

	// Apply all attributes to the clone
	clonedUser.getXClass(context).fromMap(userMappings, clonedUser);

	// Let BaseObject#apply tell us if something changed or not
	boolean needsUpdate = userObj.apply(clonedUser, false);

	// Update pam profile object
	PAMProfileXClass ldaXClass = new PAMProfileXClass(context);
	needsUpdate |= ldaXClass.updatePAMObject(userProfile, userName, uid);

	if (needsUpdate) {
	    context.getWiki().saveDocument(userProfile, "Synchronized user profile with PAM server", true, context);
	}
    }

    protected Map<String, String> getUserAttributes(String userName, String password) throws XWikiException {
	Map<String, String> attributes = null;
	User user = this.userManager.createUser(userName);
	boolean userExists = user.exists();
	boolean isAuthorized = userExists && ((password == null) || user.isAuthorized(password));

	LOGGER.debug("User {}: null password={}, exists={}, authorized={}",
		     userName, (password == null), userExists, isAuthorized);

	if (userExists && isAuthorized) {
	    String name = user.getFullName();
	    String[] nameParts = name.split(" ");

	    attributes = new  java.util.HashMap();
	    attributes.put(PAMProfileXClass.PAM_XFIELD_USER_NAME, userName);
	    attributes.put(PAMProfileXClass.PAM_XFIELD_UID, String.valueOf(user.getUserId()));
	    attributes.put("full_name", name);
	    if (nameParts.length == 2) {
		attributes.put("first_name", nameParts[0]);
		attributes.put("last_name", nameParts[1]);
	    }
	}

	LOGGER.debug("User {}: attributes: {}", userName, attributes);

	return attributes;
    }

    /**
     * Update or create XWiki user base on PAM.
     *
     * @param userProfile the name of the user.
     * @param userName the UNIX user name
     * @param authInput the input used to identify the user
     * @param attributes the attributes of the PAM user.
     * @param context the XWiki context.
     * @return the XWiki user document
     * @throws XWikiException error when updating or creating XWiki user.
     */
    public XWikiDocument syncUser(XWikiDocument userProfile, String userName, String password, XWikiContext context)
	throws XWikiException
    {
	// check if we have to create the user
	if (userProfile == null || userProfile.isNew() || (password != null) ||
	    this.configuration.getPAMParam("pam_update_user", "0").equals("1")) {

	    LOGGER.debug("Getting attributes for user name: {}, profile={}", userName, userProfile);
	    Map<String, String> attributes = getUserAttributes(userName, password);

	    if (attributes != null) {
		String uid = attributes.get(PAMProfileXClass.PAM_XFIELD_UID);

		// Load XWiki user document if we don't already have them
		if (userProfile == null) {
		    userProfile = getAvailableUserProfile(userName, context);
		}

		LOGGER.debug("Loaded user profile: {}, new=", userProfile, userProfile.isNew());

		if (userProfile.isNew()) {
		    LOGGER.debug("Creating new XWiki user based on PAM attribues located at [{}]", userName);

		    createUserFromPAM(userProfile, attributes, userName, uid, context);

		    LOGGER.debug("New XWiki user created: [{}]", userProfile.getDocumentReference());
		} else {
		    LOGGER.debug("Updating existing user with PAM attribues located at [{}]", userName);

		    try {
			updateUserFromPAM(userProfile, attributes, userName, uid, context);
		    } catch (XWikiException e) {
			LOGGER.error("Failed to synchronise user's informations", e);
		    }
		}
	    }
	}

	return userProfile;
    }
}
