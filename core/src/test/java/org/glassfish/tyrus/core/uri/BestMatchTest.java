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

package org.glassfish.tyrus.core.uri;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.core.ComponentProviderService;
import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusEndpointWrapper;

import org.junit.Test;
import static org.junit.Assert.assertNull;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

/**
 * @author dannycoward
 * @author Petr Janouch
 */
public class BestMatchTest {

    @Test
    public void testBasicExactMatch() {
        try {
            List<TestWebSocketEndpoint> endpoints = Arrays.asList(new TestWebSocketEndpoint("/a"), new
                    TestWebSocketEndpoint("/a/b"), new TestWebSocketEndpoint("/a/b/c"));

            verifyResult(endpoints, "/a", "/a");
            verifyResult(endpoints, "/a/b", "/a/b");
            verifyResult(endpoints, "/a/b/c", "/a/b/c");
            verifyResult(endpoints, "/d", null);
            verifyResult(endpoints, "/", null);
            verifyResult(endpoints, "/a/b/c/d", null);
            verifyResult(endpoints, "/d/d/d", null);

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testSingleVariableTemplates() {
        try {
            List<TestWebSocketEndpoint> endpoints = Collections.singletonList(new TestWebSocketEndpoint("/a/{var}"));

            verifyResult(endpoints, "/a", null);
            verifyResult(endpoints, "/a/b/c", null);
            verifyResult(endpoints, "/a/b", "/a/{var}");

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testMultipleVariableTemplates() {
        try {
            List<TestWebSocketEndpoint> endpoints = Arrays.asList(new TestWebSocketEndpoint("/{var1}"), new
                    TestWebSocketEndpoint("/{var1}/{var2}"), new TestWebSocketEndpoint("/{var1}/{var2}/{var3}"));

            verifyResult(endpoints, "/a", "/{var1}");
            verifyResult(endpoints, "/a/b", "/{var1}/{var2}");
            verifyResult(endpoints, "/a/b/c", "/{var1}/{var2}/{var3}");
            verifyResult(endpoints, "/a/b/c/d", null);

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testExactMatchWinsOverVariableMatch() {
        try {
            List<TestWebSocketEndpoint> endpoints = Arrays.asList(new TestWebSocketEndpoint("/a/b/c"), new
                    TestWebSocketEndpoint("/a/{var2}/{var3}"), new TestWebSocketEndpoint("/a/{var2}/c"));

            verifyResult(endpoints, "/a/b/c", "/a/b/c");
            verifyResult(endpoints, "/a/d/c", "/a/{var2}/c");
            verifyResult(endpoints, "/a/x/y", "/a/{var2}/{var3}");

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testLeftRightMatchPrecedence() {
        try {
            List<TestWebSocketEndpoint> endpoints = Arrays.asList(new TestWebSocketEndpoint("/{var1}/d"), new
                    TestWebSocketEndpoint("/b/{var2}"));

            verifyResult(endpoints, "/b/d", "/b/{var2}");

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testMoreLeftRightPrecedenceMatch() {
        try {
            List<TestWebSocketEndpoint> endpoints = Arrays.asList(
                    new TestWebSocketEndpoint("/a"), new TestWebSocketEndpoint("/{var1}"),
                    new TestWebSocketEndpoint("/a/b"), new TestWebSocketEndpoint("/{var1}/b"),
                    new TestWebSocketEndpoint("/a/{var2}"));

            verifyResult(endpoints, "/a", "/a");
            verifyResult(endpoints, "/x", "/{var1}");
            verifyResult(endpoints, "/a/b", "/a/b");
            verifyResult(endpoints, "/x/y", null);
            verifyResult(endpoints, "/x/b", "/{var1}/b");
            verifyResult(endpoints, "/a/y", "/a/{var2}");

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        }
    }


    private void verifyResult(List<TestWebSocketEndpoint> endpoints, String testedUri, String expectedMatchedPath) {
        Match m = getBestMatch(testedUri, new HashSet<TyrusEndpointWrapper>(endpoints));
        System.out.println("  Match for " + testedUri + " calculated is: " + m);

        if (expectedMatchedPath != null) {
            assertNotNull("Was expecting a match on " + expectedMatchedPath + ", but didn't get one.", m);
            assertEquals("Wrong path matched.", expectedMatchedPath, m.getEndpointWrapper().getEndpointPath());
        } else { // shouldn't be a match
            assertNull("Wasn't expecting a match, but got one.", m);
        }
    }

    private Match getBestMatch(String incoming, Set<TyrusEndpointWrapper> thingsWithPath) {
        List<Match> sortedMatches = Match.getAllMatches(incoming, thingsWithPath, new DebugContext());
        if (sortedMatches.isEmpty()) {
            return null;
        } else {
            return sortedMatches.get(0);
        }
    }

    private static class TestWebSocketEndpoint extends TyrusEndpointWrapper {

        private final String path;

        private TestWebSocketEndpoint(String path) throws DeploymentException {
            super(TestEndpoint.class, null, ComponentProviderService.createClient(), null, null, null, null, null,
                  null, null);
            this.path = path;
        }

        @Override
        public String getEndpointPath() {
            return path;
        }

        public static class TestEndpoint extends Endpoint {
            @Override
            public void onOpen(Session session, EndpointConfig config) {

            }
        }
    }
}
