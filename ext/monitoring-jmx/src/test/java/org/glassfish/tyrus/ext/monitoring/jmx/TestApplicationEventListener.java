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

import java.util.concurrent.CountDownLatch;

import org.glassfish.tyrus.core.frame.TyrusFrame;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.core.monitoring.EndpointEventListener;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;

/**
 * {@link org.glassfish.tyrus.core.monitoring.ApplicationEventListener} wrapper that accepts five latches that are
 * decremented when message is sent, received, session opened, session closed or an error occurred.
 *
 * @author Petr Janouch
 */
class TestApplicationEventListener implements ApplicationEventListener {

    private final ApplicationEventListener applicationEventListener;
    private final CountDownLatch sessionOpenedLatch;
    private final CountDownLatch sessionClosedLatch;
    private final CountDownLatch messageSentLatch;
    private final CountDownLatch messageReceivedLatch;
    private final CountDownLatch errorLatch;

    /**
     * Constructor.
     *
     * @param applicationEventListener wrapped application event listener.
     * @param sessionOpenedLatch       latch that is decreased when a session is opened.
     * @param sessionClosedLatch       latch that is decreased when a session is closed.
     * @param messageSentLatch         latch that is decreased when a message is sent.
     * @param messageReceivedLatch     latch that is decreased when a message is received.
     * @param errorLatch               latch that is decreased when an error has occurred.
     */
    TestApplicationEventListener(ApplicationEventListener applicationEventListener, CountDownLatch sessionOpenedLatch,
                                 CountDownLatch sessionClosedLatch, CountDownLatch messageSentLatch,
                                 CountDownLatch messageReceivedLatch, CountDownLatch errorLatch) {
        this.applicationEventListener = applicationEventListener;
        this.sessionOpenedLatch = sessionOpenedLatch;
        this.sessionClosedLatch = sessionClosedLatch;
        this.messageSentLatch = messageSentLatch;
        this.messageReceivedLatch = messageReceivedLatch;
        this.errorLatch = errorLatch;
    }

    @Override
    public void onApplicationInitialized(String applicationName) {
        applicationEventListener.onApplicationInitialized(applicationName);
    }

    @Override
    public void onApplicationDestroyed() {
        applicationEventListener.onApplicationDestroyed();
    }

    @Override
    public EndpointEventListener onEndpointRegistered(String endpointPath, Class<?> endpointClass) {
        return new TestEndpointEventListener(applicationEventListener.onEndpointRegistered(endpointPath, endpointClass),
                                             sessionOpenedLatch, sessionClosedLatch, messageSentLatch,
                                             messageReceivedLatch, errorLatch);
    }

    @Override
    public void onEndpointUnregistered(String endpointPath) {
        applicationEventListener.onEndpointUnregistered(endpointPath);
    }

    private class TestEndpointEventListener implements EndpointEventListener {

        private final EndpointEventListener endpointEventListener;
        private final CountDownLatch sessionOpenedLatch;
        private final CountDownLatch sessionClosedLatch;
        private final CountDownLatch messageSentLatch;
        private final CountDownLatch messageReceivedLatch;
        private final CountDownLatch errorLatch;

        TestEndpointEventListener(EndpointEventListener endpointEventListener, CountDownLatch sessionOpenedLatch,
                                  CountDownLatch sessionClosedLatch, CountDownLatch messageSentLatch,
                                  CountDownLatch messageReceivedLatch, CountDownLatch errorLatch) {
            this.endpointEventListener = endpointEventListener;
            this.sessionOpenedLatch = sessionOpenedLatch;
            this.sessionClosedLatch = sessionClosedLatch;
            this.messageSentLatch = messageSentLatch;
            this.messageReceivedLatch = messageReceivedLatch;
            this.errorLatch = errorLatch;
        }

        @Override
        public MessageEventListener onSessionOpened(String sessionId) {
            MessageEventListener messageEventListener =
                    new TestMessageEventListener(endpointEventListener.onSessionOpened(sessionId), messageSentLatch,
                                                 messageReceivedLatch);
            if (sessionOpenedLatch != null) {
                sessionOpenedLatch.countDown();
            }
            return messageEventListener;
        }

        @Override
        public void onSessionClosed(String sessionId) {
            endpointEventListener.onSessionClosed(sessionId);
            if (sessionClosedLatch != null) {
                sessionClosedLatch.countDown();
            }
        }

        @Override
        public void onError(String sessionId, Throwable t) {
            endpointEventListener.onError(sessionId, t);
            if (errorLatch != null) {
                errorLatch.countDown();
            }
        }
    }

    private class TestMessageEventListener implements MessageEventListener {

        private final MessageEventListener messageEventListener;
        private final CountDownLatch messageSentLatch;
        private final CountDownLatch messageReceivedLatch;

        TestMessageEventListener(MessageEventListener messageEventListener, CountDownLatch messageSentLatch,
                                 CountDownLatch messageReceivedLatch) {
            this.messageEventListener = messageEventListener;
            this.messageSentLatch = messageSentLatch;
            this.messageReceivedLatch = messageReceivedLatch;
        }

        @Override
        public void onFrameSent(TyrusFrame.FrameType frameType, long payloadLength) {
            messageEventListener.onFrameSent(frameType, payloadLength);
            if (messageSentLatch != null) {
                messageSentLatch.countDown();
            }
        }

        @Override
        public void onFrameReceived(TyrusFrame.FrameType frameType, long payloadLength) {
            messageEventListener.onFrameReceived(frameType, payloadLength);
            if (messageReceivedLatch != null) {
                messageReceivedLatch.countDown();
            }
        }
    }
}
