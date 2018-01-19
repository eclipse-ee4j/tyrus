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

import java.lang.reflect.Method;

/**
 * Provides instances using reflection.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class DefaultComponentProvider extends ComponentProvider {

    @Override
    public boolean isApplicable(Class<?> c) {
        return true;
    }

    @Override
    public <T> Object create(Class<T> toLoad) {
        try {
            return ReflectionHelper.getInstance(toLoad);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean destroy(Object o) {
        return false;
    }

    @Override
    public Method getInvocableMethod(Method method) {
        return method;
    }
}
