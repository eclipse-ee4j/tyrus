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

package org.glassfish.tyrus.spi;

/**
 * A callback to notify about asynchronous I/O operations status updates.
 *
 * @param <E> result type.
 * @author Alexey Stashok
 */
public abstract class CompletionHandler<E> {

    /**
     * The operation was cancelled.
     */
    public void cancelled() {
    }

    /**
     * The operation was failed.
     *
     * @param throwable error, which occurred during operation execution.
     */
    public void failed(final Throwable throwable) {
    }

    /**
     * The operation was completed.
     *
     * @param result the operation result.
     */
    public void completed(final E result) {
    }

    /**
     * The callback method may be called, when there is some progress in
     * operation execution, but it is still not completed.
     *
     * @param result the current result.
     */
    public void updated(final E result) {
    }
}
