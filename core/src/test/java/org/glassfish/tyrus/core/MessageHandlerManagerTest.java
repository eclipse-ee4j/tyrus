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

import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;

import org.glassfish.tyrus.core.coder.CoderAdapter;
import org.glassfish.tyrus.core.coder.CoderWrapper;
import org.glassfish.tyrus.core.l10n.LocalizationMessages;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class MessageHandlerManagerTest {
    @Test
    public void simpleTest() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTextHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<Reader>() {
            @Override
            public void onMessage(Reader message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTextHandlersCombined() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<Reader>() {

            @Override
            public void onMessage(Reader reader) {

            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTextHandlersWithDecoder() {
        MessageHandlerManager messageHandlerManager = MessageHandlerManager.fromDecoderInstances(Arrays.<Decoder>asList(
                new CoderWrapper<Decoder>(new TestTextDecoder(), MessageHandlerManagerTest.class)));

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<MessageHandlerManagerTest>() {
            @Override
            public void onMessage(MessageHandlerManagerTest message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<Reader>() {

            @Override
            public void onMessage(Reader reader) {

            }
        });
    }

    @Test
    public void noDecoderTest() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        try {
            messageHandlerManager.addMessageHandler(new MessageHandler.Whole<MessageHandlerManagerTest>() {
                @Override
                public void onMessage(MessageHandlerManagerTest message) {
                }
            });

            fail("IllegalStateException was expected.");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().equals(LocalizationMessages.MESSAGE_HANDLER_DECODER_NOT_REGISTERED(
                    MessageHandlerManagerTest.class)));
        }
    }

    public static class TestTextDecoder extends CoderAdapter implements Decoder.Text<MessageHandlerManagerTest> {

        @Override
        public MessageHandlerManagerTest decode(String s) throws DecodeException {
            return null;
        }

        @Override
        public boolean willDecode(String s) {
            return false;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void multipleStringHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlersCombined() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlersWithByteArray() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<byte[]>() {
            @Override
            public void onMessage(byte[] message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleInputStreamHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<InputStream>() {
            @Override
            public void onMessage(InputStream message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multiplePongHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBasicDecodable() {
        MessageHandlerManager messageHandlerManager =
                new MessageHandlerManager(Collections.<Class<? extends Decoder>>singletonList(TestTextDecoder.class));

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<MessageHandlerManagerTest>() {
            @Override
            public void onMessage(MessageHandlerManagerTest message) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Whole<MessageHandlerManagerTest>() {
            @Override
            public void onMessage(MessageHandlerManagerTest message) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTextHandlersPartial() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<String>() {
            @Override
            public void onMessage(String message, boolean last) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<String>() {
            @Override
            public void onMessage(String message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void wrongPartialHandlerType() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<MessageHandlerManagerTest>() {
            @Override
            public void onMessage(MessageHandlerManagerTest message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleBinaryHandlersWithByteArrayPartial() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<byte[]>() {
            @Override
            public void onMessage(byte[] message, boolean last) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void multipleByteBufferHandlersPartial() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();


        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        });
    }


    @Test(expected = IllegalStateException.class)
    public void multipleBasicDecodablePartial() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();


        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<MessageHandlerManagerTest>() {
            @Override
            public void onMessage(MessageHandlerManagerTest message, boolean last) {
            }
        });

        messageHandlerManager.addMessageHandler(new MessageHandler.Partial<MessageHandlerManagerTest>() {
            @Override
            public void onMessage(MessageHandlerManagerTest message, boolean last) {
            }
        });
    }

    @Test
    public void getHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();


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

        messageHandlerManager.addMessageHandler(handler1);
        messageHandlerManager.addMessageHandler(handler2);
        messageHandlerManager.addMessageHandler(handler3);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));
    }

    @Test
    public void addRemoveAddHandlers() {
        MessageHandlerManager messageHandlerManager =
                new MessageHandlerManager(Collections.<Class<? extends Decoder>>singletonList(TestTextDecoder.class));


        final MessageHandler.Whole<String> handler1 = new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        };

        final MessageHandler.Partial<ByteBuffer> handler2 = new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        };

        final MessageHandler.Whole<PongMessage> handler3 = new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        };

        final MessageHandler.Whole<MessageHandlerManagerTest> handler4 =
                new MessageHandler.Whole<MessageHandlerManagerTest>() {
                    @Override
                    public void onMessage(MessageHandlerManagerTest message) {
                    }
                };

        messageHandlerManager.addMessageHandler(handler1);
        messageHandlerManager.addMessageHandler(handler2);
        messageHandlerManager.addMessageHandler(handler3);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.removeMessageHandler(handler3);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.removeMessageHandler(handler2);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.addMessageHandler(handler3);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.addMessageHandler(handler2);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.removeMessageHandler(handler1);

        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.addMessageHandler(handler4);

        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler4));
    }


    @Test
    public void removeHandlers() {
        MessageHandlerManager messageHandlerManager = new MessageHandlerManager();


        final MessageHandler.Whole<String> handler1 = new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
            }
        };
        final MessageHandler.Partial<ByteBuffer> handler2 = new MessageHandler.Partial<ByteBuffer>() {
            @Override
            public void onMessage(ByteBuffer message, boolean last) {
            }
        };
        final MessageHandler.Whole<PongMessage> handler3 = new MessageHandler.Whole<PongMessage>() {
            @Override
            public void onMessage(PongMessage message) {
            }
        };


        messageHandlerManager.addMessageHandler(handler1);
        messageHandlerManager.addMessageHandler(handler2);
        messageHandlerManager.addMessageHandler(handler3);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.removeMessageHandler(handler3);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler3));

        messageHandlerManager.removeMessageHandler(handler2);

        assertTrue(messageHandlerManager.getMessageHandlers().contains(handler1));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler2));
        assertFalse(messageHandlerManager.getMessageHandlers().contains(handler3));
    }
}
