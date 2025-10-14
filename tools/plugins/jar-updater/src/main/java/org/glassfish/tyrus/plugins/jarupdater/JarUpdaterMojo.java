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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "update", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class JarUpdaterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "files")
    List<String> files;

    @Parameter(property = "dest")
    String dest;

    @Parameter(property = "jar")
    String jar;

    @Parameter(property = "ignoreOnError", defaultValue = "false")
    String ignoreOnError;

    @Parameter(name = "verbose", property = "verbose", defaultValue = "true")
    String verboseParam;

    @Parameter(property = "skip", name = "skip")
    String skip;

    File absoluteJar;
    List<File> sources;
    List<Pattern> skipPatterns;
    boolean ignore;
    boolean verbose;

    @Override
    public void execute() throws MojoExecutionException {
        ignore = Boolean.parseBoolean(ignoreOnError);
        verbose = Boolean.parseBoolean(verboseParam);

        try {
            prepareSkipPatterns();
            if (!skipModule()) {
                validateOptions();
                updateJar();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e);
        }
    }

    private void updateJar() throws IOException, URISyntaxException {
        try (FileSystem fs = FileSystems.newFileSystem(absoluteJar.toPath(), (ClassLoader) null)) {
            for (File fileToZip : sources) {
                if (verbose) {
                    System.out.append("Adding ").append(fileToZip.getName()).append(" to ")
                            .append(absoluteJar.getName()).append('/').println(dest);
                }
                Path pathInZipfile = fs.getPath(dest + "/" + fileToZip.getName());
                ensureFolder(pathInZipfile);
                Files.copy(fileToZip.toPath(), pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }

    private void ensureFolder(Path path) throws IOException {
        if (path.getParent() != null) {
            if (!Files.exists(path.getParent())) {
                ensureFolder(path.getParent());
                Files.createDirectory(path.getParent());
            }
        }
    }

    private void validateOptions() throws MojoExecutionException, IOException {
        if (jar == null) {
            throw new MojoExecutionException("Jar name not defined");
        }

        // -----------

        absoluteJar = validateJar(jar);

        if (dest == null || dest.isEmpty()) {
            throw new MojoExecutionException("Destination is mandatory");
        }

        // -----------

        if (files == null) {
            throw new MojoExecutionException("Files set is required");
        }

        sources = new ArrayList<>(files.size());
        for (String file : files) {
            validateSource(file);
        }
    }

    private void validateSource(String file) throws MojoExecutionException {
        File absolute = validateAbsolute(file, ignore);
        if (absolute != null) {
            if (!absolute.isDirectory()) {
                sources.add(absolute);
            } else {
                listAllFiles(absolute.toPath(), sources);
            }
        }
    }

    private File validateJar(String source) throws MojoExecutionException {
        File absolute = validateAbsolute(source, false);
        if (absolute.isDirectory()) {
            throw new MojoExecutionException("Provided source " + source + " is a directory");
        }
        return absolute;
    }

    private File validateAbsolute(String source, boolean ignore) throws MojoExecutionException {
        File absolute = getAbsolute(source);
        if (!absolute.exists()) {
            if (!ignore) {
                throw new MojoExecutionException("Provided source " + source + " does not exist");
            } else {
                System.out.println("Warning: Ignoring non-existing " + source);
                return null;
            }
        }
        return absolute;
    }

    private File getAbsolute(String sfile) {
        File file = new File(sfile);
        if (!file.isAbsolute()) {
            file = new File(project.getBasedir(), sfile);
        }
        return file;
    }

    private void prepareSkipPatterns() {
        if (skip != null) {
            String[] skipIdss = skip.split(",");
            skipPatterns = new ArrayList<>(skipIdss.length);
            for (String skip : skipIdss) {
                if (!skip.isEmpty()) {
                    String pat = skip.trim().replace(".", "\\.").replace("*", ".*");
                    Pattern pattern = Pattern.compile(pat);
                    skipPatterns.add(pattern);
                }
            }
        } else {
            skipPatterns = Collections.emptyList();
        }
    }

    private boolean skipModule() {
        String moduleName = project.getGroupId() + ":" + project.getArtifactId();
        for (Pattern pattern : skipPatterns) {
            Matcher matcher = pattern.matcher(moduleName);
            if (matcher.matches()) {
                System.out.append("Skipping module ").println(moduleName);
                return true;
            }
        }
        return false;
    }

    private static void listAllFiles(Path currentPath, List<File> allFiles) throws MojoExecutionException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listAllFiles(entry, allFiles);
                } else {
                    allFiles.add(entry.toFile());
                }
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe);
        }
    }
}
