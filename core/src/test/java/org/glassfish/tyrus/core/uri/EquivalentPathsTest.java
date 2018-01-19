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

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author dannycoward
 * @author Petr Janouch
 */
public class EquivalentPathsTest {

    @Test
    public void testBasic() {
        assertTrue(checkForEquivalents("/a/b", "/a/b", "/a/b/c"));
    }

    @Test
    public void testTemplates() {
        assertFalse(checkForEquivalents("/a/{var2}", "/a/b", "/b/{var29}"));
    }

    @Test
    public void testMorePaths() {
        assertTrue(checkForEquivalents("/a/{var2}/c", "/a/{var2}", "/b/{var2}/c", "/b/{var2}/{var3}", "/a/{m}"));
    }

    /**
     * Check for equivalent path.
     *
     * @param paths list of paths.
     * @return {@code true} if at least two path in given list are equivalent, {@code false} otherwise.
     */
    private boolean checkForEquivalents(String... paths) {
        for (int i = 0; i < paths.length; i++) {
            String nextPath = paths[i];

            for (int j = 0; j < paths.length; j++) {

                if (j != i) {
                    if (Match.isEquivalent(nextPath, paths[j])) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
