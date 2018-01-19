/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link org.glassfish.tyrus.core.Utils} properties methods.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class UtilsPropertiesTest {

    private static final Map<String, Object> properties = new HashMap<String, Object>();

    static {
        properties.put("Integer", 1);
        properties.put("IntegerAsString", "1");
        properties.put("Boolean", true);
        properties.put("BooleanAsString", "true");
        properties.put("Long", (long) 1);
        properties.put("LongAsString", "1");
        properties.put("SomeString", "Some string");
    }

    @Test
    public void testPropertiesGetInteger() {
        assertEquals(properties.get("Integer"), Utils.getProperty(properties, "Integer", Integer.class));
        assertEquals(properties.get("Integer"), Utils.getProperty(properties, "IntegerAsString", Integer.class));
        assertEquals(properties.get("Integer"), Utils.getProperty(properties, "Long", Integer.class));
        assertEquals(properties.get("Integer"), Utils.getProperty(properties, "LongAsString", Integer.class));
    }

    @Test
    public void testPropertiesGetLong() {
        assertEquals(properties.get("Long"), Utils.getProperty(properties, "Long", Long.class));
        assertEquals(properties.get("Long"), Utils.getProperty(properties, "LongAsString", Long.class));
        assertEquals(properties.get("Long"), Utils.getProperty(properties, "Integer", Long.class));
        assertEquals(properties.get("Long"), Utils.getProperty(properties, "IntegerAsString", Long.class));
    }

    @Test
    public void testPropertiesGetBoolean() {
        assertEquals(properties.get("Boolean"), Utils.getProperty(properties, "Boolean", Boolean.class));
        assertEquals(properties.get("Boolean"), Utils.getProperty(properties, "BooleanAsString", Boolean.class));
        assertEquals(properties.get("Boolean"), Utils.getProperty(properties, "Integer", Boolean.class));
        assertEquals(properties.get("Boolean"), Utils.getProperty(properties, "IntegerAsString", Boolean.class));
    }

    @Test
    public void testUnassignableValues() {
        assertNull(Utils.getProperty(properties, "SomeString", Integer.class));
        assertNull(Utils.getProperty(properties, "SomeString", Long.class));
        assertNull(Utils.getProperty(properties, "SomeString", UtilsPropertiesTest.class));
    }

}
