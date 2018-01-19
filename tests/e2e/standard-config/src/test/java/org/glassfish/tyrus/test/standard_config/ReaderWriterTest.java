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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.JAXBBean;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ReaderWriterTest extends TestContainer {

    @ServerEndpoint("/readerWriter-reader")
    public static class ReaderEndpoint {
        @OnMessage
        public String onMessage(Reader reader) throws IOException, JAXBException {
            JAXBBean jaxbBean =
                    (JAXBBean) JAXBContext.newInstance(JAXBBean.class).createUnmarshaller().unmarshal(reader);
            reader.close();

            if (jaxbBean.string1.equals("test") && jaxbBean.string2.equals("bean")) {
                return "ok";
            }

            return null;
        }
    }

    @Test
    public void testClientWriterServerReader() throws DeploymentException {

        final CountDownLatch messageLatch = new CountDownLatch(1);
        Server server = startServer(ReaderEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                if (message.equals("ok")) {
                                    messageLatch.countDown();
                                }
                            }
                        });

                        Writer sendWriter = session.getBasicRemote().getSendWriter();
                        JAXBContext.newInstance(JAXBBean.class).createMarshaller()
                                   .marshal(new JAXBBean("test", "bean"), sendWriter);
                        sendWriter.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JAXBException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(ReaderEndpoint.class));

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/readerWriter-writer")
    public static class WriterEndpoint {

        @OnOpen
        public void onOpen(Session session) throws JAXBException, IOException {
            Writer writer = session.getBasicRemote().getSendWriter();
            JAXBContext.newInstance(JAXBBean.class).createMarshaller().marshal(new JAXBBean("test", "bean"), writer);
            writer.close();
        }
    }

    @Test
    public void testClientReaderServerWriter() throws DeploymentException {

        final CountDownLatch messageLatch = new CountDownLatch(1);
        Server server = startServer(WriterEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<Reader>() {
                        @Override
                        public void onMessage(Reader reader) {
                            try {
                                JAXBBean jaxbBean =
                                        (JAXBBean) JAXBContext.newInstance(JAXBBean.class).createUnmarshaller()
                                                              .unmarshal(reader);
                                if (jaxbBean.string1.equals("test") && jaxbBean.string2.equals("bean")) {
                                    messageLatch.countDown();
                                }
                                reader.close();
                            } catch (JAXBException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }
            }, cec, getURI(WriterEndpoint.class));

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
