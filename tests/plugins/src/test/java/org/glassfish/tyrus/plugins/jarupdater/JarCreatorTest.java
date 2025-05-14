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

import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarCreatorTest {
    @Test
    public void testCreateJar() throws IOException {
        String sourceFile = "test1.txt";
        FileOutputStream fos = new FileOutputStream("target/compressed.jar");
        JarOutputStream zipOut = new JarOutputStream(fos);

        JarEntry zipEntry = new JarEntry(sourceFile);
        zipOut.putNextEntry(zipEntry);
        zipOut.closeEntry();

        zipOut.close();
        fos.close();
    }
}
