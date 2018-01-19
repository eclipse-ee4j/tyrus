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

package org.glassfish.tyrus.container.jdk.client;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.tyrus.spi.CompletionHandler;

/**
 * Queues {@link #write(java.nio.ByteBuffer, org.glassfish.tyrus.spi.CompletionHandler)}, {@link #close()}
 * and {@link #startSsl()} method calls and passes them to a downstream filter one at a time. Both
 * {@link org.glassfish.tyrus.container.jdk.client.SslFilter} and {@link
 * org.glassfish.tyrus.container.jdk.client.TransportFilter} allow {@link #write(java.nio.ByteBuffer,
 * org.glassfish.tyrus.spi.CompletionHandler)} method call only after the previous one has completed. Queueing {@link
 * #close()} method calls ensures that {@link #write(java.nio.ByteBuffer, org.glassfish.tyrus.spi.CompletionHandler)}
 * methods called before {@link #close()} will be processed. Including {@link #startSsl()} methods in the queue ensures
 * that no {@link #write(java.nio.ByteBuffer, org.glassfish.tyrus.spi.CompletionHandler)} method will be passed to
 * {@link org.glassfish.tyrus.container.jdk.client.SslFilter} while it performs SSL handshake.
 *
 * @author Petr Janouch
 */
class TaskQueueFilter extends Filter {

    private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean taskLock = new AtomicBoolean(false);

    /**
     * Constructor.
     *
     * @param downstreamFilter a filter that is positioned directly under this filter.
     */
    TaskQueueFilter(Filter downstreamFilter) {
        super(downstreamFilter);
    }

    @Override
    void write(ByteBuffer data, CompletionHandler<ByteBuffer> completionHandler) {
        taskQueue.offer(new WriteTask(data, completionHandler));
        if (taskLock.compareAndSet(false, true)) {
            processTask();
        }
    }

    private void processTask() {
        final Task task = taskQueue.poll();
        if (task == null) {
            taskLock.set(false);
            return;
        }
        task.execute(this);
    }

    @Override
    void close() {
        // close task
        taskQueue.offer(new Task() {
            @Override
            public void execute(TaskQueueFilter queueFilter) {
                if (downstreamFilter != null) {
                    downstreamFilter.close();
                    upstreamFilter = null;
                }
                processTask();
            }
        });

        if (taskLock.compareAndSet(false, true)) {
            processTask();
        }
    }

    @Override
    void startSsl() {
        // start SSL task
        taskQueue.offer(new Task() {

            @Override
            public void execute(TaskQueueFilter queueFilter) {
                downstreamFilter.startSsl();
            }
        });

        if (taskLock.compareAndSet(false, true)) {
            processTask();
        }
    }

    @Override
    void processSslHandshakeCompleted() {
        processTask();
    }

    /**
     * A task to be queued in order to be processed one at a time.
     */
    static interface Task {
        /**
         * Execute the task.
         *
         * @param queueFilter write queue filter this task should be executed in.
         */
        void execute(TaskQueueFilter queueFilter);
    }

    /**
     * A task that writes data to the downstreamFilter.
     */
    static class WriteTask implements Task {
        private final ByteBuffer data;
        private final org.glassfish.tyrus.spi.CompletionHandler<ByteBuffer> completionHandler;

        WriteTask(ByteBuffer data, org.glassfish.tyrus.spi.CompletionHandler<ByteBuffer> completionHandler) {
            this.data = data;
            this.completionHandler = completionHandler;
        }

        @Override
        public void execute(final TaskQueueFilter queueFilter) {
            queueFilter.downstreamFilter.write(getData(), new CompletionHandler<ByteBuffer>() {

                @Override
                public void failed(Throwable throwable) {
                    getCompletionHandler().failed(throwable);
                    queueFilter.processTask();
                }

                @Override
                public void completed(ByteBuffer result) {
                    if (result.hasRemaining()) {
                        execute(queueFilter);
                        return;
                    }

                    getCompletionHandler().completed(getData());
                    queueFilter.processTask();
                }
            });
        }

        ByteBuffer getData() {
            return data;
        }

        CompletionHandler<ByteBuffer> getCompletionHandler() {
            return completionHandler;
        }

        @Override
        public String toString() {
            return "WriteTask{data=" + data + ", completionHandler=" + completionHandler + '}';
        }
    }
}
