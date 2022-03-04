/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Supplier;

final class Javassistant {
    static boolean isEligibleForAssistance(Object object) {
        try {
            Class<?> objectClass = object.getClass();
            int modifiers = objectClass.getModifiers();
            if (objectClass.isLocalClass()
                    || (objectClass.isMemberClass() && !Modifier.isStatic(modifiers))
                    || Modifier.isAbstract(modifiers)) {
                return false;
            }
            Constructor constructor = objectClass.getConstructor();
            return true;
        } catch (Exception e) {
            // return false
        }
        return false;
    }

    /**
     * A package protected method that does not expose Javassist classes
     */
    static <T> T assistGetUserProperties(T toAssist, Supplier<Map<String, Object>> supplier) throws Exception {
        return assist(toAssist, GET_USER_PROPERTIES_FILTER, (self, thisMethod, proceed, args) -> supplier.get());
    }

    private static <T> T assist(T toAssist, MethodFilter methodFilter, MethodHandler methodHandler) throws Exception {
        T assisted = null;
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(toAssist.getClass());
        factory.setFilter(methodFilter);

        assisted = (T) (factory.create(new Class<?>[0], new Object[0], methodHandler));
        return assisted;
    }

    private static class GetUserPropertiesFilter implements MethodFilter {
        @Override
        public boolean isHandled(Method method) {
            return method.getName().equals("getUserProperties") & method.getParameterCount() == 0;
        }
    }

    private static final GetUserPropertiesFilter GET_USER_PROPERTIES_FILTER = new GetUserPropertiesFilter();
}
