/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core;

import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Writer;
import org.glassfish.tyrus.spi.WriterInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WriterInfoTest {

    private TyrusEndpointWrapper endpointWrapper;
    private static final String TEST_TEXT = "TEST";

    @Before
    public void setUp() throws DeploymentException {
        endpointWrapper = new TyrusEndpointWrapper(DummyEndpoint.class, null, ComponentProviderService.create(), null, null,
                            null, null, null, null, null);
    }

    @Test
    public void sendBasicTest() throws IOException, EncodeException {
        AtomicReference<WriterInfo> info = new AtomicReference<>();

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createSession(tre);
        tre.setWriterInfo(info);

        //Basic
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        info.set(new WriterInfo(null, null));
        rew.sendText(TEST_TEXT);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.TEXT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendText(TEST_TEXT, false);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.TEXT_CONTINUATION);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendText(TEST_TEXT, true);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.TEXT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendBinary(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.BINARY);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendBinary(ByteBuffer.wrap(TEST_TEXT.getBytes()), false);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.BINARY_CONTINUATION);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendBinary(ByteBuffer.wrap(TEST_TEXT.getBytes()), true);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.BINARY);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendObject(TEST_TEXT);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.OBJECT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.BASIC);

        info.set(new WriterInfo(null, null));
        rew.sendPing(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.PING);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.SUPER);

        info.set(new WriterInfo(null, null));
        rew.sendPong(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.PONG);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.SUPER);
    }

    @Test
    public void setAsyncTest() throws IOException {
        AtomicReference<WriterInfo> info = new AtomicReference<>();
        SendHandler handler = new SendHandler() {
            @Override
            public void onResult(SendResult sendResult) {
                // do nothing
            }
        };

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createSession(tre);
        tre.setWriterInfo(info);

        //Async
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        info.set(new WriterInfo(null, null));
        rew.sendText(TEST_TEXT);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.TEXT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendText(TEST_TEXT, handler);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.TEXT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendText(TEST_TEXT, handler);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.TEXT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendBinary(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.BINARY);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendBinary(ByteBuffer.wrap(TEST_TEXT.getBytes()), handler);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.BINARY);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendBinary(ByteBuffer.wrap(TEST_TEXT.getBytes()), handler);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.BINARY);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendObject(TEST_TEXT);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.OBJECT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendObject(TEST_TEXT, handler);
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.OBJECT);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.ASYNC);

        info.set(new WriterInfo(null, null));
        rew.sendPing(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.PING);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.SUPER);

        info.set(new WriterInfo(null, null));
        rew.sendPong(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        Assert.assertEquals(info.get().getMessageType(), WriterInfo.MessageType.PONG);
        Assert.assertEquals(info.get().getRemoteEndpointType(), WriterInfo.RemoteEndpointType.SUPER);
    }

    private TyrusSession createSession(TestRemoteEndpoint testRemoteEndpoint) {
        return new TyrusSession(null, new TestRemoteEndpoint(), endpointWrapper, null, null, false, null, null, null,
                null, new HashMap<String, List<String>>(), null, null, null, null, 0, TyrusConfiguration.EMPTY_CONFIGURATION,
                new DebugContext()) {
            @Override
            void restartIdleTimeoutExecutor() {
                // do nothing
            }
        };
    }

    private static class TestRemoteEndpoint extends TyrusWebSocket {

        private TestRemoteEndpoint() {
            super(new ProtocolHandler(false, null), null);
        }

        private void setWriterInfo(AtomicReference<WriterInfo> writerInfoReference) {
            getProtocolHandler().setWriter(new TestWriter(writerInfoReference));
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    private static class TestWriter extends Writer {
        private final AtomicReference<WriterInfo> writerInfo;
        private TestWriter(AtomicReference<WriterInfo> writerInfo) {
            this.writerInfo = writerInfo;
        }

        @Override
        public void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler) {
        }

        @Override
        public void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler, WriterInfo writerInfo) {
            this.writerInfo.set(writerInfo);
            completionHandler.completed(ByteBuffer.wrap(TEST_TEXT.getBytes()));
        }

        @Override
        public void close() throws IOException {
        }
    }

    @ServerEndpoint(value = "/")
    private static class DummyEndpoint extends Endpoint {

        @Override
        public void onOpen(Session session, EndpointConfig endpointConfig) {

        }
    }
}
