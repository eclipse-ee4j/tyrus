/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.tyrus.tests.qa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class BrowserTest {

    private static final Logger logger = Logger.getLogger(BrowserTest.class.getCanonicalName());

    @After
    public void cleanup() {
        SeleniumToolkit.quit();
    }

    private void simpleClientHandShake(SeleniumToolkit.Browser browser) throws InterruptedException {
        logger.log(Level.INFO, "===============testSimpleClientHandshake===============");
        TestScenarios ts = new TestScenarios(new SeleniumToolkit(browser));
        ts.testSimpleHandshake();
        logger.log(Level.INFO, "========================================================");
    }

    private void clientChat(SeleniumToolkit.Browser browser) throws InterruptedException, Exception {
        logger.log(Level.INFO, "============testClientChatApp=================");
        TestScenarios ts = new TestScenarios(new SeleniumToolkit(browser));
        ts.testChatSample();
        logger.log(Level.INFO, "==================================================");
    }

    private void twoClientsChat(SeleniumToolkit.Browser alice, SeleniumToolkit.Browser bob) throws InterruptedException,
            Exception {
        logger.log(Level.INFO, "============testClientChatWithTwoUsers=================");
        SeleniumToolkit aliceBrowser = new SeleniumToolkit(alice);
        SeleniumToolkit bobBrowser = new SeleniumToolkit(bob);
        TestScenarios ts = new TestScenarios(aliceBrowser, bobBrowser);
        ts.testChatSampleWithTwoUsers();
        logger.log(Level.INFO, "==============================================================");
    }

    private void chatScalabitlity(SeleniumToolkit.Browser browser) throws InterruptedException, Exception {
        logger.log(Level.INFO, "=============testScalabilityWith" + TestScenarios.MAX_CHAT_CLIENTS +
                "Users===============================");
        List<SeleniumToolkit> toolkits = new ArrayList<SeleniumToolkit>();
        // Launch 100 browsers
        for (int idx = 0; idx < TestScenarios.MAX_CHAT_CLIENTS; idx++) {
            toolkits.add(new SeleniumToolkit(browser));
        }
        TestScenarios ts = new TestScenarios(toolkits.toArray(new SeleniumToolkit[toolkits.size()]));
        ts.testChatSampleWith100Users();
        logger.log(Level.INFO, "==============================================================");
    }

    private void auctionTest(SeleniumToolkit.Browser browser) throws InterruptedException, Exception {
        logger.log(Level.INFO, "============testClientAuction=================");
        TestScenarios ts = new TestScenarios(new SeleniumToolkit(browser));
        ts.testAuctionSample();
        logger.log(Level.INFO, "=====================================================");
    }

    @Test
    public void testFirefoxClientSimpleHandshake() throws InterruptedException {
        simpleClientHandShake(SeleniumToolkit.Browser.FIREFOX);
    }

    @Test
    public void testFirefoxClientChat() throws InterruptedException, Exception {
        clientChat(SeleniumToolkit.Browser.FIREFOX);
    }

    @Test
    public void testFirefoxClientChatWithTwoUsers() throws InterruptedException, Exception {
        twoClientsChat(SeleniumToolkit.Browser.FIREFOX, SeleniumToolkit.Browser.FIREFOX);
    }

    @Test
    public void testFirefoxClientChatWith100Users() throws InterruptedException, Exception {
        chatScalabitlity(SeleniumToolkit.Browser.FIREFOX);
    }

    @Test
    public void testFirefoxClientAuction() throws InterruptedException, Exception {
        auctionTest(SeleniumToolkit.Browser.FIREFOX);
    }

    @Ignore
    @Test
    public void testChromeClient() throws InterruptedException {
        simpleClientHandShake(SeleniumToolkit.Browser.CHROME);
    }

    @Ignore
    @Test
    public void testChromeClientChat() throws InterruptedException, Exception {
        clientChat(SeleniumToolkit.Browser.CHROME);
    }

    @Ignore
    @Test
    public void testChromefoxClientChatWithTwoUsers() throws InterruptedException, Exception {
        twoClientsChat(SeleniumToolkit.Browser.CHROME, SeleniumToolkit.Browser.CHROME);
    }

    @Ignore
    @Test
    public void testChromeClientChatWith100Users() throws InterruptedException, Exception {
        chatScalabitlity(SeleniumToolkit.Browser.CHROME);
    }

    @Ignore
    @Test
    public void testChromeClientAuction() throws InterruptedException, Exception {
        auctionTest(SeleniumToolkit.Browser.CHROME);
    }

    //
    // Visit http://code.google.com/p/selenium/wiki/SafariDriver to know more about Safari Driver
    // installation.
    //
    @Ignore
    @Test
    public void testSafariClient() throws InterruptedException {
        Assume.assumeTrue(SeleniumToolkit.safariPlatform());
        simpleClientHandShake(SeleniumToolkit.Browser.SAFARI);
    }

    @Ignore
    @Test
    public void testSafariClientChat() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.safariPlatform());
        clientChat(SeleniumToolkit.Browser.SAFARI);
    }

    @Ignore
    @Test
    public void testSafariClientChatWithTwoUsers() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.safariPlatform());
        twoClientsChat(SeleniumToolkit.Browser.SAFARI, SeleniumToolkit.Browser.SAFARI);
    }

    @Ignore
    @Test
    public void testSafariClientChatWith100Users() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.safariPlatform());
        chatScalabitlity(SeleniumToolkit.Browser.SAFARI);
    }

    @Ignore
    @Test
    public void testSafariClientAuction() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.safariPlatform());
        auctionTest(SeleniumToolkit.Browser.SAFARI);
    }

    @Test
    public void testInternetExplorerClientSimpleHandshake() throws InterruptedException {
        Assume.assumeTrue(SeleniumToolkit.onWindows()); // skip this test on non-Windows platforms
        simpleClientHandShake(SeleniumToolkit.Browser.IE);
    }

    @Test
    public void testInternetExplorerClientChat() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.onWindows()); // skip this test on non-Windows platforms
        clientChat(SeleniumToolkit.Browser.IE);
    }

    @Test
    public void testInternetExplorerClientChatWithTwoUsers() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.onWindows()); // skip this test on non-Windows platforms
        twoClientsChat(SeleniumToolkit.Browser.IE, SeleniumToolkit.Browser.IE);
    }

    @Test
    @Ignore
    public void testInterentExplorerClientChatWith100Users() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.onWindows()); // skip this test on non-Windows platforms
        chatScalabitlity(SeleniumToolkit.Browser.IE);
    }

    @Test
    @Ignore
    public void testInternetExplorerClientAuction() throws InterruptedException, Exception {
        Assume.assumeTrue(SeleniumToolkit.onWindows()); // skip this test on non-Windows platforms
        auctionTest(SeleniumToolkit.Browser.IE);
    }
}
