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

package org.glassfish.tyrus.container.grizzly.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class responsible for processing {@link Task}. It ensures that only one task will be processed at a time, because
 * Grizzly Worker-thread IOStrategy does not wait until one message is processed before dispatching another one.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Petr Janouch
 */
public class TaskProcessor {

    private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<Task>();
    private final Condition condition;
    /**
     * A lock that indicates that a thread is processing a task.
     */
    private final Lock taskLock = new ReentrantLock();

    /**
     * Constructor.
     *
     * @param condition if present, it will be called before processing each {@link Task}. When {@link
     *                  org.glassfish.tyrus.container.grizzly.client.TaskProcessor.Condition#isValid()}
     *                  returns {@code false}, processing will be terminated. If {@code null},
     *                  all tasks from the queue will be processed.
     */
    public TaskProcessor(Condition condition) {
        this.condition = condition;
    }

    /**
     * Constructor.
     * <p>
     * There is no condition that has to be checked before processing each task.
     */
    public TaskProcessor() {
        this.condition = null;
    }

    /**
     * Add a task to the task queue and process as much tasks from the task queue as possible.
     *
     * @param task {@link Task} that should be processed.
     */
    public void processTask(Task task) {
        taskQueue.offer(task);
        processTask();
    }

    /**
     * Process as much tasks from task queue as possible.
     */
    public void processTask() {
        if (!taskLock.tryLock()) {
            // there is another thread processing a task it will take care of this task too
            return;
        }

        try {
            while (!taskQueue.isEmpty()) {
                if (condition != null && !condition.isValid()) {
                    return;
                }

                final Task first = taskQueue.poll();
                if (first == null) {
                    continue;
                }

                first.execute();
            }
        } finally {
            taskLock.unlock();
        }

        /*
         * There is a small chance that another thread will manage to add a task to the queue in the moment when the
         * thread processing tasks has left the while cycle, but has not released the lock yet. In that case a task
         * might be added to the queue and stay there indefinitely. It is quite improbable, but the thread that has
         * finished processing tasks should try to process more tasks after releasing the lock.
         */
        if (!taskQueue.isEmpty()) {
            processTask();
        }
    }

    /**
     * Generic task representation.
     */
    public abstract static class Task {
        /**
         * To be overridden.
         */
        public abstract void execute();
    }

    /**
     * Condition used in {@link #processTask(org.glassfish.tyrus.container.grizzly.client.TaskProcessor.Task)}.
     */
    public interface Condition {

        /**
         * Check the condition.
         *
         * @return {@code true} when condition is valid, {@code false otherwise}.
         */
        public boolean isValid();
    }
}
