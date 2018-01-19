/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.ext.client.java8;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.core.coder.CoderAdapter;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SessionBuilderTest extends TestContainer {

    @ServerEndpoint("/sessionBuilderTest")
    public static class SessionBuilderTestEndpoint {

        @OnMessage
        public String onMessage(Session session, String message) {
            return message;
        }

        @OnMessage
        public byte[] onMessage(byte[] message) {
            return message;
        }
    }

    private static final String MESSAGE = "I find your lack of faith disturbing";

    @Test
    public void testEcho() throws IOException, DeploymentException, InterruptedException {
        Server server = startServer(SessionBuilderTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            Session session = new SessionBuilder()
                    .uri(getURI(SessionBuilderTestEndpoint.class))
                    .messageHandler(String.class,
                                    message -> {
                                        if (MESSAGE.equals(message)) {
                                            messageLatch.countDown();
                                        }
                                    })
                    .connect();

            session.getBasicRemote().sendText(MESSAGE);

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testEchoPartial() throws IOException, DeploymentException, InterruptedException {
        Server server = startServer(SessionBuilderTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            Session session = new SessionBuilder()
                    .uri(getURI(SessionBuilderTestEndpoint.class))
                    .messageHandlerPartial(String.class,
                                           (message, complete) -> {
                                               System.out.println("partial: " + message + " " + complete);

                                               if (MESSAGE.equals(message) && complete) {
                                                   messageLatch.countDown();
                                               }
                                           })
                    .connect();

            session.getBasicRemote().sendText(MESSAGE);

            assertTrue(messageLatch.await(30000000, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testEchoBinary() throws IOException, DeploymentException, InterruptedException {
        Server server = startServer(SessionBuilderTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            Session session = new SessionBuilder()
                    .uri(getURI(SessionBuilderTestEndpoint.class))
                    .messageHandler(byte[].class,
                                    message -> {
                                        if (MESSAGE.equals(new String(message))) {
                                            messageLatch.countDown();
                                        }
                                    })
                    .connect();

            session.getBasicRemote().sendBinary(ByteBuffer.wrap(MESSAGE.getBytes()));

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testEchoAsync() throws IOException, DeploymentException, InterruptedException {
        Server server = startServer(SessionBuilderTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            CompletableFuture<Session> sessionCompletableFuture = new SessionBuilder()
                    .uri(getURI(SessionBuilderTestEndpoint.class))
                    .messageHandler(String.class,
                                    message -> {
                                        if (MESSAGE.equals(message)) {
                                            messageLatch.countDown();
                                        }
                                    })
                    .connectAsync();

            sessionCompletableFuture.thenApply(new Function<Session, Session>() {
                @Override
                public Session apply(Session session) {
                    try {
                        session.getBasicRemote().sendText(MESSAGE);
                    } catch (IOException ignored) {
                    }
                    return session;
                }
            });


            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testEchoAsyncCustomES() throws IOException, DeploymentException, InterruptedException {
        Server server = startServer(SessionBuilderTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            CompletableFuture<Session> sessionCompletableFuture = new SessionBuilder()
                    .uri(getURI(SessionBuilderTestEndpoint.class))
                    .messageHandler(String.class,
                                    message -> {
                                        if (MESSAGE.equals(message)) {
                                            messageLatch.countDown();
                                        }
                                    })
                    .connectAsync(Executors.newCachedThreadPool());

            sessionCompletableFuture.thenApply(new Function<Session, Session>() {
                @Override
                public Session apply(Session session) {
                    try {
                        session.getBasicRemote().sendText(MESSAGE);
                    } catch (IOException ignored) {
                    }
                    return session;
                }
            });


            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    public static class AClass {
        @Override
        public String toString() {
            return MESSAGE;
        }
    }

    public static class AClassCoder extends CoderAdapter implements Encoder.Text<AClass>, Decoder.Text<AClass> {

        @Override
        public String encode(AClass aClass) throws EncodeException {
            return aClass.toString();
        }

        @Override
        public AClass decode(String s) throws DecodeException {
            return new AClass();
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }

    @ServerEndpoint("/sessionBuilderEncDecTest")
    public static class SessionBuilderEncDecTestEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    @Test
    public void testEncoderDecoder() throws IOException, DeploymentException, InterruptedException, EncodeException {
        Server server = startServer(SessionBuilderEncDecTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientEndpointConfig clientEndpointConfig =
                ClientEndpointConfig.Builder.create()
                                            .encoders(Collections.singletonList(AClassCoder.class))
                                            .decoders(Collections.singletonList(AClassCoder.class))
                                            .build();

        try {
            Session session = new SessionBuilder()
                    .uri(getURI(SessionBuilderEncDecTestEndpoint.class))
                    .clientEndpointConfig(clientEndpointConfig)
                    .messageHandler(AClass.class,
                                    aClass -> {
                                        if (MESSAGE.equals(aClass.toString())) {
                                            messageLatch.countDown();
                                        }
                                    })
                    .connect();

            session.getBasicRemote().sendObject(new AClass());

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAllMethods() throws IOException, DeploymentException, InterruptedException, EncodeException {
        Server server = startServer(SessionBuilderEncDecTestEndpoint.class);

        CountDownLatch messageLatch = new CountDownLatch(1);
        CountDownLatch onOpenLatch = new CountDownLatch(1);
        CountDownLatch onCloseLatch = new CountDownLatch(1);
        CountDownLatch onErrorLatch = new CountDownLatch(1);

        final ClientEndpointConfig clientEndpointConfig =
                ClientEndpointConfig.Builder.create()
                                            .encoders(Collections.singletonList(AClassCoder.class))
                                            .decoders(Collections.singletonList(AClassCoder.class))
                                            .build();

        try {
            Session session = new SessionBuilder()
                    .uri(getURI(SessionBuilderEncDecTestEndpoint.class))
                    .clientEndpointConfig(clientEndpointConfig)
                    .messageHandler(AClass.class,
                                    aClass -> {
                                        if (MESSAGE.equals(aClass.toString())) {
                                            messageLatch.countDown();
                                        }
                                    })
                    .onOpen((session1, endpointConfig) -> onOpenLatch.countDown())
                    .onError((session1, throwable) -> onErrorLatch.countDown())
                    .onClose((session1, closeReason) -> {
                        onCloseLatch.countDown();
                        throw new RuntimeException("onErrorTrigger");
                    })
                    .connect();

            session.getBasicRemote().sendObject(new AClass());

            assertTrue(onOpenLatch.await(3, TimeUnit.SECONDS));
            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

            session.close();

            assertTrue(onCloseLatch.await(3, TimeUnit.SECONDS));
            assertTrue(onErrorLatch.await(3, TimeUnit.SECONDS));

        } finally {
            stopServer(server);
        }
    }
}
