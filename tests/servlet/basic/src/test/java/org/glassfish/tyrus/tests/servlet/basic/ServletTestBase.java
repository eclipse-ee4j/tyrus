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

package org.glassfish.tyrus.tests.servlet.basic;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Primarily meant to test servlet integration, might be someday used for simple stress testing.
 * <p/>
 * Tests are executed from descendant classes, which must implement {@link #getScheme()} method. This is used to enable
 * testing with {@code ws} and {@code wss} schemes.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class ServletTestBase extends TestContainer {

    private static final String CONTEXT_PATH = "/servlet-test";

    public ServletTestBase() {
        setContextPath(CONTEXT_PATH);
    }

    protected abstract String getScheme();

    @Test
    public void testPlainEchoShort() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                                       @Override
                                       public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                           try {
                                               session.addMessageHandler(new MessageHandler.Whole<String>() {
                                                   @Override
                                                   public void onMessage(String message) {
                                                       assertEquals(message, "Do or do not, there is no try.");
                                                       messageLatch.countDown();
                                                   }
                                               });

                                               session.getBasicRemote().sendText("Do or do not, there is no try.");
                                           } catch (IOException e) {
                                               // do nothing
                                           }
                                       }
                                   }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                          getScheme()));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testPlainEchoShort100() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(100);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                                       @Override
                                       public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                           try {
                                               session.addMessageHandler(new MessageHandler.Whole<String>() {
                                                   @Override
                                                   public void onMessage(String message) {
                                                       assertEquals(message, "Do or do not, there is no try.");
                                                       messageLatch.countDown();
                                                   }
                                               });

                                               for (int i = 0; i < 100; i++) {
                                                   session.getBasicRemote().sendText("Do or do not, there is no try.");
                                               }
                                           } catch (IOException e) {
                                               // do nothing
                                           }
                                       }
                                   }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                          getScheme()));

            messageLatch.await(20, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testPlainEchoShort10Sequence() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(10);

        try {
            for (int i = 0; i < 10; i++) {
                final ClientManager client = createClient();
                client.connectToServer(new Endpoint() {
                                           @Override
                                           public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                               try {
                                                   session.addMessageHandler(new MessageHandler.Whole<String>() {
                                                       @Override
                                                       public void onMessage(String message) {
                                                           assertEquals(message, "Do or do not, there is no try.");
                                                           messageLatch.countDown();
                                                       }
                                                   });

                                                   session.getBasicRemote().sendText("Do or do not, there is no try.");
                                               } catch (IOException e) {
                                                   // do nothing
                                               }
                                           }
                                       }, ClientEndpointConfig.Builder.create().build(),
                                       getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                              getScheme()));

                // TODO - remove when possible.
                Thread.sleep(100);
            }

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testPlainEchoShort10SequenceReturnedSession() throws DeploymentException, InterruptedException,
            IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(10);

        try {
            for (int i = 0; i < 10; i++) {
                final ClientManager client = createClient();
                Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertEquals(message, "Do or do not, there is no try.");
                                messageLatch.countDown();
                            }
                        });
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(PlainEchoEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));

                session.getBasicRemote().sendText("Do or do not, there is no try.");
                // TODO - remove when possible.
                Thread.sleep(100);
            }

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    /**
     * 10x10x10 bytes.
     */
    private static final String LONG_MESSAGE =
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
                    + "123456789012345678901234567890";

    @Test
    public void testPlainEchoLong() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                                       @Override
                                       public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                           try {
                                               session.addMessageHandler(new MessageHandler.Whole<String>() {
                                                   @Override
                                                   public void onMessage(String message) {
                                                       assertEquals(message, LONG_MESSAGE);
                                                       messageLatch.countDown();
                                                   }
                                               });

                                               session.getBasicRemote().sendText(LONG_MESSAGE);
                                           } catch (IOException e) {
                                               // do nothing
                                           }
                                       }
                                   }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                          getScheme()));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testPlainEchoLong10() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(10);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                                       @Override
                                       public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                           try {
                                               session.addMessageHandler(new MessageHandler.Whole<String>() {
                                                   @Override
                                                   public void onMessage(String message) {
                                                       assertEquals(message, LONG_MESSAGE);
                                                       messageLatch.countDown();
                                                   }
                                               });

                                               for (int i = 0; i < 10; i++) {
                                                   session.getBasicRemote().sendText(LONG_MESSAGE);
                                               }
                                           } catch (IOException e) {
                                               // do nothing
                                           }
                                       }
                                   }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                          getScheme()));

            messageLatch.await(10, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testPlainEchoLong10Sequence() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(PlainEchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(10);

        try {
            for (int i = 0; i < 10; i++) {
                final ClientManager client = createClient();
                client.connectToServer(new Endpoint() {
                                           @Override
                                           public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                               try {
                                                   session.addMessageHandler(new MessageHandler.Whole<String>() {
                                                       @Override
                                                       public void onMessage(String message) {
                                                           assertEquals(message, LONG_MESSAGE);
                                                           messageLatch.countDown();
                                                       }
                                                   });

                                                   session.getBasicRemote().sendText(LONG_MESSAGE);
                                               } catch (IOException e) {
                                                   // do nothing
                                               }
                                           }
                                       }, ClientEndpointConfig.Builder.create().build(),
                                       getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                              getScheme()));

                // TODO - remove when possible.
                Thread.sleep(300);
            }

            messageLatch.await(10, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testGetRequestURI() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(RequestUriEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();

            URI uri = getURI(RequestUriEndpoint.class.getAnnotation(ServerEndpoint.class).value(), getScheme());
            uri = URI.create(uri.toString() + "?test=1;aaa");

            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertTrue(message.endsWith("?test=1;aaa"));
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("test");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(), uri);

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testOnOpenClose() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(OnOpenCloseEndpoint.class);

        final CountDownLatch latch = new CountDownLatch(2);

        try {
            final ClientManager client = createClient();

            URI uri = getURI(OnOpenCloseEndpoint.class.getAnnotation(ServerEndpoint.class).value(), getScheme());

            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    latch.countDown();
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    latch.countDown();
                }
            }, ClientEndpointConfig.Builder.create().build(), uri);

            latch.await(3, TimeUnit.SECONDS);
            assertEquals(0, latch.getCount());
        } finally {
            stopServer(server);
        }
    }

    // "performance" test; 500 kB message is echoed 10 times.
    @Test
    public void testMultiEcho() throws IOException, DeploymentException, InterruptedException {
        final Server server = startServer(MultiEchoEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(10);

        try {
            final ClientManager client = createClient();
            final Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals(LENGTH, message.length());
                            messageLatch.countDown();
                        }
                    });
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(MultiEchoEndpoint.class
                                                                             .getAnnotation(ServerEndpoint.class)
                                                                             .value(), getScheme()));

            session.getBasicRemote().sendText(text);

            messageLatch.await(10, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    // "performance" test; 20 clients, endpoint broadcasts.
    @Test
    public void testTyrusBroadcastString() throws IOException, DeploymentException, InterruptedException {
        final Server server = startServer(TyrusBroadcastEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(800);
        final List<Session> sessions = new ArrayList<Session>(20);

        try {
            for (int i = 0; i < 20; i++) {
                final ClientManager client = createClient();
                final Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertEquals(LENGTH, message.length());
                                System.out.println("### " + messageLatch.getCount());
                                messageLatch.countDown();
                            }
                        });
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(TyrusBroadcastEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));
                System.out.println("Client " + i + " connected.");
                sessions.add(session);
            }

            final long l = System.currentTimeMillis();
            for (Session s : sessions) {
                s.getBasicRemote().sendText(text);
                s.getBasicRemote().sendText(text);
            }

            messageLatch.await(100, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            System.out.println("***** Tyrus broadcast - text ***** " + (System.currentTimeMillis() - l));
        } finally {
            for (Session session : sessions) {
                session.close();
            }

            stopServer(server);
        }
    }

    // "performance" test; 20 clients, endpoint broadcasts.
    @Test
    public void testTyrusBroadcastBinary() throws IOException, DeploymentException, InterruptedException {
        final Server server = startServer(TyrusBroadcastEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(800);
        final List<Session> sessions = new ArrayList<Session>(20);

        try {
            for (int i = 0; i < 20; i++) {
                final ClientManager client = createClient();
                final Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onError(Session session, Throwable thr) {
                        thr.printStackTrace();
                    }

                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                            @Override
                            public void onMessage(byte[] message) {
                                assertEquals(LENGTH, message.length);
                                System.out.println("### " + messageLatch.getCount());
                                messageLatch.countDown();
                            }
                        });


                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(TyrusBroadcastEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));
                System.out.println("Client " + i + " connected.");
                sessions.add(session);
            }

            final long l = System.currentTimeMillis();
            for (Session s : sessions) {
                s.getBasicRemote().sendBinary(ByteBuffer.wrap(text.getBytes()));
                s.getBasicRemote().sendBinary(ByteBuffer.wrap(text.getBytes()));
            }

            messageLatch.await(100, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            System.out.println("***** Tyrus broadcast - binary ***** " + (System.currentTimeMillis() - l));
        } finally {
            for (Session session : sessions) {
                session.close();
            }

            stopServer(server);
        }
    }

    // "performance" test; 20 clients, endpoint broadcasts.
    @Test
    public void testWebSocketBroadcast() throws IOException, DeploymentException, InterruptedException {
        final Server server = startServer(WebSocketBroadcastEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(800);
        final List<Session> sessions = new ArrayList<Session>(20);

        try {
            for (int i = 0; i < 20; i++) {
                final ClientManager client = createClient();
                final Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertEquals(LENGTH, message.length());
                                messageLatch.countDown();
                                System.out.println("### " + messageLatch.getCount());
                            }
                        });
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(WebSocketBroadcastEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));
                System.out.println("Client " + i + " connected.");
                sessions.add(session);
            }

            final long l = System.currentTimeMillis();
            for (Session s : sessions) {
                s.getBasicRemote().sendText(text);
                s.getBasicRemote().sendText(text);
            }

            messageLatch.await(60, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());

            System.out.println("***** WebSocket broadcast ***** " + (System.currentTimeMillis() - l));

        } finally {
            for (Session session : sessions) {
                session.close();
            }

            stopServer(server);
        }
    }

    // "performance" test; 20 clients, endpoint broadcasts.
    @Test
    public void testTyrusBroadcastStringSharedClientContainer() throws IOException, DeploymentException,
            InterruptedException {
        final Server server = startServer(TyrusBroadcastEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(800);
        final List<Session> sessions = new ArrayList<Session>(20);

        try {
            for (int i = 0; i < 20; i++) {
                final ClientManager client = createClient();
                client.getProperties().put(GrizzlyClientContainer.SHARED_CONTAINER, true);
                final Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertEquals(LENGTH, message.length());
                                System.out.println("### " + messageLatch.getCount());
                                messageLatch.countDown();
                            }
                        });
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(TyrusBroadcastEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));
                System.out.println("Client " + i + " connected.");
                sessions.add(session);
            }

            final long l = System.currentTimeMillis();
            for (Session s : sessions) {
                s.getBasicRemote().sendText(text);
                s.getBasicRemote().sendText(text);
            }

            messageLatch.await(100, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            System.out.println("***** Tyrus broadcast - text ***** " + (System.currentTimeMillis() - l));
        } finally {
            for (Session session : sessions) {
                session.close();
            }

            stopServer(server);
        }
    }

    // "performance" test; 20 clients, endpoint broadcasts.
    @Test
    public void testTyrusBroadcastBinarySharedClientContainer() throws IOException, DeploymentException,
            InterruptedException {
        final Server server = startServer(TyrusBroadcastEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(800);
        final List<Session> sessions = new ArrayList<Session>(20);

        try {
            for (int i = 0; i < 20; i++) {
                final ClientManager client = createClient();
                client.getProperties().put(GrizzlyClientContainer.SHARED_CONTAINER, true);
                final Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                            @Override
                            public void onMessage(byte[] message) {
                                assertEquals(LENGTH, message.length);
                                System.out.println("### " + messageLatch.getCount());
                                messageLatch.countDown();
                            }
                        });
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(TyrusBroadcastEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));
                System.out.println("Client " + i + " connected.");
                sessions.add(session);
            }

            final long l = System.currentTimeMillis();
            for (Session s : sessions) {
                s.getBasicRemote().sendBinary(ByteBuffer.wrap(text.getBytes()));
                s.getBasicRemote().sendBinary(ByteBuffer.wrap(text.getBytes()));
            }

            messageLatch.await(100, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            System.out.println("***** Tyrus broadcast - binary ***** " + (System.currentTimeMillis() - l));
        } finally {
            for (Session session : sessions) {
                session.close();
            }

            stopServer(server);
        }
    }

    // "performance" test; 20 clients, endpoint broadcasts.
    @Test
    public void testWebSocketBroadcastSharedClientContainer() throws IOException, DeploymentException,
            InterruptedException {
        final Server server = startServer(WebSocketBroadcastEndpoint.class);

        final int LENGTH = 587952;
        byte[] b = new byte[LENGTH];
        Arrays.fill(b, 0, LENGTH, (byte) 'a');

        final String text = new String(b);

        final CountDownLatch messageLatch = new CountDownLatch(800);
        final List<Session> sessions = new ArrayList<Session>(20);

        try {
            for (int i = 0; i < 20; i++) {
                final ClientManager client = createClient();
                client.getProperties().put(GrizzlyClientContainer.SHARED_CONTAINER, true);
                final Session session = client.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertEquals(LENGTH, message.length());
                                messageLatch.countDown();
                                System.out.println("### " + messageLatch.getCount());
                            }
                        });
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI(WebSocketBroadcastEndpoint.class
                                                                                 .getAnnotation(ServerEndpoint.class)
                                                                                 .value(), getScheme()));
                System.out.println("Client " + i + " connected.");
                sessions.add(session);
            }

            final long l = System.currentTimeMillis();
            for (Session s : sessions) {
                s.getBasicRemote().sendText(text);
                s.getBasicRemote().sendText(text);
            }

            messageLatch.await(60, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());

            System.out.println("***** WebSocket broadcast ***** " + (System.currentTimeMillis() - l));

        } finally {
            for (Session session : sessions) {
                session.close();
            }

            stopServer(server);
        }
    }
}
