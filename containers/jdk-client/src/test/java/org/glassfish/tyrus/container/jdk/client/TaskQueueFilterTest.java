/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.tyrus.spi.CompletionHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that {@link TaskQueueFilter} does not leave a write in its queue when the write is added while the thread
 * completing the previous write has already found the queue empty but has not yet released the task lock.
 * <p/>
 * The sender's {@code compareAndSet} fails in that window, so unless the completing thread checks the queue again
 * after releasing the lock, nothing writes the queued data and the sender waits for it indefinitely (see issue 678).
 */
public class TaskQueueFilterTest {

    private static final long TIMEOUT_SECONDS = 10;

    @Test
    public void testWriteQueuedBeforeTaskLockReleaseIsWritten() throws Exception {
        final List<ByteBuffer> written = new CopyOnWriteArrayList<>();
        final AtomicReference<CompletionHandler<ByteBuffer>> pendingWrite = new AtomicReference<>();
        final TaskQueueFilter taskQueueFilter = new TaskQueueFilter(new Filter(null) {
            @Override
            void write(ByteBuffer data, CompletionHandler<ByteBuffer> completionHandler) {
                // record the write and complete it later from another thread, like the transport does
                written.add(data);
                pendingWrite.set(completionHandler);
            }
        });

        // stop the completing thread right after poll() has found the queue empty, while it still holds the task lock
        final AtomicBoolean holdNextEmptyPoll = new AtomicBoolean(false);
        final CountDownLatch emptyPollReached = new CountDownLatch(1);
        final CountDownLatch secondWriteQueued = new CountDownLatch(1);
        setTaskQueue(taskQueueFilter, new ConcurrentLinkedQueue<TaskQueueFilter.Task>() {
            @Override
            public TaskQueueFilter.Task poll() {
                final TaskQueueFilter.Task task = super.poll();
                if (task == null && holdNextEmptyPoll.compareAndSet(true, false)) {
                    emptyPollReached.countDown();
                    await(secondWriteQueued);
                }
                return task;
            }
        });

        taskQueueFilter.write(ByteBuffer.wrap(new byte[] {1}), new CompletionHandler<ByteBuffer>() {
        });
        assertEquals(1, written.size());

        // complete the first write; the completing thread takes the next task, finds none and is held there
        holdNextEmptyPoll.set(true);
        final Thread completingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pendingWrite.get().completed(ByteBuffer.allocate(0));
            }
        });
        completingThread.start();
        assertTrue("the completing thread did not reach the empty queue",
                   emptyPollReached.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // the sender writes the next message while the task lock is still held, so its compareAndSet fails
        taskQueueFilter.write(ByteBuffer.wrap(new byte[] {2}), new CompletionHandler<ByteBuffer>() {
        });
        secondWriteQueued.countDown();
        completingThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertEquals("the second write was left in the queue and never passed downstream", 2, written.size());
        assertEquals(2, written.get(1).get(0));
    }

    private static void setTaskQueue(TaskQueueFilter taskQueueFilter, ConcurrentLinkedQueue<TaskQueueFilter.Task> queue)
            throws Exception {
        final Field taskQueueField = TaskQueueFilter.class.getDeclaredField("taskQueue");
        taskQueueField.setAccessible(true);
        taskQueueField.set(taskQueueFilter, queue);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
