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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.MessageHandler;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * Buffer used for the case when partial messages are received by the {@link MessageHandler.Whole}.
 * <p>
 * For the first received message {@link MessageHandler.Whole#onMessage(Object)} is called in a new {@link Thread} to
 * allow blocking reading of passed {@link java.io.InputStream}.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class InputStreamBuffer {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private final List<ByteBuffer> bufferedFragments = new ArrayList<ByteBuffer>();
    private final ExecutorService executorService;

    private static final Logger LOGGER = Logger.getLogger(InputStreamBuffer.class.getName());

    private volatile boolean receivedLast = false;
    private volatile BufferedInputStream inputStream = null;
    private volatile MessageHandler.Whole<InputStream> messageHandler;
    private volatile int bufferSize;
    private volatile int currentlyBuffered;
    private volatile boolean sessionClosed = false;

    /**
     * Constructor.
     */
    public InputStreamBuffer(ExecutorService executorService) {
        this.executorService = executorService;
        currentlyBuffered = 0;
    }

    /**
     * Get next received bytes.
     *
     * @return next received bytes.
     */
    public int getNextByte() throws IOException {
        lock.lock();
        try {
            if (this.bufferedFragments.isEmpty()) {
                if (receivedLast) {
                    this.inputStream = null;
                    this.currentlyBuffered = 0;
                    return -1;
                } else { // there's more to come...so wait here...
                    // don't let the reader block on a closed session
                    checkClosedSession();

                    boolean interrupted;
                    do {
                        interrupted = false;
                        try {
                            condition.await();

                            checkClosedSession();
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    } while (interrupted);
                }
            }

            if (bufferedFragments.size() == 1 && !bufferedFragments.get(0).hasRemaining() && receivedLast) {
                this.inputStream = null;
                this.currentlyBuffered = 0;
                return -1;
            }

            ByteBuffer firstBuffer = bufferedFragments.get(0);
            byte result = firstBuffer.get();

            if (!firstBuffer.hasRemaining()) {
                bufferedFragments.remove(0);
            }

            return result & 0xFF;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Finish reading of the buffer.
     */
    public void finishReading() {
        this.bufferedFragments.clear();
        this.inputStream = null;
    }

    /**
     * Append next message part to the buffer.
     *
     * @param message the message.
     * @param last    should be {@code true} iff this is the last part of the message, {@code false} otherwise.
     */
    public void appendMessagePart(ByteBuffer message, boolean last) {
        lock.lock();
        try {
            currentlyBuffered += message.remaining();
            if (currentlyBuffered <= bufferSize) {
                bufferedFragments.add(message);
            } else {
                final MessageTooBigException messageTooBigException =
                        new MessageTooBigException(LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW());
                LOGGER.log(Level.FINE, LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW(), messageTooBigException);
                receivedLast = true;
                throw messageTooBigException;
            }

            this.receivedLast = last;
            condition.signalAll();
        } finally {
            lock.unlock();
        }

        if (this.inputStream == null) {
            this.inputStream = new BufferedInputStream(this);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    messageHandler.onMessage(inputStream);
                }
            });
        }
    }

    /**
     * Set the {@link MessageHandler} that will consume the constructed {@link java.io.InputStream}.
     *
     * @param messageHandler {@link MessageHandler} that will consume the constructed {@link java.io.InputStream}.
     */
    public void setMessageHandler(MessageHandler.Whole<InputStream> messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Reset the buffer size.
     *
     * @param bufferSize the size to be set.
     */
    public void resetBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        currentlyBuffered = 0;
        bufferedFragments.clear();
    }

    void onSessionClosed() {
        sessionClosed = true;
        lock.lock();
        try {
            // wake up blocked thread
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void checkClosedSession() throws IOException {
        if (sessionClosed) {
            throw new IOException("Websocket session has been closed.");
        }
    }
}
