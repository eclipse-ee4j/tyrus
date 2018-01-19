/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * @author michal.conos at oracle.com
 */
public class Misc {

    private static final Logger logger = Logger.getLogger(Misc.class.getCanonicalName());
    private static final char UNIX_SEPARATOR = '/';
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * Copy file set to the target directory. If the directory does not exist it is created.
     *
     * @param fileSet      set of files to be copied
     * @param dstDirectory destination where the files are copied
     */
    public static void copyFiles(Set<File> fileSet, File dstDirectory, String regex, String move) throws IOException {
        if (!dstDirectory.isDirectory()) {
            FileUtils.forceMkdir(dstDirectory);
        }
        for (File src : fileSet) {
            File srcParent = src.getParentFile();
            String targetDir = FilenameUtils.separatorsToUnix(srcParent.toString());
            if (regex != null) {
                if (move == null) {
                    move = "";
                }
                targetDir = targetDir.replaceFirst(regex, move);
            }
            File dst = new File(dstDirectory, targetDir);
            logger.log(Level.FINE, "copyFiles: {0} ---> {1}", new Object[]{src, dst});
            FileUtils.copyFileToDirectory(src, dst);
        }
    }

    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }

    public static File getTempDirectory() {
        return new File(getTempDirectoryPath());
    }

    public static String separatorsToUnix(String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }

    public static void delete(final File path, final long timeout) throws InterruptedException {
        final CountDownLatch timer = new CountDownLatch(1);
        Thread worker = new Thread() {
            @Override
            public void run() {
                try {
                    for (; ; ) {
                        if (path.delete()) {
                            timer.countDown();
                            break;
                        } else {
                            logger.log(Level.SEVERE, "Delete did not succeded for {0}", path.toString());
                            Thread.sleep(timeout * 10); // 100 tries
                        }


                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        };
        worker.start();
        timer.await(timeout, TimeUnit.SECONDS);
        if (timer.getCount() > 0) {
            worker.interrupt();
            throw new RuntimeException(String.format("Delete of %s failed after %d secs!", path.toString(), timeout));
        }
    }
}
