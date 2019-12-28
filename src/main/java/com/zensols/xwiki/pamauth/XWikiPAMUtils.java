package com.zensols.xwiki.pamauth;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.web.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

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

    public XWikiPAMUtils(XWikiPAMConfig configuration)
    {
        this.configuration = configuration;
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

        // Check if the default profile document is available
        for (int i = 0; true; ++i) {
            if (i > 0) {
                userReference =
                    new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName + "_" + i);
            }

            XWikiDocument doc = context.getWiki().getDocument(userReference, context);

            // Don't use non user existing document
            if (doc.isNew()) {
                return doc;
            }
        }
    }

    /**
     * @param validXWikiUserName the valid XWiki name of the user to get the profile for. Used for fast lookup relying
     *            on the document cache before doing a database search.
     * @param userId the UID to get the profile for
     * @param context the XWiki context
     * @return the XWiki document of the user with the passed UID
     * @throws XWikiException when a problem occurs while retrieving the user profile
     */
    public XWikiDocument getUserProfileByUid(String validXWikiUserName, String userId, XWikiContext context)
        throws XWikiException
    {
        PAMProfileXClass pamXClass = new PAMProfileXClass(context);

        // Try default profile name (generally in the cache)
        XWikiDocument userProfile;
        if (validXWikiUserName != null) {
            userProfile = context.getWiki()
                .getDocument(new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName), context);
        } else {
            userProfile = null;
        }

        if (!userId.equalsIgnoreCase(pamXClass.getUid(userProfile))) {
            // Search for existing profile with provided uid
            userProfile = pamXClass.searchDocumentByUid(userId);

            // Resolve default profile patch of an uid
            if (userProfile == null && validXWikiUserName != null) {
                userProfile = getAvailableUserProfile(validXWikiUserName, context);
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
    protected void createUserFromPAM(XWikiDocument userProfile, String userName, String uid, XWikiContext context)
	throws XWikiException
    {
        //Map<String, String> userMappings = this.configuration.getUserMappings(null, context);

        // LOGGER.debug("Start first synchronization of PAM profile [{}] with new user profile based on mapping [{}]",
	// 	     , userMappings);

        Map<String, Object> map = new java.util.HashMap();//toMap(attributes, userMappings, context);

        // Mark user active
        map.put("active", "1");

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
                throw new XWikiPAMException(
                    "User [" + userProfile.getDocumentReference() + "] hasn't been created for unknown reason");
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
     * @param userName the DN of the PAM user to update
     * @param uid value of the unique identifier for the user to update.
     * @param context the XWiki context.
     * @throws XWikiException error when updating XWiki user.
     */
    protected void updateUserFromPAM(XWikiDocument userProfile, String userName, String uid,
				      XWikiContext context) throws XWikiException
    {
        Map<String, String> userMappings = this.configuration.getUserMappings(null);

        BaseClass userClass = context.getWiki().getUserClass(context);

        BaseObject userObj = userProfile.getXObject(userClass.getDocumentReference());

        LOGGER.debug("Start synchronization of PAM profile with existing user profile based on mapping [{}]",
		     userMappings);

        // Clone the user object
        BaseObject clonedUser = userObj.clone();

        // Apply all attributes to the clone
        //set(attributes, userMappings, clonedUser, context);

        // Let BaseObject#apply tell us if something changed or not
        boolean needsUpdate = userObj.apply(clonedUser, false);

        // Sync user photo with PAM
        //needsUpdate |= updateAvatarFromPam(attributes, userProfile, context);

        // Update pam profile object
        PAMProfileXClass ldaXClass = new PAMProfileXClass(context);
        needsUpdate |= ldaXClass.updatePAMObject(userProfile, userName, uid);

        if (needsUpdate) {
            context.getWiki().saveDocument(userProfile, "Synchronized user profile with PAM server", true, context);
        }
    }

    /**
     * Update or create XWiki user base on PAM.
     * 
     * @param userProfile the name of the user.
     * @param pamDn the PAM user DN.
     * @param authInput the input used to identify the user
     * @param attributes the attributes of the PAM user.
     * @param context the XWiki context.
     * @return the XWiki user document
     * @throws XWikiException error when updating or creating XWiki user.
     */
    public XWikiDocument syncUser(XWikiDocument userProfile, String userName, String uid, XWikiContext context)
	throws XWikiException
    {
        // check if we have to create the user
        if (userProfile == null || userProfile.isNew()
            || this.configuration.getPAMParam("pam_update_user", "0").equals("1")) {
            LOGGER.debug("PAM attributes will be used to update XWiki attributes.");

            // Load XWiki user document if we don't already have them
            if (userProfile == null) {
                userProfile = getAvailableUserProfile(uid, context);
            }

            if (userProfile.isNew()) {
                LOGGER.debug("Creating new XWiki user based on PAM attribues located at [{}]", userName);

                createUserFromPAM(userProfile, userName, uid, context);

                LOGGER.debug("New XWiki user created: [{}]", userProfile.getDocumentReference());
            } else {
                LOGGER.debug("Updating existing user with PAM attribues located at [{}]", userName);

                try {
                    updateUserFromPAM(userProfile, userName, uid, context);
                } catch (XWikiException e) {
                    LOGGER.error("Failed to synchronise user's informations", e);
                }
            }
        }

        return userProfile;
    }
}
