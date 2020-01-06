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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

public class XWikiPAMAuthServiceImplTest {
    private static final Logger log = LoggerFactory.getLogger(XWikiPAMAuthServiceImplTest.class);

    @Before
    public void setup() {
	if (log.isDebugEnabled()) {
	    log.debug("setting up...");
	}
    }

    @After
    public void tearDown() {
	if (log.isDebugEnabled()) {
	    log.debug("tearing down...");
	}
    }

    @Test
    public void testAuth() throws Exception {
	//XWikiPAMAuthServiceImpl impl = new XWikiPAMAuthServiceImpl();
        assertEquals(1, 1);
    }
}
