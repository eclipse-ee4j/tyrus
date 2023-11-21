/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.artifacts;

import java.util.Arrays;

class DependencyPair {
    private final String groupId;
    private final String artifactId;

    DependencyPair(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    String artifactId() {
        return artifactId;
    }

    String groupId() {
        return groupId;
    }

    static DependencyPair[] concat(DependencyPair[] first, DependencyPair[] second) {
        DependencyPair[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @Override
    public String toString() {
        return groupId + ':' + artifactId;
    }
}
