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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Simple {@link Future} implementation.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TyrusFuture<T> implements Future<T> {

    private volatile T result;
    private volatile Throwable throwable = null;
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return (latch.getCount() == 0);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();

        if (throwable != null) {
            throw new ExecutionException(throwable);
        }

        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            if (throwable != null) {
                throw new ExecutionException(throwable);
            }
            return result;
        }

        throw new TimeoutException();
    }

    /**
     * Sets the result of the message writing process.
     *
     * @param result result
     */
    public void setResult(T result) {
        if (latch.getCount() == 1) {
            this.result = result;
            latch.countDown();
        }
    }

    /**
     * Sets the failure result of message writing process.
     *
     * @param throwable throwable.
     */
    public void setFailure(Throwable throwable) {
        if (latch.getCount() == 1) {
            this.throwable = throwable;
            latch.countDown();
        }
    }
}
