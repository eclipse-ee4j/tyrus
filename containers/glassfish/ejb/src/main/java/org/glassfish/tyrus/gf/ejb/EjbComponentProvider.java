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

package org.glassfish.tyrus.gf.ejb;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.tyrus.core.ComponentProvider;

/**
 * Provides the instance for the supported EJB classes.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EjbComponentProvider extends ComponentProvider {

    private static final Logger LOGGER = Logger.getLogger(EjbComponentProvider.class.getName());

    @SuppressWarnings("unchecked")
    @Override
    public <T> Object create(Class<T> c) {
        String name = getName(c);
        T result = null;
        if (name == null) {
            return null;
        }

        try {
            InitialContext ic = new InitialContext();
            result = (T) lookup(ic, c, name);
        } catch (NamingException ex) {
            String message = "An instance of EJB class " + c.getName()
                    + " could not be looked up using simple form name or the fully-qualified form name.";
            LOGGER.log(Level.SEVERE, message, ex);
        }

        return result;
    }

    @Override
    public boolean isApplicable(Class<?> c) {
        return (c.isAnnotationPresent(Singleton.class)
                || c.isAnnotationPresent(Stateful.class)
                || c.isAnnotationPresent(Stateless.class));
    }

    @Override
    public boolean destroy(Object o) {
        return false;
    }

    @Override
    public Method getInvocableMethod(Method method) {
        final Class<?> declaringClass = method.getDeclaringClass();

        final List<Class> interfaces = new LinkedList<Class>();
        if (declaringClass.isAnnotationPresent(Remote.class)) {
            interfaces.addAll(Arrays.asList(declaringClass.getAnnotation(Remote.class).value()));
        }
        if (declaringClass.isAnnotationPresent(Local.class)) {
            interfaces.addAll(Arrays.asList(declaringClass.getAnnotation(Local.class).value()));
        }
        for (Class<?> i : declaringClass.getInterfaces()) {
            if (i.isAnnotationPresent(Remote.class) || i.isAnnotationPresent(Local.class)) {
                interfaces.add(i);
            }
        }

        for (Class iface : interfaces) {
            try {
                final Method interfaceMethod = iface.getDeclaredMethod(method.getName(), method.getParameterTypes());
                if (interfaceMethod != null) {
                    return interfaceMethod;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return method;
    }

    private String getName(Class<?> c) {
        String name;

        if (c.isAnnotationPresent(Singleton.class)) {
            name = c.getAnnotation(Singleton.class).name();
        } else if (c.isAnnotationPresent(Stateful.class)) {
            name = c.getAnnotation(Stateful.class).name();
        } else if (c.isAnnotationPresent(Stateless.class)) {
            name = c.getAnnotation(Stateless.class).name();
        } else {
            return null;
        }

        if (name == null || name.length() == 0) {
            name = c.getSimpleName();
        }
        return name;
    }

    private Object lookup(InitialContext ic, Class<?> c, String name) throws NamingException {
        try {
            return lookupSimpleForm(ic, name);
        } catch (NamingException ex) {
            LOGGER.log(Level.WARNING, "An instance of EJB class " + c.getName()
                    + " could not be looked up using simple form name. "
                    + "Attempting to look up using the fully-qualified form name.", ex);

            return lookupFullyQualfiedForm(ic, c, name);
        }
    }

    private Object lookupSimpleForm(InitialContext ic, String name) throws NamingException {
        String jndiName = "java:module/" + name;
        return ic.lookup(jndiName);
    }

    private Object lookupFullyQualfiedForm(InitialContext ic, Class<?> c, String name) throws NamingException {
        String jndiName = "java:module/" + name + "!" + c.getName();
        return ic.lookup(jndiName);
    }

}
