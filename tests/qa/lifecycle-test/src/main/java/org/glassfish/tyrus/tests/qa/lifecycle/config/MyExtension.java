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

package org.glassfish.tyrus.tests.qa.lifecycle.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.websocket.Extension;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class MyExtension implements Extension {

    static List<Extension> extensions = new CopyOnWriteArrayList<Extension>();
    String name;
    Map<String, String> map;

    public MyExtension(String name, Map map) {
        this.name = name;
        this.map = map;
    }

    public MyExtension(String name, String param, String value) {
        Map params = new HashMap();
        params.put(param, value);
        this.map = params;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Extension.Parameter> getParameters() {
        List<Extension.Parameter> params = new ArrayList<Extension.Parameter>();

        for (final String name : map.keySet()) {
            final String val = map.get(name);

            params.add(new Extension.Parameter() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getValue() {
                    return val;
                }
            });
        }

        return params;
    }

    public static List<Extension> initExtensions() {
        for (int i = 0; i < 100; i++) {
            extensions.add(
                    new MyExtension(
                            "mikcext" + i,
                            "mikcparam" + i,
                            "mikcval" + i));
        }
        return extensions;
    }
}



