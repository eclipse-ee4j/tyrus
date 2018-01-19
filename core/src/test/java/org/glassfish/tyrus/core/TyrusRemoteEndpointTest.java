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

package org.glassfish.tyrus.core;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.core.frame.Frame;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the RemoteEndpointWrapper.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TyrusRemoteEndpointTest {

    private final byte[] sentBytes = {'a', 'b', 'c'};
    private final byte[] sentBytesComplete = {'a', 'b', 'c', 'a', 'b', 'c'};
    private TyrusEndpointWrapper endpointWrapper;

    public TyrusRemoteEndpointTest() {
        try {
            endpointWrapper = new TyrusEndpointWrapper(EchoEndpoint.class, null, ComponentProviderService.create(),
                                                       new TestContainer(), null, null, null, null, null, null);
        } catch (DeploymentException e) {
            // do nothing.
        }
    }

    @Test
    public void testGetSendStream() throws IOException {

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        OutputStream stream = rew.getSendStream();

        for (byte b : sentBytes) {
            stream.write(b);
        }

        stream.flush();

        // Assert.assertArrayEquals("Writing bytes one by one to stream and flushing.", sentBytes, tre
        // .getBytesAndClearBuffer());

        stream.write(sentBytes);
        stream.close();

        Assert.assertArrayEquals("Writing byte[] to stream and flushing.", sentBytesComplete,
                                 tre.getBytesAndClearBuffer());
    }

    @Test
    public void testGetSendStreamWriteArrayWhole() throws IOException {

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        OutputStream stream = rew.getSendStream();

        stream.write(sentBytesComplete);
        Assert.assertEquals(6, tre.getLastSentMessageSize());
        stream.close();
        Assert.assertEquals(0, tre.getLastSentMessageSize());

        Assert.assertArrayEquals("Writing byte[] to stream and flushing.", sentBytesComplete,
                                 tre.getBytesAndClearBuffer());
    }

    @Test
    public void testGetSendStreamWriteArrayPerPartes() throws IOException {

        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        OutputStream stream = rew.getSendStream();

        stream.write(sentBytes);
        Assert.assertEquals(3, tre.getLastSentMessageSize());
        stream.write(sentBytes);
        Assert.assertEquals(3, tre.getLastSentMessageSize());
        stream.close();
        Assert.assertEquals(0, tre.getLastSentMessageSize());

        Assert.assertArrayEquals("Writing byte[] to stream and flushing.", sentBytesComplete,
                                 tre.getBytesAndClearBuffer());
    }


    @Test
    public void testGetSendWriter() throws IOException {
        final String sentString = "abc";

        char[] toSend = sentString.toCharArray();
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);
        Writer writer = rew.getSendWriter();

        writer.write(toSend, 0, 3);
        writer.flush();
        Assert.assertEquals("Writing the whole message.", sentString, tre.getStringAndCleanBuilder());

        writer.write(toSend, 0, 1);
        writer.flush();
        Assert.assertEquals("Writing first character.", String.valueOf(toSend[0]), tre.getStringAndCleanBuilder());

        writer.write(toSend, 2, 1);
        writer.flush();
        Assert.assertEquals("Writing first character.", String.valueOf(toSend[2]), tre.getStringAndCleanBuilder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendText() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendBinary() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendBinary(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendPartialText() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendText(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendPartialBinary() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendBinary(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBasicSendObject() throws IOException, EncodeException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Basic rew = new TyrusRemoteEndpoint.Basic(testSession, tre, endpointWrapper);

        rew.sendObject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendTextHandler1() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendText(null, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                // do nothing.
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendTextHandler2() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendText("We are all here to do what we are all here to do...", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendTextFuture() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendText(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendBinaryHandler1() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendBinary(null, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                // do nothing.
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendBinaryHandler2() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendBinary(ByteBuffer.wrap("We are all here to do what we are all here to do...".getBytes("UTF-8")), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendBinaryFuture() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendBinary(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendObjectHandler1() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendObject(null, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                // do nothing.
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendObjectHandler2() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendObject("We are all here to do what we are all here to do...", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAsyncSendObjectFuture() throws IOException {
        TestRemoteEndpoint tre = new TestRemoteEndpoint();
        TyrusSession testSession = createTestSession(tre, endpointWrapper);
        TyrusRemoteEndpoint.Async rew = new TyrusRemoteEndpoint.Async(testSession, tre, endpointWrapper);

        rew.sendObject(null);
    }

    private TyrusSession createTestSession(TyrusWebSocket webSocket, TyrusEndpointWrapper endpointWrapper) {
        return new TyrusSession(null, webSocket, endpointWrapper, null, null, true, null, null,
                                Collections.<String, String>emptyMap(), null, new HashMap<String, List<String>>(), null,
                                null, null, new DebugContext());
    }

    private class TestRemoteEndpoint extends TyrusWebSocket {

        private final ArrayList<Byte> bytesToSend = new ArrayList<Byte>();
        StringBuilder builder = new StringBuilder();
        private int lastSentMessageSize;

        private TestRemoteEndpoint() {
            super(new ProtocolHandler(false, null), null);
        }

        @Override
        public Future<Frame> sendText(String fragment, boolean isLast) {
            builder.append(fragment);
            return new Future<Frame>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Frame get() throws InterruptedException, ExecutionException {
                    return null;
                }

                @Override
                public Frame get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                        TimeoutException {
                    return null;
                }
            };
        }

        @Override
        public Future<Frame> sendBinary(byte[] data, int off, int len, boolean isLast) {
            lastSentMessageSize = len;
            for (int i = off; i < len; i++) {
                bytesToSend.add(data[i]);
            }
            return new Future<Frame>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Frame get() throws InterruptedException, ExecutionException {
                    return null;
                }

                @Override
                public Frame get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                        TimeoutException {
                    return null;
                }
            };
        }

        public byte[] getBytesAndClearBuffer() {
            byte[] result = new byte[bytesToSend.size()];

            for (int i = 0; i < bytesToSend.size(); i++) {
                result[i] = bytesToSend.get(i);
            }

            bytesToSend.clear();
            return result;
        }

        public String getStringAndCleanBuilder() {
            String result = builder.toString();
            builder = new StringBuilder();
            return result;
        }

        private int getLastSentMessageSize() {
            return lastSentMessageSize;
        }
    }

    @ServerEndpoint(value = "/echo")
    private static class EchoEndpoint extends Endpoint {

        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    final String s = EchoEndpoint.this.doThat(message);
                    if (s != null) {
                        try {
                            session.getBasicRemote().sendText(s);
                        } catch (IOException e) {
                            System.out.println("# error");
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @OnMessage
        public String doThat(String message) {
            return message;
        }
    }

    private static class TestContainer extends BaseContainer {

        @Override
        public long getDefaultAsyncSendTimeout() {
            return 0;
        }

        @Override
        public void setAsyncSendTimeout(long l) {

        }

        @Override
        public Session connectToServer(Object o, URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public Session connectToServer(Class<?> aClass, URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public Session connectToServer(Endpoint endpoint, ClientEndpointConfig clientEndpointConfig, URI uri) throws
                DeploymentException, IOException {
            return null;
        }

        @Override
        public Session connectToServer(Class<? extends Endpoint> aClass, ClientEndpointConfig clientEndpointConfig,
                                       URI uri) throws DeploymentException, IOException {
            return null;
        }

        @Override
        public long getDefaultMaxSessionIdleTimeout() {
            return 0;
        }

        @Override
        public void setDefaultMaxSessionIdleTimeout(long l) {

        }

        @Override
        public int getDefaultMaxBinaryMessageBufferSize() {
            return 0;
        }

        @Override
        public void setDefaultMaxBinaryMessageBufferSize(int i) {

        }

        @Override
        public int getDefaultMaxTextMessageBufferSize() {
            return 0;
        }

        @Override
        public void setDefaultMaxTextMessageBufferSize(int i) {

        }

        @Override
        public Set<Extension> getInstalledExtensions() {
            return Collections.emptySet();
        }

        @Override
        public ScheduledExecutorService getScheduledExecutorService() {
            return null;
        }
    }
}
