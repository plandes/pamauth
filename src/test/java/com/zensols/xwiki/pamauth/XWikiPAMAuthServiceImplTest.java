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
