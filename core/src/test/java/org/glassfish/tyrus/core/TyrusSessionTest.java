/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TyrusSessionTest {
    private TyrusEndpointWrapper endpointWrapper;

    public TyrusSessionTest() {
        try {
            endpointWrapper =
                    new TyrusEndpointWrapper(EchoEndpoint.class, null, ComponentProviderService.create(), null, null,
                                             null, null, null, null, null);
        } catch (DeploymentException e) {
            // do nothing.
        }
    }

    @Test
    public void simpleTest() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        });
    }


    @Test(expected = IllegalStateException.class)
    public void multipleTextHandlers() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<Reader>() {
            @Override
            public void onMessage(Reader message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleStringHandlers() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlers() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlersWithByteArray() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
            @Override
            public void onMessage(byte[] message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleInputStreamHandlers() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multiplePongHandlers() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBasicDecodable() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Whole<TyrusSessionTest>() {
            @Override
            public void onMessage(TyrusSessionTest message) {
            }
        });

        session.addMessageHandler(new MessageHandler.Whole<TyrusSessionTest>() {
            @Override
            public void onMessage(TyrusSessionTest message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTextHandlersAsync() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Partial<String>() {
            @Override
            public void onMessage(String message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<Reader>() {
            @Override
            public void onMessage(Reader message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleStringHandlersAsync() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Partial<String>() {
            @Override
            public void onMessage(String message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<String>() {
            @Override
            public void onMessage(String message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlersAsync() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Partial<InputStream>() {
            @Override
            public void onMessage(InputStream message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlersWithByteArrayAsync() {
        Session session = createSession(endpointWrapper);

        session.addMessageHandler(new MessageHandler.Partial<byte[]>() {
            @Override
            public void onMessage(byte[] message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleInputStreamHandlersAsync() {
        Session session = createSession(endpointWrapper);


        session.addMessageHandler(new MessageHandler.Partial<InputStream>() {
            @Override
            public void onMessage(InputStream message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<InputStream>() {
            @Override
            public void onMessage(InputStream message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multiplePongHandlersAsync() {
        Session session = createSession(endpointWrapper);


        session.addMessageHandler(new MessageHandler.Partial<PongMessage>() {
            @Override
            public void onMessage(PongMessage message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<PongMessage>() {
            @Override
            public void onMessage(PongMessage message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBasicDecodableAsync() {
        Session session = createSession(endpointWrapper);


        session.addMessageHandler(new MessageHandler.Partial<TyrusSessionTest>() {
            @Override
            public void onMessage(TyrusSessionTest message, boolean last) {
            }
        });

        session.addMessageHandler(new MessageHandler.Partial<TyrusSessionTest>() {
            @Override
            public void onMessage(TyrusSessionTest message, boolean last) {
            }
        });
    }

    @Test
    public void getHandlers() {
        Session session = createSession(endpointWrapper);

        final MessageHandler.Whole<String> handler1 = new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        };
        final MessageHandler.Whole<ByteBuffer> handler2 = new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        };
        final MessageHandler.Whole<PongMessage> handler3 = new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        };

        session.addMessageHandler(handler1);
        session.addMessageHandler(handler2);
        session.addMessageHandler(handler3);

        assertTrue(session.getMessageHandlers().contains(handler1));
        assertTrue(session.getMessageHandlers().contains(handler2));
        assertTrue(session.getMessageHandlers().contains(handler3));
    }

    @Test
    public void removeHandlers() {
        Session session = createSession(endpointWrapper);


        final MessageHandler.Partial<String> handler1 = new MessageHandler.Partial<String>() {
            @Override
            public void onMessage(String message, boolean last) {
            }
        };
        final MessageHandler.Whole<ByteBuffer> handler2 = new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        };
        final MessageHandler.Whole<PongMessage> handler3 = new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        };

        session.addMessageHandler(handler1);
        session.addMessageHandler(handler2);
        session.addMessageHandler(handler3);

        assertTrue(session.getMessageHandlers().contains(handler1));
        assertTrue(session.getMessageHandlers().contains(handler2));
        assertTrue(session.getMessageHandlers().contains(handler3));

        session.removeMessageHandler(handler3);

        assertTrue(session.getMessageHandlers().contains(handler1));
        assertTrue(session.getMessageHandlers().contains(handler2));
        assertFalse(session.getMessageHandlers().contains(handler3));

        session.removeMessageHandler(handler2);

        assertTrue(session.getMessageHandlers().contains(handler1));
        assertFalse(session.getMessageHandlers().contains(handler2));
        assertFalse(session.getMessageHandlers().contains(handler3));
    }

    @Test
    public void idTest() {
        Session session1 = createSession(endpointWrapper);
        Session session2 = createSession(endpointWrapper);
        Session session3 = createSession(endpointWrapper);

        assertFalse(session1.getId().equals(session2.getId()));
        assertFalse(session1.getId().equals(session3.getId()));
        assertFalse(session2.getId().equals(session3.getId()));
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

    @Test
    public void userPropertiesTest() {
        Session session1 = createSession(endpointWrapper);
        Session session2 = createSession(endpointWrapper);

        final String test1 = "test1";
        final String test2 = "test2";

        session1.getUserProperties().put(test1, test1);
        session2.getUserProperties().put(test2, test2);

        assertNull(session1.getUserProperties().get(test2));
        assertNull(session2.getUserProperties().get(test1));

        assertNotNull(session1.getUserProperties().get(test1));
        assertNotNull(session2.getUserProperties().get(test2));
    }

    private TyrusSession createSession(TyrusEndpointWrapper endpointWrapper) {
        return new TyrusSession(null, new TestRemoteEndpoint(), endpointWrapper, null, null, false, null, null, null,
                                null, new HashMap<String, List<String>>(), null, null, null, new DebugContext());
    }

    private static class TestRemoteEndpoint extends TyrusWebSocket {

        private TestRemoteEndpoint() {
            super(new ProtocolHandler(false, null), null);
        }
    }
}
