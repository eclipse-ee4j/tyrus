/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.sample.chat.chatdata.ChatDecoder;
import org.glassfish.tyrus.sample.chat.chatdata.ChatMessage;
import org.glassfish.tyrus.sample.chat.chatdata.ChatTranscriptUpdateMessage;
import org.glassfish.tyrus.sample.chat.chatdata.ChatUpdateMessage;
import org.glassfish.tyrus.sample.chat.chatdata.DisconnectRequestMessage;
import org.glassfish.tyrus.sample.chat.chatdata.DisconnectResponseEncoder;
import org.glassfish.tyrus.sample.chat.chatdata.DisconnectResponseMessage;
import org.glassfish.tyrus.sample.chat.chatdata.LoginRequestMessage;
import org.glassfish.tyrus.sample.chat.chatdata.LoginResponseMessage;
import org.glassfish.tyrus.sample.chat.chatdata.UserListUpdateMessage;


@ServerEndpoint(
        value = "/chat",
        decoders = {ChatDecoder.class},
        encoders = {DisconnectResponseEncoder.class}
)
public class ChatEndpoint {

    static final Logger logger = Logger.getLogger("application");

    private static ConcurrentHashMap<String, Session> connections = new ConcurrentHashMap<String, Session>();

    private List<String> chatTranscript = new ArrayList<String>();
    static int transcriptMaxLines = 20;

    @OnOpen
    public void init(Session s) {
        logger.info("############Someone connected...");
    }

    @OnMessage
    public void handleMessage(ChatMessage message, Session session) {
        final String messageType = message.getType();

        if (messageType.equals(ChatMessage.LOGIN_REQUEST)) {
            handleLoginRequest((LoginRequestMessage) message, session);
        } else if (messageType.equals(ChatMessage.CHAT_MESSAGE)) {
            handleChatMessage((ChatUpdateMessage) message);
        } else if (messageType.equals(ChatMessage.DISCONNECT_REQUEST)) {
            handleDisconnectRequest((DisconnectRequestMessage) message);
        }
    }

    public void handleLoginRequest(LoginRequestMessage message, Session session) {
        String newUsername = this.registerNewUsername(message.getUsername(), session);
        logger.info("Signing " + newUsername + " into chat.");
        LoginResponseMessage lres = new LoginResponseMessage(newUsername);
        try {
            session.getBasicRemote().sendText(lres.asString());
        } catch (IOException ioe) {
            logger.warning("Error signing " + message.getUsername() + " into chat : " + ioe.getMessage());
        }

        this.addToTranscriptAndNotify(newUsername, " has just joined.");
        this.broadcastUserList();
    }

    public void handleChatMessage(ChatUpdateMessage message) {
        logger.info("Receiving chat message from " + message.getUsername());
        this.addToTranscriptAndNotify(message.getUsername(), message.getMessage());
    }

    public DisconnectResponseMessage handleDisconnectRequest(DisconnectRequestMessage drm) {
        logger.info(drm.getUsername() + " would like to leave chat");
        DisconnectResponseMessage reply = new DisconnectResponseMessage(drm.getUsername());
        this.addToTranscriptAndNotify(drm.getUsername(), " has just left.");
        this.removeUserAndBroadcast(drm.getUsername());
        return reply;
    }

    @OnClose
    public void handleClientClose(Session session) {
        String username = null;
        logger.info("The web socket closed");
        for (String s : connections.keySet()) {
            if (session.equals(connections.get(s))) {
                username = s;
            }
        }

        if (username != null) {
            this.removeUserAndBroadcast(username);
            this.addToTranscriptAndNotify(username, " has just left...rather abruptly !");
        }
    }

    private void broadcastUserList() {
        logger.info("Broadcasting updated user list");
        UserListUpdateMessage ulum = new UserListUpdateMessage(new ArrayList(connections.keySet()));
        for (Session nextSession : connections.values()) {
            RemoteEndpoint.Basic remote = nextSession.getBasicRemote();
            try {
                remote.sendText(ulum.asString());
            } catch (IOException ioe) {
                logger.warning("Error updating a client " + remote + " : " + ioe.getMessage());
            }
        }
    }

    private void removeUserAndBroadcast(String username) {
        logger.info("Removing " + username + " from chat.");
        Session nextSession = connections.get(username);

        try {
            nextSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "User logged off"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        connections.remove(username);
        this.broadcastUserList();
    }

    private void broadcastUpdatedTranscript() {
        List transcriptEntry = new ArrayList();
        transcriptEntry.add(this.chatTranscript.get(this.chatTranscript.size() - 1));
        logger.info("Broadcasting updated transcript with " + transcriptEntry);

        for (Session nextSession : connections.values()) {
            RemoteEndpoint.Basic remote = nextSession.getBasicRemote();
            if (remote != null) {
                ChatTranscriptUpdateMessage cm = new ChatTranscriptUpdateMessage(transcriptEntry);
                try {
                    remote.sendText(cm.asString());
                } catch (IOException ioe) {
                    logger.warning("Error updating a client " + remote + " : " + ioe.getMessage());
                }
            }
        }
    }

    private void addToTranscriptAndNotify(String user, String message) {
        if (chatTranscript.size() > transcriptMaxLines) {
            chatTranscript.remove(0);
        }
        chatTranscript.add(user + "> " + message);
        this.broadcastUpdatedTranscript();
    }

    private String registerNewUsername(String newUsername, Session session) {
        if (connections.containsKey(newUsername)) {
            return this.registerNewUsername(newUsername + "1", session);
        }

        connections.put(newUsername, session);
        return newUsername;
    }
}
