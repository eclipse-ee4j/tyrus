/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Writer;
import org.glassfish.tyrus.spi.WriterInfo;
import org.junit.Assert;
import org.junit.Test;

import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

public class ProtocolHandlerTest {

    public static class ProtocolHandlerOnCloseEndpoint extends Endpoint {
        CountDownLatch onCloseLatch = new CountDownLatch(1);

        public void onClose(Session session, CloseReason closeReason) {
            onCloseLatch.countDown();
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {

        }
    }

    @Test
    public void testOnCloseIsCalledWhenCloseThrowsError() throws DeploymentException {
        ProtocolHandler handler = new ProtocolHandler(false, null);
        handler.setWriter(new Writer() {
            @Override
            public void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler) {
                throw new IllegalStateException("Not Expected");
            }

            @Override
            public void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler, WriterInfo writerInfo) {
                if (writerInfo.getMessageType() == WriterInfo.MessageType.CLOSE) {
                    throw new UncheckedIOException(new SocketException("Connection reset"));
                }
            }

            @Override
            public void close() throws IOException {
            }
        });

        ProtocolHandlerOnCloseEndpoint endpoint = new ProtocolHandlerOnCloseEndpoint();
        TyrusEndpointWrapper endpointWrapper = new TyrusEndpointWrapper(
                endpoint, null, ComponentProviderService.create(), null, "path",
                null, new TyrusEndpointWrapper.SessionListener() {}, null, null, null);

        TyrusWebSocket tyrusWebSocket = new TyrusWebSocket(handler, endpointWrapper);
        handler.setWebSocket(tyrusWebSocket);
        endpointWrapper.createSessionForRemoteEndpoint(tyrusWebSocket, null, Collections.emptyList(), new DebugContext());
        handler.close(1000, "TEST");
        Assert.assertEquals(0, endpoint.onCloseLatch.getCount());
    }
}
