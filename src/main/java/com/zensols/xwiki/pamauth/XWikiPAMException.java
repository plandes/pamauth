/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.zensols.xwiki.pamauth;

import com.xpn.xwiki.XWikiException;

/**
 * PAM plugin base exception.
 * 
 * @version $Id$
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
