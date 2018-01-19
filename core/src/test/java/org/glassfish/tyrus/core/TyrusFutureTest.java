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

package org.glassfish.tyrus.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Sanity tests for TyrusFuture.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class TyrusFutureTest {

    private static final String RESULT = "You do have your moments. Not many, but you have them.";

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        TyrusFuture<String> voidTyrusFuture = new TyrusFuture<String>();
        voidTyrusFuture.setResult(RESULT);
        assertEquals(RESULT, voidTyrusFuture.get());
    }

    @Test(expected = ExecutionException.class)
    public void testException() throws ExecutionException, InterruptedException {
        TyrusFuture<String> voidTyrusFuture = new TyrusFuture<String>();
        voidTyrusFuture.setFailure(new Throwable());
        voidTyrusFuture.get();
    }

    @Test(expected = InterruptedException.class)
    public void testInterrupted() throws ExecutionException, InterruptedException {
        TyrusFuture<String> voidTyrusFuture = new TyrusFuture<String>();
        Thread.currentThread().interrupt();
        voidTyrusFuture.get();
    }

    @Test
    public void testIsDone() {
        TyrusFuture<Void> voidTyrusFuture = new TyrusFuture<Void>();
        assertFalse(voidTyrusFuture.isDone());
        voidTyrusFuture.setResult(null);
        assertTrue(voidTyrusFuture.isDone());
    }

    @Test(expected = TimeoutException.class)
    public void testTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        TyrusFuture<Void> voidTyrusFuture = new TyrusFuture<Void>();
        voidTyrusFuture.get(1, TimeUnit.MILLISECONDS);
    }
}
