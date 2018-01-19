/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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
 * Provides an instance.
 * <p>
 * Method {@link #isApplicable(Class)} is called first to check whether the provider is able to provide the given
 * {@link Class}.  Method {@link #create(Class)} is called to get the instance.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class ComponentProvider {
    /**
     * Checks whether this component provider is able to provide an instance of given {@link Class}.
     *
     * @param c {@link Class} to be checked.
     * @return {@code true} iff this {@link ComponentProvider} is able to create an instance of the given {@link Class}.
     */
    public abstract boolean isApplicable(Class<?> c);

    /**
     * Create new instance.
     *
     * @param c   {@link Class} to be created.
     * @param <T> type of the created object.
     * @return instance, iff found, {@code null} otherwise.
     */
    public abstract <T> Object create(Class<T> c);

    /**
     * Get the method which should be invoked instead provided one.
     * <p>
     * Useful mainly for EJB container support, where methods from endpoint class cannot be invoked directly - Tyrus
     * needs
     * to use method declared on remote interface.
     * <p>
     * Default implementation returns method provided as parameter.
     *
     * @param method method from endpoint class.
     * @return method which should be invoked.
     */
    public Method getInvocableMethod(Method method) {
        return method;
    }

    /**
     * Destroys the given managed instance.
     *
     * @param o instance to be destroyed.
     * @return <code>true</code> iff the instance was coupled to this {@link ComponentProvider}, false otherwise.
     */
    public abstract boolean destroy(Object o);
}
