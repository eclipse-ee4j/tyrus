/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.e2e.non_deployable;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.core.AnnotatedEndpoint;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.l10n.LocalizationMessages;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests warnings logged when max message size given in {@link javax.websocket.OnMessage} is larger than max message
 * size specified in a container. Cannot be moved to standard tests due the need to handle server logger.
 *
 * @author Petr Janouch
 */
public class MaxMessageSizeDeploymentTest extends TestContainer {

    private final Logger logger = Logger.getLogger(AnnotatedEndpoint.class.getName());

    @ServerEndpoint("/largeMaxMessageSizeServerEndpoint")
    public static class LargeMaxMessageSizeServerEndpoint {

        @OnMessage(maxMessageSize = 2)
        public void onTooBigMessage(String message) {
        }
    }

    @Test
    public void serverMaxMessageSizeTooLargeTest() throws DeploymentException, InterruptedException, IOException {
        Map<String, Object> serverProperties = getServerProperties();
        serverProperties.put(TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, 1);
        final AtomicBoolean warningLogged = new AtomicBoolean(false);
        LoggerHandler handler = new LoggerHandler() {
            @Override
            public void publish(LogRecord record) {
                String expectedWarningMessage =
                        LocalizationMessages.ENDPOINT_MAX_MESSAGE_SIZE_TOO_LONG(
                                2, LargeMaxMessageSizeServerEndpoint.class.getMethods()[0].getName(),
                                LargeMaxMessageSizeServerEndpoint.class.getName(), 1);
                System.out.println("Expected message: " + expectedWarningMessage);
                System.out.println("Logged message: " + record.getMessage());
                if (expectedWarningMessage.equals(record.getMessage())) {
                    warningLogged.set(true);
                }
            }
        };
        logger.setLevel(Level.CONFIG);
        logger.addHandler(handler);
        Server server = null;
        try {
            server = startServer(LargeMaxMessageSizeServerEndpoint.class);
        } finally {
            stopServer(server);
        }
        assertTrue(warningLogged.get());
        logger.removeHandler(handler);
    }

    /**
     * Tests that no warning is given during server endpoint deployment. It does not look for a specific message, but
     * checks that no warning is given, therefore it might fail, when other warnings than max message size check are
     * introduced.
     */
    @Test
    public void serverMaxMessageSizeOkTest() throws DeploymentException, InterruptedException, IOException {
        Map<String, Object> serverProperties = getServerProperties();
        serverProperties.put(TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, 3);
        final AtomicBoolean warningLogged = new AtomicBoolean(false);
        LoggerHandler handler = new LoggerHandler() {
            @Override
            public void publish(LogRecord record) {
                System.out.println("Logged message: " + record.getMessage());
                warningLogged.set(true);
            }
        };
        logger.setLevel(Level.CONFIG);
        logger.addHandler(handler);
        Server server = null;
        try {
            server = startServer(LargeMaxMessageSizeServerEndpoint.class);
        } finally {
            stopServer(server);
        }
        assertFalse(warningLogged.get());
        logger.removeHandler(handler);
    }

    @ClientEndpoint
    public static class LargeMaxMessageSizeClientEndpoint {

        @OnMessage(maxMessageSize = 2)
        public void onTooBigMessage(String message) {
        }

    }

    @ServerEndpoint("/dummyServerEndpoint")
    public static class DummyServerEndpoint {

    }

    @Test
    public void clientMaxMessageSizeTooLargeTest() throws DeploymentException {
        Server server = startServer(DummyServerEndpoint.class);
        try {
            ClientManager client = createClient();
            Map<String, Object> properties = client.getProperties();
            properties.put(ClientProperties.INCOMING_BUFFER_SIZE, 1);
            final AtomicBoolean warningLogged = new AtomicBoolean(false);
            LoggerHandler handler = new LoggerHandler() {
                @Override
                public void publish(LogRecord record) {
                    String expectedWarningMessage =
                            LocalizationMessages.ENDPOINT_MAX_MESSAGE_SIZE_TOO_LONG(
                                    2, LargeMaxMessageSizeClientEndpoint.class.getMethods()[0].getName(),
                                    LargeMaxMessageSizeClientEndpoint.class.getName(), 1);
                    System.out.println("Expected message: " + expectedWarningMessage);
                    System.out.println("Logged message: " + record.getMessage());
                    if (expectedWarningMessage.equals(record.getMessage())) {
                        warningLogged.set(true);
                    }
                }
            };
            logger.setLevel(Level.CONFIG);
            logger.addHandler(handler);
            client.connectToServer(LargeMaxMessageSizeClientEndpoint.class, getURI(DummyServerEndpoint.class, "ws"));
            assertTrue(warningLogged.get());
            logger.removeHandler(handler);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Tests that no warning is given during client endpoint deployment. It does not look for a specific message, but
     * checks that no warning is given, therefore it might fail, when other warnings than max message size check are
     * introduced.
     */
    @Test
    public void clientMaxMessageSizeOkTest() throws DeploymentException {
        Server server = startServer(DummyServerEndpoint.class);
        try {
            ClientManager client = createClient();
            Map<String, Object> properties = client.getProperties();
            properties.put(ClientProperties.INCOMING_BUFFER_SIZE, 3);
            final AtomicBoolean warningLogged = new AtomicBoolean(false);
            LoggerHandler handler = new LoggerHandler() {
                @Override
                public void publish(LogRecord record) {
                    System.out.println("Logged message: " + record.getMessage());
                    warningLogged.set(true);
                }
            };
            logger.setLevel(Level.CONFIG);
            logger.addHandler(handler);
            client.connectToServer(LargeMaxMessageSizeClientEndpoint.class, getURI(DummyServerEndpoint.class, "ws"));
            assertFalse(warningLogged.get());
            logger.removeHandler(handler);

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    private abstract static class LoggerHandler extends Handler {

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }

        @Override
        public synchronized Level getLevel() {
            return Level.CONFIG;
        }
    }

}
