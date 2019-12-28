package com.zensols.xwiki.pamauth;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
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
}
