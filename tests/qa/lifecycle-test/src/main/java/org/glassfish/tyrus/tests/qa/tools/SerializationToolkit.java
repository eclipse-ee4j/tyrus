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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import org.glassfish.tyrus.tests.qa.regression.Issue;

/**
 * @author Michal Conos (michal.conos at oracle.com)
 */
public class SerializationToolkit {

    File store;
    private static final Logger logger = Logger.getLogger(SerializationToolkit.class.getCanonicalName());

    public SerializationToolkit() throws IOException {
        this(File.createTempFile("tests", "websockets"));
    }

    public SerializationToolkit(File store) {
        this.store = store;
    }

    public SerializationToolkit(String store) {
        this(new File(store));
    }

    public void save(Object obj) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(store));
            oos.writeObject(obj);
            oos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Object load() {

        Object o = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(store));
            o = ois.readObject();
            ois.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return o;
    }
}
