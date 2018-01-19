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

package org.glassfish.tyrus.test.standard_config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class Tyrus109Test extends TestContainer {

    @ServerEndpoint("/open109")
    public static class OnOpenErrorTestEndpoint {
        public static volatile Throwable throwable;
        public static volatile Session session;
        public static CountDownLatch errorLatch = new CountDownLatch(1);

        @OnOpen
        public void open() {
            throw new RuntimeException("testException");
        }

        @OnMessage
        public String message(String message, Session session) {
            // won't be called.
            return message;
        }

        @OnError
        public void handleError(Throwable throwable, Session session) {
            OnOpenErrorTestEndpoint.throwable = throwable;
            OnOpenErrorTestEndpoint.session = session;
            errorLatch.countDown();
            throw new RuntimeException(throwable);
        }
    }

    @ServerEndpoint("/tyrus-109-test-service-endpoint")
    public static class ServiceEndpoint {

        @OnMessage
        public String onMessage(String message) throws InterruptedException {
            if ("throwable".equals(message)) {
                if (OnOpenErrorTestEndpoint.throwable != null) {
                    return POSITIVE;
                }
            }
            if ("session".equals(message)) {
                if (OnOpenErrorTestEndpoint.session != null) {
                    return POSITIVE;
                }
            }
            if ("errorLatch".equals(message)) {
                if (OnOpenErrorTestEndpoint.errorLatch.await(1, TimeUnit.SECONDS)) {
                    return POSITIVE;
                }
            }
            if ("exceptionMessage".equals(message)) {
                if (OnOpenErrorTestEndpoint.throwable.getMessage().equals("testException")) {
                    return POSITIVE;
                }
            }
            return NEGATIVE;
        }
    }

    @Test
    public void testErrorOnOpen() throws DeploymentException {
        Server server = startServer(OnOpenErrorTestEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final CountDownLatch closeLatch = new CountDownLatch(1);
            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig configuration) {
                    // do nothing
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    closeLatch.countDown();
                }
            }, cec, getURI(OnOpenErrorTestEndpoint.class));

            closeLatch.await(1, TimeUnit.SECONDS);

            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "errorLatch");
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "session");
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "throwable");
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "exceptionMessage");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
