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

package org.glassfish.tyrus.test.standard_config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.coder.CoderAdapter;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.TestEndpoint;
import org.glassfish.tyrus.test.standard_config.message.StringContainer;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the decoding and message handling of custom object.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class DecodedObjectTest extends TestContainer {

    private CountDownLatch messageLatch;

    private static String receivedMessage;
    private static final String receivedTextMessage = null;
    private static final String SENT_MESSAGE = "hello";

    @Test
    public void testSimpleDecoder() throws DeploymentException {

        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);
            ArrayList<Class<? extends Decoder>> decoders = new ArrayList<Class<? extends Decoder>>();
            decoders.add(CustomDecoder.class);
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().decoders(decoders).build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new DecodedMessageHandler());
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
            assertTrue("The received message is the same as the sent one", receivedMessage.equals(SENT_MESSAGE));
            assertNull("The message was not received via the TextMessageHandler", receivedTextMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testDecodeException() throws DeploymentException {

        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);
            ArrayList<Class<? extends Decoder>> decoders = new ArrayList<Class<? extends Decoder>>();
            decoders.add(CustomDecoderThrowingDecodeException.class);
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().decoders(decoders).build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new DecodedMessageHandler());
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Session session, Throwable thr) {
                    if (thr instanceof DecodeException) {
                        messageLatch.countDown();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testExtendedDecoded() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);
            ArrayList<Class<? extends Decoder>> decoders = new ArrayList<Class<? extends Decoder>>();
            decoders.add(ExtendedDecoder.class);
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().decoders(decoders).build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        System.out.println("#### onOpen Client side ####");
                        // session.addMessageHandler(new ObjectMessageHandler());
                        session.addMessageHandler(new DecodedMessageHandler());
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
            assertTrue("The received message is the same as the sent one",
                       receivedMessage.equals("Extended " + SENT_MESSAGE));
            assertNull("The message was not received via the TextMessageHandler", receivedTextMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    public static class CustomDecoder extends CoderAdapter implements Decoder.Text<StringContainer> {

        @Override
        public StringContainer decode(String s) throws DecodeException {
            return new StringContainer(s);
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }

    public static class CustomDecoderThrowingDecodeException extends CoderAdapter
            implements Decoder.Text<StringContainer> {

        @Override
        public StringContainer decode(String s) throws DecodeException {
            System.out.println(CustomDecoderThrowingDecodeException.class.getName());
            throw new DecodeException(s, s);
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }

    public static class ExtendedDecoder extends CoderAdapter implements Decoder.Text<ExtendedStringContainer> {

        @Override
        public ExtendedStringContainer decode(String s) throws DecodeException {
            return new ExtendedStringContainer(s);
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }

    public static class ExtendedStringContainer extends StringContainer {
        public ExtendedStringContainer(String string) {
            super("Extended " + string);
        }
    }

    class DecodedMessageHandler implements MessageHandler.Whole<StringContainer> {

        @Override
        public void onMessage(StringContainer customObject) {
            System.out.println("### DecodedMessageHandler ### " + customObject.getString());
            DecodedObjectTest.receivedMessage = customObject.getString();
            messageLatch.countDown();
        }
    }
}
