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

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class NoticeFilesTest {

    @Test
    public void test() throws IOException, XmlPullParserException {
        Model model = MavenUtil.getModelFromFile("../../pom.xml");
        List<NoticeDependencyVersionPair> mainExpectedNoticeDeps = mainExpectedDependencies();

        File mainNotice = new File("../../NOTICE.md");
        List<NoticeDependencyVersionPair> mainNoticeDeps = parseNoticeFileVersions(mainNotice);
        TestResult testResult = compareDependencies(mainExpectedNoticeDeps, mainNoticeDeps, model, mainNotice.getCanonicalPath());

        Assert.assertTrue("Some error occurred, see previous messages", testResult.result());
    }

    private TestResult compareDependencies(List<NoticeDependencyVersionPair> expectedDeps,
                                     List<NoticeDependencyVersionPair> actualDeps,
                                     Model model, String noticeName) {
        TestResult testResult = new TestResult();
        NextExpected:
        for (NoticeDependencyVersionPair expectedDep : expectedDeps) {
            for (NoticeDependencyVersionPair actualDep : actualDeps) {
                if (expectedDep.dependency.equals(actualDep.dependency)) {
                    String expectedVersion = findVersionInModel(expectedDep, model);
                    testResult.ok().append("Expected dependency ").append(expectedDep.dependency).println(" found");
                    if (expectedVersion.equals(actualDep.version)) {
                        testResult.ok().append("Dependency ").append(actualDep.dependency).append(" contains expected version ")
                                .append(expectedVersion).append(" in ").println(noticeName);
                    } else {
                        testResult.exception().append("Dependency ").append(actualDep.dependency).append(" differs version ")
                                .append(expectedVersion).append(" from ").append(noticeName).append(" version ")
                                .println(actualDep.version);
                    }
                    continue NextExpected;
                }
            }
            testResult.exception().append("Expected dependency ").append(expectedDep.dependency).append(" not found in ")
                    .println(noticeName);
        }
        return testResult;
    }

    private static String findVersionInModel(NoticeDependencyVersionPair pair, Model model) {
        if (pair.version.startsWith("${")) {
            String version = pair.version.substring(2, pair.version.length() - 1);
            return model.getProperties().getProperty(version);
        } else {
            return pair.version;
        }
    }

    private void cat(File path) throws IOException {
        StringTokenizer tokenizer = tokenizerFromNoticeFile(path);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.trim().length() > 1 && !token.trim().startsWith("*")) {
                System.out.println(token);
//                String filteredToken = removeUnnecessary(token);
//                System.out.println(filteredToken);
//                Pattern versionizer = Pattern.compile("([.*])?([\\d])");
//                System.out.println(versionizer.matcher(filteredToken).replaceFirst("$1:$2"));
            }
        }
    }

    private List<NoticeDependencyVersionPair> parseNoticeFileVersions(File path) throws IOException {
        List<NoticeDependencyVersionPair> list = new LinkedList<>();
        StringTokenizer tokenizer = tokenizerFromNoticeFile(path);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.trim().length() > 1 && !token.trim().startsWith("*")) {
                String filteredToken = removeUnnecessary(token);
                Pattern versionizer = Pattern.compile("([.*])?([\\d])");
                String[] args = versionizer.matcher(filteredToken).replaceFirst("$1:$2").split(":", 2);
                NoticeDependencyVersionPair pair = args.length == 2
                        ? new NoticeDependencyVersionPair(args[0], args[1])
                        : new NoticeDependencyVersionPair(args[0], "");
                list.add(pair);
            }
        }

        return list;
    }

    private StringTokenizer tokenizerFromNoticeFile(File path) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(getFile(path), "\n");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.trim().startsWith("## Third-party Content")) {
                break;
            }
        }
        return tokenizer;
    }

    private String getFile(File path) throws IOException {
        return new String(Files.newInputStream(path.toPath()).readAllBytes());
    }

    private String removeUnnecessary(String dependency) {
        String filtered = dependency
                .replace(" Version", "").replace(" version", "")
                .replace(" API ", "")
                .replace(" v", "")
                .replace(", ", "").replace(",", "")
                .replace(": ", "").replace(": ", "")
                .replace("(", "").replace(")", "");
        return filtered;
    }

    /**
     * Return pair of Notice file dependency name & pom.xml version property name
     */
    private List<NoticeDependencyVersionPair> mainExpectedDependencies() {
        final List<NoticeDependencyVersionPair> dependencyPairs = new LinkedList<>();
        dependencyPairs.add(new NoticeDependencyVersionPair("jakarta.activation", "${activation.api.version}"));
        dependencyPairs.add(new NoticeDependencyVersionPair("jakarta.enterprise.cdi-api", "${cdi-api.version}"));
        dependencyPairs.add(new NoticeDependencyVersionPair("jakarta.inject", "${inject.api.version}"));
        dependencyPairs.add(new NoticeDependencyVersionPair("jakarta.xml.bind-api", "${jaxb.api.version}"));
        dependencyPairs.add(new NoticeDependencyVersionPair("jline", "${jline.version}"));
        return dependencyPairs;
    }

    private static class NoticeDependencyVersionPair {
        private final String dependency;
        private final String version;

        private NoticeDependencyVersionPair(String dependency, String version) {
            this.dependency = dependency.trim();
            this.version = version.trim();
        }
    }
}
