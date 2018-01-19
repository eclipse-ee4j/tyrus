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

package org.glassfish.tyrus.test.standard_config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.core.MaskingKeyGenerator;
import org.glassfish.tyrus.core.extension.ExtendedExtension;
import org.glassfish.tyrus.core.frame.Frame;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the use of a custom masking key generator.
 *
 * @author Petr Janouch
 */
public class CustomMaskingKeyGeneratorTest extends TestContainer {

    private static final int MASKING_KEY = 123456;

    @Test
    public void testCustomMaskingKeyGenerator() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            final int messageCount = 3;
            final CountDownLatch messageLatch = new CountDownLatch(messageCount);
            final AtomicInteger generatedMaskingKeyCounter = new AtomicInteger(0);

            ClientManager client = createClient();
            client.getProperties().put(ClientProperties.MASKING_KEY_GENERATOR, new MaskingKeyGenerator() {

                @Override
                public int nextInt() {
                    generatedMaskingKeyCounter.incrementAndGet();
                    return MASKING_KEY;
                }
            });

            AtomicInteger errorCounter = new AtomicInteger(0);

            List<Extension> extensions = new ArrayList<Extension>();
            extensions.add(new MaskingKeyCheckingExtension(true, errorCounter));

            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            messageLatch.countDown();
                        }
                    });

                    for (int i = 0; i < messageCount; i++) {
                        session.getAsyncRemote().sendText("hi");
                    }
                }
            }, ClientEndpointConfig.Builder.create().extensions(extensions).build(),
                                   getURI(AnnotatedServerEndpoint.class));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
            assertEquals(0, errorCounter.get());
            assertEquals(messageCount, generatedMaskingKeyCounter.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    private static class MaskingKeyCheckingExtension implements ExtendedExtension {

        private final boolean client;
        private final AtomicInteger errorCounter;

        MaskingKeyCheckingExtension(boolean client, AtomicInteger errorCounter) {
            this.client = client;
            this.errorCounter = errorCounter;
        }

        @Override
        public Frame processIncoming(ExtensionContext context, Frame frame) {
            return frame;
        }

        @Override
        public Frame processOutgoing(ExtensionContext context, Frame frame) {
            if (client && MASKING_KEY != frame.getMaskingKey()) {
                // exception thrown by JUnit assert would be swallowed by extension error handling mechanism
                errorCounter.incrementAndGet();
            }

            return frame;
        }

        @Override
        public List<Parameter> onExtensionNegotiation(ExtensionContext context, List<Parameter> requestedParameters) {
            return null;
        }

        @Override
        public void onHandshakeResponse(ExtensionContext context, List<Parameter> responseParameters) {

        }

        @Override
        public void destroy(ExtensionContext context) {

        }

        @Override
        public String getName() {
            return "MaskingKeyCheckingExtension";
        }

        @Override
        public List<Parameter> getParameters() {
            return null;
        }
    }

    @ServerEndpoint(value = "/customMaskingKeyTest", configurator = ServerConfig.class)
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    public static class ServerConfig extends ServerEndpointConfig.Configurator {

        @Override
        public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
            List<Extension> extensions = new ArrayList<Extension>();
            extensions.add(new MaskingKeyCheckingExtension(false, null));
            return extensions;
        }
    }
}
