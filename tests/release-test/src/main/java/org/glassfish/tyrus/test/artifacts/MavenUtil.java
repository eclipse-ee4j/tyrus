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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public final class MavenUtil {

    static final String TYRUS_VERSION = "tyrus.version";
    private static final String PROJECT_VERSION = "project.version";

    static File getArtifactJar(File repositoryRoot, Dependency dependency, Properties properties) {
        return getArtifactFile(repositoryRoot, dependency, properties, "jar");
    }

    private static File getArtifactFile(File repositoryRoot, Dependency dependency, Properties properties, String extension) {
        StringBuilder fileSuffix = new StringBuilder();
        String groupIdParts[] = dependency.getGroupId().split("\\.");
        for (String groupIdPart : groupIdParts) {
            fileSuffix.append(groupIdPart).append(File.separator);
        }
        String artifactIdParts[] = dependency.getArtifactId().split("\\.");
        for (String artifactIdPart : artifactIdParts) {
            fileSuffix.append(artifactIdPart).append(File.separator);
        }
        String version = MavenUtil.getDependencyVersion(dependency, properties);
        fileSuffix.append(version).append(File.separator);
        fileSuffix.append(dependency.getArtifactId()).append('-').append(version).append(".").append(extension);
        return new File(repositoryRoot, fileSuffix.toString());
    }

    static String getDependencyVersion(Dependency dependency, Properties properties) {
        String version = dependency.getVersion();
        if (version.startsWith("${") && version.endsWith("}")) {
            String property = version.substring(2, version.length() - 1);
            final String value;
            switch (property) {
                case TYRUS_VERSION: // in pom.xml
                case PROJECT_VERSION: // in bom.pom
                    value = getTyrusVersion(properties);
                    break;
                default:
                    value = properties.getProperty(property);
                    break;
            }
            version = value == null ? version : value;
        }
        return version;
    }

    static File getLocalMavenRepository() {
        String folder = System.getProperty("localRepository");
        return new File(folder);
    }

    static Properties getMavenProperties() {
        try {
            Model model = getModelFromFile("pom.xml");
            return model.getProperties();
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Stream<Dependency> keepTyrusJars(Stream<Dependency> stream, DependencyPair... keep) {
        return stream.filter(dependency -> {
            for (DependencyPair pair : keep) {
                if (dependency.getGroupId().equals(pair.groupId()) && dependency.getArtifactId().equals(pair.artifactId())) {
                    return true;
                }
            }
            return false;
        });
    }

    static Stream<Dependency> streamTyrusJars() throws IOException, XmlPullParserException {
        Model model = getModelFromFile("pom.xml");
        List<Dependency> deps = getBomPomDependencies(model);

        return deps.stream()
                .filter(dep -> dep.getGroupId().startsWith("org.glassfish.tyrus"))
                .filter(dep -> dep.getType().equals("jar"));
    }

    static Model getModelFromFile(String fileName) throws IOException, XmlPullParserException {
        File pomFile = new File(fileName);
        return getModelFromFile(pomFile);
    }

    private static Model getModelFromFile(File file) throws IOException, XmlPullParserException {
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        try (Reader fileReader = Files.newBufferedReader(file.toPath())) {
            Model model = mavenReader.read(fileReader);
            return model;
        }
    }

    private static List<Dependency> getBomPomDependencies(Model model) throws IOException, XmlPullParserException {
        Dependency bomPom = null;
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        for (Dependency dependency : dependencies) {
            if (dependency.getGroupId().equals("org.glassfish.tyrus") && dependency.getArtifactId().equals("tyrus-bom")) {
                bomPom = dependency;
                break;
            }
        }
        if (bomPom == null) {
            throw new IllegalStateException("Bom pom not found");
        }
        File pom = getArtifactFile(getLocalMavenRepository(), bomPom, model.getProperties(), "pom");
        Model bomPomModel = getModelFromFile(pom);
        return bomPomModel.getDependencyManagement().getDependencies();
    }

    static String getTyrusVersion(Properties properties) {
        String property = properties.getProperty(TYRUS_VERSION); // when it is in the pom.file
        if (property == null || property.startsWith("${")) {
            property = System.getProperty(TYRUS_VERSION);        // not in pom, but -Dtyrus.version
        }
        if (property == null || property.startsWith("${")) {
            throw new IllegalStateException("Property " + TYRUS_VERSION + " not set (-Dtyrus.version=)");
        }
        return property;
    }

    /* Unused at the moment, but could be useful in the future in the case of profiles are needed */
    private static List<Dependency> getProfileDependency(Model model) {
        List<Dependency> profileDependencies = Collections.EMPTY_LIST;
        List<Profile> profiles = model.getProfiles();
        String activeProfile = getActiveProfile();
        for (Profile profile : profiles) {
            if (activeProfile.equals(profile.getId())) {
                profileDependencies = profile.getDependencies();
                break;
            }
        }
        return profileDependencies;
    }

    private static String getActiveProfile() {
        String profileId = System.getProperty("profileId"); // set this to the surefire plugin
        return profileId;
    }
}
