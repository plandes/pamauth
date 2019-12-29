package com.zensols.xwiki.pamauth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Helper to manager PAM profile XClass and XObject.
 * 
 * @version $Id$
 */
public class PAMProfileXClass
{
    public static final String PAM_XCLASS = "XWiki.PAMProfileClass";
    public static final String PAM_XFIELD_USER_NAME = "username";
    public static final String PAM_XFIELDPN_USER_NAME = "PAM user unique identifier";
    public static final String PAM_XFIELD_UID = "uid";
    public static final String PAM_XFIELDPN_UID = "PAM UID";

    /**
     * The XWiki space where users are stored.
     */
    private static final String XWIKI_USER_SPACE = "XWiki";

    public static final EntityReference PAMPROFILECLASS_REFERENCE =
        new EntityReference("PAMProfileClass", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PAMProfileXClass.class);

    private XWikiContext context;

    private final BaseClass pamClass;

    public PAMProfileXClass(XWikiContext context) throws XWikiException
    {
        this.context = context;

        XWikiDocument pamClassDoc = context.getWiki().getDocument(PAMPROFILECLASS_REFERENCE, context);

        this.pamClass = pamClassDoc.getXClass();

        // Make sure the current class contains required properties
        boolean needsUpdate = updateClass();

        if (pamClassDoc.getCreatorReference() == null) {
            needsUpdate = true;
            pamClassDoc.setCreator(XWikiRightService.SUPERADMIN_USER);
        }
        if (pamClassDoc.getAuthorReference() == null) {
            needsUpdate = true;
            pamClassDoc.setAuthorReference(pamClassDoc.getCreatorReference());
        }
        if (!pamClassDoc.isHidden()) {
            needsUpdate = true;
            pamClassDoc.setHidden(true);
        }

        if (needsUpdate) {
            context.getWiki().saveDocument(pamClassDoc, "Update PAM user profile class", context);
        }
    }

    private boolean updateClass()
    {
        // Generate the class from scratch
        BaseClass newClass = new BaseClass();
        newClass.setDocumentReference(this.pamClass.getDocumentReference());
        createClass(newClass);

        // Apply standard class to current one
        return this.pamClass.apply(newClass, false);
    }

    private void createClass(BaseClass newClass)
    {
        newClass.addTextAreaField(PAM_XFIELD_UID, PAM_XFIELDPN_UID, 80, 1);
        TextAreaClass textAreaClass = (TextAreaClass) newClass.get(PAM_XFIELD_UID);
        textAreaClass.setContentType("PureText");
        newClass.addTextField(PAM_XFIELD_USER_NAME, PAM_XFIELDPN_USER_NAME, 80);
    }

    /**
     * @param userDocument the user profile page.
     * @return the userName store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getUserName(XWikiDocument userDocument)
    {
        BaseObject pamObject = userDocument.getXObject(this.pamClass.getDocumentReference());

        return pamObject == null ? null : getUserName(pamObject);
    }

    /**
     * @param pamObject the pam profile object.
     * @return the userName store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getUserName(BaseObject pamObject)
    {
        String userName = pamObject.getStringValue(PAM_XFIELD_USER_NAME);

        return userName.length() == 0 ? null : userName;
    }

    /**
     * @param userDocument the user profile page.
     * @return the uid store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getUid(XWikiDocument userDocument)
    {
        String uid = null;

        if (userDocument != null) {
            BaseObject pamObject = userDocument.getXObject(this.pamClass.getDocumentReference());

            if (pamObject != null) {
                uid = getUid(pamObject);
            }
        }

        return uid;
    }

    /**
     * @param pamObject the pam profile object.
     * @return the uid store in the user profile. Null if it can't find any or if it's empty.
     */
    public String getUid(BaseObject pamObject)
    {
        String uid = pamObject.getStringValue(PAM_XFIELD_UID);

        return uid.length() == 0 ? null : uid;
    }

    /**
     * Update PAM profile object with provided PAM user informations.
     * 
     * @param userDocument the user profile page to update.
     * @param userName the userName to store in the PAM profile.
     * @param uid the uid to store in the PAM profile.
     * @return true if modifications has been made to provided user profile, false otherwise.
     */
    public boolean updatePAMObject(XWikiDocument userDocument, String userName, String uid)
    {
        BaseObject pamObject = userDocument.getXObject(this.pamClass.getDocumentReference(), true, this.context);
        Map<String, String> map = new HashMap<String, String>();
        boolean needsUpdate = false;

        String objUserName = getUserName(pamObject);
        if (!userName.equalsIgnoreCase(objUserName)) {
            map.put(PAM_XFIELD_USER_NAME, userName);
            needsUpdate = true;
        }

        String objUid = getUid(pamObject);
        if (!uid.equalsIgnoreCase(objUid)) {
            map.put(PAM_XFIELD_UID, uid);
            needsUpdate = true;
        }

	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("storing PAM object: objUserName={}, objUid={}, userName={}, uid={}, needsUpdate={}",
			 objUserName, objUid, userName, uid, needsUpdate);
	}

        if (needsUpdate) {
            this.pamClass.fromMap(map, pamObject);
        }

        return needsUpdate;
    }

    /**
     * Search the XWiki storage for a existing user profile with provided PAM user uid stored.
     * <p>
     * If more than one profile is found the first one in returned and an error is logged.
     * 
     * @param uid the PAM unique id.
     * @return the user profile containing PAM uid.
     */
    public XWikiDocument searchDocumentByUserName(String userName)
    {
        XWikiDocument doc = null;

        List<XWikiDocument> documentList;
        try {
            // Search for userName in database, make sure to compare userNames lower cased to make to to not take into account the
            // case since PAM does not
            String sql =
                ", BaseObject as obj, StringProperty as prop where doc.fullName=obj.name and obj.className=? and obj.id=prop.id.id and prop.name=? and lower(prop.value)=?";

            documentList = this.context.getWiki().getStore().searchDocuments(sql, false, false, false, 0, 0,
                Arrays.asList(PAM_XCLASS, PAM_XFIELD_USER_NAME, userName.toLowerCase()), this.context);
        } catch (XWikiException e) {
            LOGGER.error("Fail to search for document containing pam userName [" + userName + "]", e);

            documentList = Collections.emptyList();
        }

	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("counted {} docs searching for id={}, {}={}",
			 documentList.size(), PAM_XCLASS, PAM_XFIELD_USER_NAME, userName.toLowerCase());
	}	

        if (documentList.size() > 1) {
            LOGGER.error("There is more than one user profile for PAM userName [" + userName + "]");
        }

        if (!documentList.isEmpty()) {
            doc = documentList.get(0);
        }

        return doc;
    }
}
