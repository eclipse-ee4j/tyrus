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

package org.glassfish.tyrus.ext.monitoring.jmx;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Tests that errors are counted correctly.
 *
 * @author Petr Janouch
 */
public class ErrorStatisticsTest extends TestContainer {

    @Test
    public void monitoringOnEndpointLevelTest() {
        test(false);
    }

    @Test
    public void monitoringOnSessionLevelTest() {
        test(true);
    }

    private void test(boolean monitorOnSessionLevel) {
        Server server = null;
        try {
            setContextPath("/errorTestApp");

            ApplicationMonitor applicationMonitor;
            if (monitorOnSessionLevel) {
                applicationMonitor = new SessionAwareApplicationMonitor();
            } else {
                applicationMonitor = new SessionlessApplicationMonitor();
            }

            CountDownLatch errorCountDownLatch = new CountDownLatch(8);

            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(applicationMonitor, null, null, null, null, errorCountDownLatch);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(ApplicationConfig.class);

            ClientManager client = createClient();

            Session session = client.connectToServer(
                    AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            session.getBasicRemote().sendText("Hello");
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("Hello".getBytes()));
            session.close();

            Session session2 = client.connectToServer(AnnotatedClientEndpoint.class, getURI("/programmaticEndpoint"));
            session2.getBasicRemote().sendText("Hello");
            session2.getBasicRemote().sendBinary(ByteBuffer.wrap("Hello".getBytes()));
            session2.close();

            assertTrue(errorCountDownLatch.await(1, TimeUnit.SECONDS));

            String applicationMXBeanName = "org.glassfish.tyrus:type=/errorTestApp";
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ApplicationMXBean applicationMXBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(applicationMXBeanName), ApplicationMXBean.class);

            Map<String, Long> errorMap = new HashMap<String, Long>();
            for (ErrorCount error : applicationMXBean.getErrorCounts()) {
                errorMap.put(error.getThrowableClassName(), error.getCount());
            }

            long onOpenCount = errorMap.get(OnOpenException.class.getName());
            assertEquals(2, onOpenCount);

            long onTextCount = errorMap.get(OnTextException.class.getName());
            assertEquals(2, onTextCount);

            long onBinaryCount = errorMap.get(OnBinaryException.class.getName());
            assertEquals(2, onBinaryCount);

            long onCloseCount = errorMap.get(OnCloseException.class.getName());
            assertEquals(2, onCloseCount);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    public static class ApplicationConfig implements ServerApplicationConfig {

        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
            return new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder.create(ProgrammaticServerEndpoint.class,
                                                            "/programmaticEndpoint").build());
                }
            };
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
            return new HashSet<Class<?>>(Collections.singleton(AnnotatedServerEndpoint.class));
        }

        public static class ProgrammaticServerEndpoint extends Endpoint {

            @Override
            public void onOpen(final Session session, final EndpointConfig EndpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        throw new OnTextException();
                    }
                });
                session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                    @Override
                    public void onMessage(ByteBuffer message) {
                        throw new OnBinaryException();
                    }
                });
                throw new OnOpenException();
            }

            @Override
            public void onClose(Session session, CloseReason closeReason) {
                throw new OnCloseException();
            }

            @Override
            public void onError(Session session, Throwable thr) {
                // do nothing - keep so that exceptions will not be logged.
            }
        }
    }

    public static class OnTextException extends RuntimeException {
    }

    public static class OnBinaryException extends RuntimeException {
    }

    public static class OnOpenException extends RuntimeException {
    }

    public static class OnCloseException extends RuntimeException {
    }

    @ServerEndpoint("/annotatedEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            throw new OnOpenException();
        }

        @OnMessage
        public void onTextMessage(String message, Session session) {
            throw new OnTextException();
        }

        @OnMessage
        public void onBinaryMessage(ByteBuffer message, Session session) {
            throw new OnBinaryException();
        }

        @OnClose
        public void onClose() {
            throw new OnCloseException();
        }

        @OnError
        public void onError(Throwable t) {
            // do nothing - keep so that exceptions will not be logged.
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {
    }
}
