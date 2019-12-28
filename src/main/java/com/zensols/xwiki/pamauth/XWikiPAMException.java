package com.zensols.xwiki.pamauth;

import com.xpn.xwiki.XWikiException;

/**
 * PAM plugin base exception.
 * 
 * @version $Id$
 * @since 10.11
 */
public class XWikiPAMException extends XWikiException
{
    /**
     * Create new instance of PAM exception.
     * 
     * @param message error message.
     */
    public XWikiPAMException(String message)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, message);
    }
    
    /**
     * Create new instance of PAM exception.
     * 
     * @param message error message.
     * @param e the wrapped exception.
     */
    public XWikiPAMException(String message, Exception e)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, message, e);
    }
}
