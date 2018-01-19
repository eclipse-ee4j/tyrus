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
import java.io.Reader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.MessageHandler;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * Buffer used for the case when partial messages are received by the {@link MessageHandler.Whole}.
 * <p>
 * For the first received message {@link MessageHandler.Whole#onMessage(Object)} is called within a new executor to
 * allow blocking reading of passed {@link Reader}.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class ReaderBuffer {

    private final AtomicBoolean buffering = new AtomicBoolean(true);
    private final ExecutorService executorService;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private static final Logger LOGGER = Logger.getLogger(ReaderBuffer.class.getName());

    private volatile boolean receivedLast = false;
    private volatile int bufferSize;
    private volatile int currentlyBuffered;
    private volatile StringBuffer buffer;
    private volatile BufferedStringReader reader = null;
    private volatile MessageHandler.Whole<Reader> messageHandler;
    private volatile boolean sessionClosed = false;

    /**
     * Constructor.
     */
    public ReaderBuffer(ExecutorService executorService) {
        this.buffer = new StringBuffer();
        this.executorService = executorService;
        currentlyBuffered = 0;
    }

    /**
     * Get next received chars.
     *
     * @return next received chars.
     */
    public char[] getNextChars(int number) throws IOException {
        lock.lock();
        try {
            if (buffer.length() == 0) {
                if (receivedLast) {
                    this.reader = null;
                    buffering.set(true);
                    this.currentlyBuffered = 0;
                    return null;
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

            int size = number > buffer.length() ? buffer.length() : number;

            char[] result = new char[size];
            buffer.getChars(0, size, result, 0);
            buffer.delete(0, size);

            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Finish reading of the buffer.
     */
    public void finishReading() {
        buffer = new StringBuffer();
        reader = null;
    }

    /**
     * Append next message part to the buffer.
     *
     * @param message the message.
     * @param last    should be {@code true} iff this is the last part of the message, {@code false} otherwise.
     */
    public void appendMessagePart(String message, boolean last) {
        lock.lock();
        try {
            currentlyBuffered += message.length();
            if (currentlyBuffered <= bufferSize) {
                buffer.append(message);
            } else {
                if (buffering.get()) {
                    buffering.set(false);
                    final MessageTooBigException messageTooBigException =
                            new MessageTooBigException(LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW());
                    LOGGER.log(Level.FINE, LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW(),
                               messageTooBigException);
                    receivedLast = true;
                    throw messageTooBigException;
                }
            }

            this.receivedLast = last;
            condition.signalAll();
        } finally {
            lock.unlock();
        }

        if (this.reader == null) {
            this.reader = new BufferedStringReader(this);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    messageHandler.onMessage(reader);
                }
            });
        }
    }

    /**
     * Set the {@link MessageHandler} that will consume the constructed {@link java.io.InputStream}.
     *
     * @param messageHandler {@link MessageHandler} that will consume the constructed {@link java.io.InputStream}.
     */
    public void setMessageHandler(MessageHandler.Whole<Reader> messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Reset the buffer size.
     *
     * @param bufferSize the size to be set.
     */
    public void resetBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        buffering.set(true);
        currentlyBuffered = 0;
        buffer.delete(0, buffer.length());
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
