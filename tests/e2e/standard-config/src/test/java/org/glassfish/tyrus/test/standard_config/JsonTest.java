/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
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
import java.io.StringReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.coder.CoderAdapter;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the JSON format.
 *
 * @author Stepan Kopriva
 */
public class JsonTest extends TestContainer {

    private CountDownLatch messageLatch;

    private String receivedMessage;

    private static final String SENT_MESSAGE = "{\"NAME\" : \"Danny\"}";

    private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

    @Test
    public void testJson() throws DeploymentException {
        Server server = startServer(JsonTestEndpoint.class);

        messageLatch = new CountDownLatch(1);

        try {
            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: " + message);
                                receivedMessage = message;
                                messageLatch.countDown();
                            }
                        });
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(JsonTestEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertTrue("The received message is {REPLY : Danny}",
                              receivedMessage.equals("{\"REPLY\":\"Danny\"}"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    /**
     * @author Danny Coward
     */
    @ServerEndpoint(
            value = "/json2",
            encoders = {JsonEncoder.class},
            decoders = {JsonDecoder.class}
    )
    public static class JsonTestEndpoint {

        @OnMessage
        public JsonObject helloWorld(JsonObject message) {
            return Json.createObjectBuilder().add("REPLY", message.get("NAME")).build();
        }

    }

    /**
     * @author Danny Coward
     */
    public static class JsonDecoder extends CoderAdapter implements Decoder.Text<JsonObject> {

        @Override
        public JsonObject decode(String s) throws DecodeException {
            try {
                JsonObject jsonObject = Json.createReader(new StringReader(s)).readObject();
                return jsonObject;
            } catch (JsonException je) {
                throw new DecodeException(s, "JSON not decoded", je);
            }
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }

    /**
     * @author Danny Coward
     */
    public static class JsonEncoder extends CoderAdapter implements Encoder.Text<JsonObject> {

        @Override
        public String encode(JsonObject o) throws EncodeException {
            return o.toString();
        }
    }
}
