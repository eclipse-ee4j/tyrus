/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.plugins.jarupdater;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarCreatorIT {
    @Test
    public void testJarContent() throws IOException {
        int verify = 0;
        int lastVerify = 0;
        FileInputStream fis = new FileInputStream("target/compressed.jar");
        JarInputStream zipOut = new JarInputStream(fis);
        JarEntry entry;
        while ((entry = zipOut.getNextJarEntry()) != null) {
            if (entry.getName().equals("test1.txt")) {
                verify += 10;
            }
            if (entry.getName().equals("META-INF/LICENSE.md")) {
                verify += 100;
            }
            if (entry.getName().equals("META-INF/NOTICE.md")) {
                verify += 1000;
            }
            if (entry.getName().equals("META-INF/versions/11/module-info.clz")) {
                verify += 10000;
            }
            if (verify != lastVerify) {
                System.out.append("Found ").println(entry.getName());
                lastVerify = verify;
            }
        }
        fis.close();

        Assertions.assertEquals(11110, verify);
    }
}
