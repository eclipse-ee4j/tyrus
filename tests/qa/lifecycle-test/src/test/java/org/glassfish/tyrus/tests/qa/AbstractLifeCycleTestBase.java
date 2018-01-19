/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.tests.qa.config.AppConfig;
import org.glassfish.tyrus.tests.qa.lifecycle.LifeCycleDeployment;
import org.glassfish.tyrus.tests.qa.tools.GlassFishToolkit;
import org.glassfish.tyrus.tests.qa.tools.ServerToolkit;
import org.glassfish.tyrus.tests.qa.tools.SessionController;
import org.glassfish.tyrus.tests.qa.tools.TyrusToolkit;

import org.junit.After;
import org.junit.Before;

import junit.framework.Assert;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Conos (michal.conos at oracle.com)
 */
public abstract class AbstractLifeCycleTestBase {

    protected static final Logger logger = Logger.getLogger(AbstractLifeCycleTestBase.class.getCanonicalName());
    AppConfig testConf = new AppConfig(
            LifeCycleDeployment.CONTEXT_PATH,
            LifeCycleDeployment.LIFECYCLE_ENDPOINT_PATH,
            LifeCycleDeployment.COMMCHANNEL_SCHEME,
            LifeCycleDeployment.COMMCHANNEL_HOST,
            LifeCycleDeployment.COMMCHANNEL_PORT,
            LifeCycleDeployment.INSTALL_ROOT);
    ServerToolkit tyrus;

    //private Server tyrusServer;
    @Before
    public void setupServer() throws Exception {
        if (AppConfig.isGlassFishContainer()) {
            tyrus = new GlassFishToolkit(testConf);
        } else {
            tyrus = new TyrusToolkit(testConf);
        }
        SessionController.resetState();

    }

    @After
    public void stopServer() {
        tyrus.stopServer();
    }

    protected Session deployClient(Class client, URI connectURI) throws DeploymentException, IOException {
        return deployClient(client, connectURI, ClientEndpointConfig.Builder.create().build());
    }

    protected Session deployClient(Class client, URI connectURI, ClientEndpointConfig cec) throws DeploymentException,
            IOException {
        WebSocketContainer wsc = ContainerProvider.getWebSocketContainer();
        logger.log(Level.INFO, "deployClient: registering client: {0}", client);
        logger.log(Level.INFO, "deployClient: connectTo: {0}", connectURI);
        logger.log(Level.INFO, "deployClient: subProtocols: {0}", cec.getPreferredSubprotocols());
        Session clientSession;
        if (Endpoint.class.isAssignableFrom(client)) {
            clientSession = wsc.connectToServer(
                    client,
                    cec,
                    connectURI);
        } else {
            clientSession = wsc.connectToServer(
                    client,
                    connectURI);
        }

        logger.log(Level.INFO, "deployClient: client session: {0}", clientSession);
        logger.log(Level.INFO, "deployClient: Negotiated subprotocol: {0}", clientSession.getNegotiatedSubprotocol());
        return clientSession;
    }

    protected void deployServer(Class config) throws DeploymentException {
        logger.log(Level.INFO, "registering server: {0}", config);
        tyrus.registerEndpoint(config);
        tyrus.startServer();
    }

    protected void lifeCycle(Class serverHandler, Class clientHandler) throws DeploymentException, IOException {
        lifeCycle(serverHandler, clientHandler, SessionController.SessionState.FINISHED_SERVER.getMessage(),
                  testConf.getURI(), null);
    }

    protected void lifeCycle(Class serverHandler, Class clientHandler, ClientEndpointConfig cec) throws
            DeploymentException, IOException {
        lifeCycle(serverHandler, clientHandler, SessionController.SessionState.FINISHED_SERVER.getMessage(),
                  testConf.getURI(), cec);
    }

    protected void lifeCycle(Class serverHandler, Class clientHandler, String state, URI clientUri,
                             ClientEndpointConfig cec) throws DeploymentException, IOException {
        Set<String> states = new HashSet<String>();
        states.add(state);
        lifeCycle(serverHandler, clientHandler, states, clientUri, cec);
    }

    protected void lifeCycle(Class serverHandler, Class clientHandler, Set state, URI clientUri,
                             ClientEndpointConfig cec) throws DeploymentException, IOException {
        final CountDownLatch stopConversation = new CountDownLatch(1);
        try {
            deployServer(serverHandler);
        } catch (DeploymentException e) {
            stopServer();
            throw e;
        }
        if (cec == null) {
            cec = ClientEndpointConfig.Builder.create().build();
        }
        Session clientSession = deployClient(clientHandler, clientUri, cec);
        // FIXME TC: clientSession.equals(lcSession)
        // FIXME TC: clientSession.addMessageHandler .. .throw excetpion
        try {
            if (System.getProperty("DEBUG_ON") != null) {
                stopConversation.await(LifeCycleDeployment.DEBUG_TIMEOUT, TimeUnit.SECONDS);
            } else {
                stopConversation.await(LifeCycleDeployment.NORMAL_TIMEOUT, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            // this is fine
        }
        /*
         if (stopConversation.getCount() != 0) {
         fail();
         }
         */

        //tyrus.stopServer();

        if (!state.isEmpty()) {
            String finalState = SessionController.getState();
            logger.log(Level.INFO, "Asserting: {0} contains {1}", new Object[]{state, finalState});
            if (!state.contains(finalState)) {
                Assert.fail("session lifecycle finished with final state: " + finalState + " expected:" + state);
            }
        }

        /*
         private void lifeCycleAnnotated(Class serverHandler, Class clientHandler) throws DeploymentException,
         InterruptedException, IOException {
    
         ServerAnnotatedConfiguration.registerServer("annotatedLifeCycle", serverHandler);
         ServerAnnotatedConfiguration.registerSessionController("annotatedSessionController", sc);
         tyrus.registerEndpoint(ServerAnnotatedConfiguration.class);
         final Server tyrusServer = tyrus.startServer();
         WebSocketContainer wsc = ContainerProvider.getWebSocketContainer();
         Session clientSession = wsc.connectToServer(
         AnnotatedClient.class,
         new ClientConfiguration(clientHandler, sc),
         testConf.getURI());
         Thread.sleep(10000);
         tyrus.stopServer(tyrusServer);
         Assert.assertEquals(sessionName + ": session lifecycle finished", SessionController.SessionState
         .FINISHED_SERVER.getMessage(), sc.getState());
         }
         */
    }

    protected void isMultipleAnnotationEx(Exception ex, String what) {
        if (ex == null || ex.getMessage() == null) {
            Assert.fail("isMultipleAnnotationEx: ex==null or ex.getMessage()==null");
        }
        if (!ex.getMessage().startsWith(what)) {
            Assert.fail(ex.getMessage());
        }
    }

    protected void multipleDeployment(Class server, Class client, String whichOne) {
        Exception exThrown = null;
        try {
            lifeCycle(server, client, Collections.EMPTY_SET, testConf.getURI(), null);
        } catch (Exception e) {
            exThrown = e;
            e.printStackTrace();
        }
        if (AppConfig.isGlassFishContainer()) {
            if (exThrown instanceof DeploymentException) {
                logger.log(Level.INFO, "The DeploymentExcetion looks good here");
            }
        } else {
            isMultipleAnnotationEx(exThrown, whichOne);
        }
    }
}
