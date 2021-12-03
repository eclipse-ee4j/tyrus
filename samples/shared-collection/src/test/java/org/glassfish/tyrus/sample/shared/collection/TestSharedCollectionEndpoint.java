/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.shared.collection;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class TestSharedCollectionEndpoint extends TestContainer {

    public TestSharedCollectionEndpoint() {
        setContextPath("/sample-shared-collection");
    }

    @Test
    public void testInit() throws DeploymentException, IOException, InterruptedException {
        final Server server = startServer(SharedCollectionEndpoint.class);

        try {
            final CountDownLatch updateLatch = new CountDownLatch(1);

            final SharedMap sharedMap1 = new SharedMap(createClient(), getURI(SharedCollectionEndpoint.class),
                                                       new SharedMap.UpdateListener() {
                                                           @Override
                                                           public void onUpdate() {
                                                               updateLatch.countDown();
                                                           }
                                                       });

            assertTrue(updateLatch.await(5, TimeUnit.SECONDS));

            // initial content = [{Red Leader}, {Red Two}, {Red Three}, ...]
            assertEquals(5, sharedMap1.size());

        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testClear() throws DeploymentException, IOException, InterruptedException {
        final Server server = startServer(SharedCollectionEndpoint.class);

        try {
            // init
            final CountDownLatch updateLatch1 = new CountDownLatch(2);
            // init, put
            final CountDownLatch updateLatch2 = new CountDownLatch(2);

            final SharedMap sharedMap1 = new SharedMap(
                    createClient(), getURI(SharedCollectionEndpoint.class),
                    new SharedMap.UpdateListener() {
                        @Override
                        public void onUpdate() {
                            updateLatch1.countDown();
                        }
                    });

            final SharedMap sharedMap2 = new SharedMap(
                    createClient(), getURI(SharedCollectionEndpoint.class),
                    new SharedMap.UpdateListener() {
                        @Override
                        public void onUpdate() {
                            updateLatch1.countDown();
                            updateLatch2.countDown();
                        }
                    });

            // waiting for init - connect + initial values
            assertTrue(updateLatch1.await(5, TimeUnit.SECONDS));

            sharedMap1.clear();

            // init + values + clear from sharedMap1
            assertTrue(updateLatch2.await(5, TimeUnit.SECONDS));

            assertEquals(0, sharedMap2.size());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testPut() throws DeploymentException, IOException, InterruptedException {
        final Server server = startServer(SharedCollectionEndpoint.class);

        try {
            // init
            final CountDownLatch updateLatch1 = new CountDownLatch(2);
            // init, put
            final CountDownLatch updateLatch2 = new CountDownLatch(2);

            final SharedMap sharedMap1 = new SharedMap(
                    createClient(), getURI(SharedCollectionEndpoint.class),
                    new SharedMap.UpdateListener() {
                        @Override
                        public void onUpdate() {
                            updateLatch1.countDown();
                        }
                    });

            final SharedMap sharedMap2 = new SharedMap(
                    createClient(), getURI(SharedCollectionEndpoint.class),
                    new SharedMap.UpdateListener() {
                        @Override
                        public void onUpdate() {
                            updateLatch1.countDown();
                            updateLatch2.countDown();
                        }
                    });

            // waiting for init - connect + initial values
            assertTrue(updateLatch1.await(5, TimeUnit.SECONDS));

            sharedMap1.put(TestSharedCollectionEndpoint.class.getName(), "test");

            // init + values + put from sharedMap1
            assertTrue(updateLatch2.await(5, TimeUnit.SECONDS));

            final String value = sharedMap2.get(TestSharedCollectionEndpoint.class.getName());
            assertNotNull(value);
            assertEquals("test", value);

        } finally {
            stopServer(server);
        }
    }
}
