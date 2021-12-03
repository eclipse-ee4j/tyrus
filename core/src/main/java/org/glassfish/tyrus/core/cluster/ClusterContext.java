/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.cluster;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import jakarta.websocket.CloseReason;
import jakarta.websocket.SendHandler;

/**
 * Cluster related context.
 * <p>
 * There is exactly one instance per cluster node and all communication is realized using this instance.
 *
 * @author Pavel Bucek
 */
public abstract class ClusterContext {

    /**
     * ClusterContext registration property.
     * <p>
     * ClusterContext is registered to the Server container via properties passed to
     * {@link org.glassfish.tyrus.spi.ServerContainerFactory#createServerContainer(java.util.Map)}.
     */
    public static final String CLUSTER_CONTEXT = "org.glassfish.tyrus.core.cluster.ClusterContext";

    /**
     * Send text message.
     *
     * @param sessionId remote session id.
     * @param text      text to be sent.
     * @return future representing the send event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the message has been successfully sent. If there is any exception, it will
     * be wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> sendText(String sessionId, String text);

    /**
     * Send partial text message.
     *
     * @param sessionId remote session id.
     * @param text      text to be sent.
     * @param isLast    {@code true} when the partial message being sent is the last part of the message.
     * @return future representing the send event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the message has been successfully sent. If there is any exception, it will
     * be wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> sendText(String sessionId, String text, boolean isLast);

    /**
     * Send binary message.
     *
     * @param sessionId remote session id.
     * @param data      data to be sent.
     * @return future representing the send event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the message has been successfully sent. If there is any exception, it will
     * be wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> sendBinary(String sessionId, byte[] data);

    /**
     * Send partial binary message.
     *
     * @param sessionId remote session id.
     * @param data      data to be sent.
     * @param isLast    {@code true} when the partial message being sent is the last part of the message.
     * @return future representing the send event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the message has been successfully sent. If there is any exception, it will
     * be wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> sendBinary(String sessionId, byte[] data, boolean isLast);

    /**
     * Send ping message.
     *
     * @param sessionId remote session id.
     * @param data      data to be sent as ping message payload.
     * @return future representing the send event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the message has been successfully sent. If there is any exception, it will
     * be wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> sendPing(String sessionId, byte[] data);

    /**
     * Send pong message.
     *
     * @param sessionId remote session id.
     * @param data      data to be sent as pong message payload.
     * @return future representing the send event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the message has been successfully sent. If there is any exception, it will
     * be wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> sendPong(String sessionId, byte[] data);

    /**
     * Send text message with {@link jakarta.websocket.SendHandler}.
     *
     * @param sessionId   remote session id.
     * @param text        text to be sent.
     * @param sendHandler sendhandler instance on which
     *                    {@link jakarta.websocket.SendHandler#onResult(jakarta.websocket.SendResult)} will be invoked.
     * @see jakarta.websocket.SendHandler
     */
    public abstract void sendText(String sessionId, String text, SendHandler sendHandler);

    /**
     * Send binary message with {@link jakarta.websocket.SendHandler}.
     *
     * @param sessionId   remote session id.
     * @param data        data to be sent.
     * @param sendHandler sendhandler instance on which
     *                    {@link jakarta.websocket.SendHandler#onResult(jakarta.websocket.SendResult)} will be invoked.
     * @see jakarta.websocket.SendHandler
     */
    public abstract void sendBinary(String sessionId, byte[] data, SendHandler sendHandler);

    /**
     * Broadcast text message.
     *
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     * @param text         message to be broadcasted.
     */
    public abstract void broadcastText(String endpointPath, String text);

    /**
     * Broadcast binary message.
     *
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     * @param data         data to be broadcasted.
     */
    public abstract void broadcastBinary(String endpointPath, byte[] data);

    /**
     * Get information about session state.
     *
     * @param sessionId    remote session id.
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     * @return {@code true} when session is opened, {@code false} otherwise.
     * @see jakarta.websocket.Session#isOpen()
     */
    public abstract boolean isSessionOpen(String sessionId, String endpointPath);

