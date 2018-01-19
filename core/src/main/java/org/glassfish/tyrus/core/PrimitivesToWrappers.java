/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
class PrimitivesToWrappers {

    private static final HashMap<Class<?>, Class<?>> conversionMap = new HashMap<Class<?>, Class<?>>();

    static {
        conversionMap.put(int.class, Integer.class);
        conversionMap.put(short.class, Short.class);
        conversionMap.put(long.class, Long.class);
        conversionMap.put(double.class, Double.class);
        conversionMap.put(float.class, Float.class);
        conversionMap.put(boolean.class, Boolean.class);
        conversionMap.put(byte.class, Byte.class);
        conversionMap.put(char.class, Character.class);
        conversionMap.put(void.class, Void.class);
    }

    /**
     * Gets the Boxing class for the primitive type.
     *
     * @param c primitive type
     * @return boxing class if c is primitive type, c otherwise
     */
    public static Class<?> getPrimitiveWrapper(Class<?> c) {
        if (!c.isPrimitive()) {
            return c;
        }

        return conversionMap.containsKey(c) ? conversionMap.get(c) : c;
    }

    /**
     * Checks whether the given {@link Class} is a primitive wrapper class.
     *
     * @param c {@link Class} to be checked.
     * @return {@code true} iff the class is primitive wrapper, {@code false} otherwise.
     */
    public static boolean isPrimitiveWrapper(Class<?> c) {
        return conversionMap.containsValue(c);
    }
}
