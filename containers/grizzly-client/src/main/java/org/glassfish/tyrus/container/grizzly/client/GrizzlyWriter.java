/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.grizzly.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Writer;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.memory.Buffers;

import static org.glassfish.tyrus.container.grizzly.client.TaskProcessor.Task;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class GrizzlyWriter extends Writer {

    private final TaskProcessor taskProcessor;
    final org.glassfish.grizzly.Connection connection;

    public GrizzlyWriter(final org.glassfish.grizzly.Connection connection) {
        this.connection = connection;
        this.connection.configureBlocking(false);
        this.taskProcessor = new TaskProcessor(new WriterCondition());
    }

    @Override
    public void write(final ByteBuffer buffer, final CompletionHandler<ByteBuffer> completionHandler) {
        if (!connection.isOpen()) {
            completionHandler.failed(new IllegalStateException("Connection is not open."));
            return;
        }

        final Buffer message = Buffers.wrap(connection.getTransport().getMemoryManager(), buffer);

        final EmptyCompletionHandler emptyCompletionHandler = new EmptyCompletionHandler() {
            @Override
            public void cancelled() {
                if (completionHandler != null) {
                    completionHandler.cancelled();
                }
            }

            @Override
            public void completed(Object result) {
                if (completionHandler != null) {
                    completionHandler.completed(buffer);
                }
            }

            @Override
            public void failed(Throwable throwable) {
                if (completionHandler != null) {
                    completionHandler.failed(throwable);
                }
            }
        };

        taskProcessor.processTask(new WriteTask(connection, message, emptyCompletionHandler));
    }

    private class WriterCondition implements TaskProcessor.Condition {

        private final AtomicBoolean writeHandlerRegistered = new AtomicBoolean(false);

        @Override
        public boolean isValid() {
            if (!connection.canWrite() && writeHandlerRegistered.compareAndSet(false, true)) {
                connection.notifyCanWrite(new WriteHandler() {
                    @Override
                    public void onWritePossible() throws Exception {
                        writeHandlerRegistered.set(false);
                        taskProcessor.processTask();
                    }

                    @Override
                    public void onError(Throwable t) {
                        writeHandlerRegistered.set(false);
                        Logger.getLogger(GrizzlyWriter.class.getName()).log(Level.WARNING, t.getMessage(), t);
                        // TODO: do what?
                    }
                });

                return false;
            }

            return true;
        }
    }

    @Override
    public void close() {
        taskProcessor.processTask(new CloseTask(connection));
    }

    @Override
    public int hashCode() {
        return connection.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GrizzlyWriter && connection.equals(((GrizzlyWriter) obj).connection);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " " + connection.toString() + " " + connection.hashCode();
    }

    private class WriteTask extends Task {
        private final Connection connection;
        private final Buffer message;
        private final EmptyCompletionHandler completionHandler;

        private WriteTask(Connection connection, Buffer message, EmptyCompletionHandler completionHandler) {
            this.connection = connection;
            this.message = message;
            this.completionHandler = completionHandler;
        }

        @Override
        public void execute() {
            //noinspection unchecked
            connection.write(message, completionHandler);
        }
    }

    private class CloseTask extends Task {
        private final Connection connection;

        private CloseTask(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void execute() {
            connection.closeSilently();
        }
    }
}
