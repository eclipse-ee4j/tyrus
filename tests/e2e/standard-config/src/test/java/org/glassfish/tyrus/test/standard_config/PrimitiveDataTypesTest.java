/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the primitive data types
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class PrimitiveDataTypesTest extends TestContainer {

    private CountDownLatch messageLatch;

    private String receivedMessage;

    private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();


    @Test
    public void testBoolean() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.BooleanBean.class, "/standardInputTypes/boolean",
                 "true", "PASS");
    }


    @Test
    public void testChar() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.CharBean.class, "/standardInputTypes/char", "c",
                 "PASS");
    }


    @Test
    public void testDouble() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.DoubleBean.class, "/standardInputTypes/double",
                 "42.0", "PASS");
    }


    @Test
    public void testFloat() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.FloatBean.class, "/standardInputTypes/float",
                 "42.0", "PASS");
    }


    @Test
    public void testInt() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.IntBean.class, "/standardInputTypes/int", "42",
                 "PASS");
    }

    @Test
    public void testLong() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.LongBean.class, "/standardInputTypes/long", "42",
                 "PASS");
    }

    @Test
    public void testShort() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.ShortBean.class, "/standardInputTypes/short", "42",
                 "PASS");
    }

    @Test
    public void testString() throws DeploymentException {
        testPojo(org.glassfish.tyrus.test.standard_config.bean.stin.StringBean.class, "/standardInputTypes/String",
                 "String", "PASS");
    }

    public void testPojo(Class<?> bean, String segmentPath, final String message, String response) throws
            DeploymentException {
        Server server = startServer(bean);

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            TestAdapter ta = new TestAdapter(message);
            client.connectToServer(ta, cec, getURI(segmentPath));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(response, receivedMessage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    private class TestAdapter extends TestEndpointAdapter {
        private final String message;

        private TestAdapter(String message) {
            this.message = message;
        }

        @Override
        public EndpointConfig getEndpointConfig() {
            return null;
        }

        @Override
        public void onOpen(Session session) {
            try {
                session.addMessageHandler(new TestTextMessageHandler(this));
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(String message) {
            receivedMessage = message;
            messageLatch.countDown();
        }
    }
}
