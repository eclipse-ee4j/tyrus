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

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ManifestTest {
    private static final File localRepository = MavenUtil.getLocalMavenRepository();
    private static final Properties properties = MavenUtil.getMavenProperties();

    private static final String BUNDLE_NAME_ATTRIBUTE = "Bundle-Name";
    private static final String BUNDLE_VERSION_ATTRIBUTE = "Bundle-Version";
    private static final String[] EXCLUDED_JARS = {};

    @Test
    public void testHasOsgiManifest() throws IOException, XmlPullParserException {
        TestResult testResult = new TestResult();
        List<File> jars = MavenUtil.streamTyrusJars()
                .filter(dependency -> {
                    for (String excluded : EXCLUDED_JARS) {
                        if (dependency.getArtifactId().contains(excluded)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(dependency -> MavenUtil.getArtifactJar(localRepository, dependency, properties))
                .collect(Collectors.toList());

        for (String ATTRIBUTE : new String[]{BUNDLE_NAME_ATTRIBUTE, BUNDLE_VERSION_ATTRIBUTE}) {
            for (File jar : jars) {
                JarFile jarFile = new JarFile(jar);
                String value = jarFile.getManifest().getMainAttributes().getValue(ATTRIBUTE);
                TestResult.MessageBuilder builder = value != null ? testResult.ok() : testResult.exception();
                builder.append(jar.getName()).append(value == null ? " DOES NOT CONTAIN " : " CONTAINS ")
                        .append(ATTRIBUTE).println(" attribute");
            }
        }

        //Assertions.assertTrue(testResult.result(), "Some error occurred, see previous messages");
        Assert.assertTrue("Some error occurred, see previous messages", testResult.result());
    }

}
