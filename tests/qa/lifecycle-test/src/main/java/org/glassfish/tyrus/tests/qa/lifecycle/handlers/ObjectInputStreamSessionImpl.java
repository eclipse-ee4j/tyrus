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

package org.glassfish.tyrus.tests.qa.lifecycle.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;

import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class ObjectInputStreamSessionImpl extends SessionLifeCycle<InputStream> {

    public ObjectInputStreamSessionImpl() {
        super(false);
    }

    SendMeSomething original;

    @Override
    public void startTalk(Session s) throws IOException {
        this.original = new ObjectInputStreamSessionImpl.SendMeSomething("message", "over network", "now");
        logger.log(Level.INFO, "startTalk: Sending:{0}", this.original);
        ObjectOutputStream oos = new ObjectOutputStream(s.getBasicRemote().getSendStream());
        oos.writeObject(original);
        oos.close();
    }

    @Override
    public void startTalkPartial(Session s) {
    }

    @Override
    public void onServerMessageHandler(final InputStream is, Session session) throws IOException {
        logger.log(Level.INFO, "onServerMessageHandler:is:{0}", is.toString());
        logger.log(Level.INFO, "onServerMessageHandler:is avail:{0}", is.available());
        try {
            ObjectOutputStream oos = new ObjectOutputStream(session.getBasicRemote().getSendStream());
            /*ObjectInputStream ois = new ObjectInputStream(new InputStream() {
                @Override
                public int read() throws IOException {
                    int i = is.read();
                    //logger.log(Level.INFO, "received: " + (char)i);
                    return i;
                }
            });*/
            ObjectInputStream ois = new ObjectInputStream(is);
            logger.log(Level.INFO, "onServerMessageHandler:ois:{0}", ois.toString());
            Object objToBounce = ois.readObject();
            logger.log(Level.INFO, "onServerMessageHandler:object:{0}", objToBounce.toString());
            oos.writeObject(objToBounce);
            oos.close();
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onClientMessageHandler(InputStream message, Session session) throws IOException {
        try {
            SendMeSomething what = (SendMeSomething) new ObjectInputStream(message).readObject();
            if (what.equals(original)) {
                closeTheSessionFromClient(session);
            }
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onServerMessageHandler(InputStream message, Session session, boolean last) throws IOException {
    }

    @Override
    public void onClientMessageHandler(InputStream message, Session session, boolean last) throws IOException {
    }

    static class SendMeSomething implements Serializable {

        private String what;
        private String how;
        private String when;
        private boolean nice;

        public SendMeSomething(String what, String how, String when) {
            this.what = what;
            this.how = how;
            this.when = when;
            this.nice = false;
        }

        public String getWhat() {
            return what;
        }

        public String getHow() {
            return how;
        }

        public String getWhen() {
            return when;
        }

        public boolean isNice() {
            return nice;
        }

        public void setNice(boolean nice) {
            this.nice = nice;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof SendMeSomething) {
                SendMeSomething dst = (SendMeSomething) obj;
                if (dst.getHow().equals(how) && dst.getWhat().equals(what) && dst.getWhen().equals(when) &&
                        dst.isNice() == nice) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + Objects.hashCode(this.what);
            hash = 41 * hash + Objects.hashCode(this.how);
            hash = 41 * hash + Objects.hashCode(this.when);
            hash = 41 * hash + (this.nice ? 1 : 0);
            return hash;
        }
    }
}