    /**
     * Close remote session.
     *
     * @param sessionId remote session id.
     * @return future representing the event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the command was successfully executed. If there is any exception, it will
     * be
     * wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> close(String sessionId);

    /**
     * Close remote session with custom {@link jakarta.websocket.CloseReason}.
     *
     * @param sessionId   remote session id.
     * @param closeReason custom close reason.
     * @return future representing the event. {@link java.util.concurrent.Future#get()} returns when there is an
     * acknowledge from the other node that the command was successfully executed. If there is any exception, it will
     * be
     * wrapped into {@link java.util.concurrent.ExecutionException} and thrown.
     */
    public abstract Future<Void> close(String sessionId, CloseReason closeReason);

    /**
     * Get set containing session ids of all remote sessions registered to given endpoint path.
     *
     * @param endpointPath endpoint path identifying endpoint within the cluster.
     * @return set of sessions ids.
     */
    public abstract Set<String> getRemoteSessionIds(String endpointPath);

    /**
     * Create session id. It has to be unique among all cluster nodes.
     *
     * @return session id.
     */
    public abstract String createSessionId();

    /**
     * Create connection id. It has to be unique among all cluster nodes.
     *
     * @return connection id.
     */
    public abstract String createConnectionId();

    /**
     * Register local session.
     * <p>
     * Session id will be broadcasted to other nodes which will call {@link #getDistributedSessionProperties(String)}
     * and process its values. The map must be ready before this method is invoked.
     *
     * @param sessionId    session id to be registered.
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     * @param listener     session event listener. When remote node sends a message to this session, it will be
     *                     invoked.
     * @see org.glassfish.tyrus.core.cluster.SessionEventListener
     */
    public abstract void registerSession(String sessionId, String endpointPath, SessionEventListener listener);

    /**
     * Register session listener.
     * <p>
     * Gets notification about session creation {@link org.glassfish.tyrus.core.cluster
     * .SessionListener#onSessionOpened(String)} and destruction {@link org.glassfish.tyrus.core.cluster
     * .SessionListener#onSessionClosed(String)}.
     *
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     * @param listener     listener instance.
     * @see org.glassfish.tyrus.core.cluster.SessionListener
     */
    public abstract void registerSessionListener(String endpointPath, SessionListener listener);

    /**
     * Register broadcast listener.
     * <p>
     * Gets notification about broadcasted messages. Used as an optimized variant of standard websocket broadcast
     * pattern. In this case, only one message is sent to all cluster nodes (instead {@code n} when {@code n} represent
     * number of clients connected to remote nodes).
     *
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     * @param listener     listener instance.
     * @see org.glassfish.tyrus.core.cluster.BroadcastListener
     */
    public abstract void registerBroadcastListener(String endpointPath, BroadcastListener listener);

    /**
     * Get the map containing session properties to be shared among nodes.
     * <p>
     * Changes must be propagated to remote instances.
     *
     * @param sessionId remote session id.
     * @return distributed map containing session properties.
     */
    public abstract Map<RemoteSession.DistributedMapKey, Object> getDistributedSessionProperties(String sessionId);

    /**
     * Get the map containing session user properties to be shared among nodes.
     * <p>
     * Changes must be propagated to remote instances.
     *
     * @param connectionId connection id. Connection id may be shared among subsequent TCP connection - represents
     *                     logical connection.
     * @return distributed map containing session properties.
     */
    public abstract Map<String, Object> getDistributedUserProperties(String connectionId);

    /**
     * Destroy map which holds distributed user properties.
     * <p>
     * This method should be invoked only when session is properly closed.
     *
     * @param connectionId connection id. Connection id may be shared among subsequent TCP connection - represents
     *                     logical connection.
     */
    public abstract void destroyDistributedUserProperties(String connectionId);

    /**
     * Remove session from this Cluster context.
     *
     * @param sessionId    session id.
     * @param endpointPath endpoint path identifying sessions alignment to the endpoint.
     */
    public abstract void removeSession(String sessionId, String endpointPath);

    /**
     * Shutdown this ClusterContext.
     * <p>
     * This will stop whole clustered node, any operation related to this cluster context will fail after this method
     * is invoked.
     */
    public abstract void shutdown();
}
