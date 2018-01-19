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

package org.glassfish.tyrus.test.e2e.non_deployable;

import java.net.URI;
import java.util.ArrayList;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Cannot be moved to standard tests due the expected deployment exception.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class AnnotatedClassModelcheckingTest extends TestContainer {

    private void testServerPositive(Class<?> testedBean) {
        testServer(testedBean, false);
    }

    private void testServerNegative(Class<?> testedBean) {
        testServer(testedBean, true);
    }

    private void testServer(Class<?> testedBean, boolean shouldThrowException) {
        Server server = null;
        boolean exceptionThrown = false;

        try {
            server = startServer(testedBean);
        } catch (DeploymentException e) {
            exceptionThrown = true;
        } finally {
            stopServer(server);
            Assert.assertEquals(shouldThrowException, exceptionThrown);
        }
    }

    @Test
    public void testEndpointWithTwoSessionMessageParameters() {
        testServerNegative(TwoSessionParametersErrorBean.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class TwoSessionParametersErrorBean {

        @OnMessage
        public void twoSessions(PongMessage message, Session peer1, Session peer2) {

        }
    }

    @Test
    public void testEndpointWithTwoStringMessageParameters() {
        testServerNegative(TwoStringParametersErrorBean.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class TwoStringParametersErrorBean {

        @OnMessage
        public void twoStrings(String message1, String message2, Session peer2) {

        }
    }

    @Test
    public void testEndpointWithWrongMessageReturnParameter() {
        testServerNegative(WrongMessageReturnParameter.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class WrongMessageReturnParameter {

        @OnMessage
        public ArrayList<String> wrongReturn(String message1, Session peer2) {
            return new ArrayList<String>();
        }
    }

    @Test
    public void testEndpointWithCorrectMessageReturnParameter1() {
        testServerPositive(CorrectMessageReturnParameter1.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class CorrectMessageReturnParameter1 {

        @OnMessage
        public Float wrongReturn(String message1, Session peer2) {
            return new Float(5);
        }
    }

    @Test
    public void testEndpointWithCorrectMessageReturnParameter2() {
        testServerPositive(CorrectMessageReturnParameter2.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class CorrectMessageReturnParameter2 {

        @OnMessage
        public float wrongReturn(String message1, Session peer2) {
            return (float) 5.23;
        }
    }

    @Test
    public void testErrorMethodWithoutThrowable() {
        testServerNegative(ErrorMethodWithoutThrowableErrorBean.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class ErrorMethodWithoutThrowableErrorBean {

        @OnError
        public void wrongOnError(Session peer) {

        }
    }

    @Test
    public void testErrorMethodWithWrongParameter() {
        testServerNegative(ErrorMethodWithWrongParam.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class ErrorMethodWithWrongParam {

        @OnError
        public void wrongOnError(Session peer, Throwable t, String s) {

        }
    }

    @Test
    public void testOpenMethodWithWrongParameter() {
        testServerNegative(OpenMethodWithWrongParam.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class OpenMethodWithWrongParam {

        @OnOpen
        public void wrongOnOpen(Session peer, String s) {

        }
    }

    @Test
    public void testCloseMethodWithWrongParameter() {
        testServerNegative(CloseMethodWithWrongParam.class);
    }

    @ServerEndpoint(value = "/hello")
    public static class CloseMethodWithWrongParam {

        @OnClose
        public void wrongOnClose(Session peer, String s) {

        }
    }

    @Test
    public void testMultipleWrongMethods() {
        testServerNegative(MultipleWrongMethodsBean.class);
    }

    @Test
    public void testMultipleWrongMethodsOnClient() {
        boolean exceptionThrown = false;

        try {
            ClientManager client = createClient();
            client.connectToServer(MultipleWrongMethodsBean.class,
                                   new URI("wss://localhost:8025/websockets/tests/hello"));
        } catch (DeploymentException e) {
            //e.printStackTrace();
            exceptionThrown = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Assert.assertEquals(true, exceptionThrown);
        }
    }

    @ClientEndpoint
    @ServerEndpoint(value = "/hello")
    public static class MultipleWrongMethodsBean {

        @OnClose
        public void wrongOnClose(Session peer, String s) {

        }

        @OnOpen
        public void wrongOnOpen(Session peer, String s) {

        }

        @OnError
        public void wrongOnError(Session peer, Throwable t, String s) {

        }

        @OnMessage
        public void twoStrings(String message1, String message2, Session peer2) {

        }
    }
}
