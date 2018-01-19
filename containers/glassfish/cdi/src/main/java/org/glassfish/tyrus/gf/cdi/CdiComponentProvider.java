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

package org.glassfish.tyrus.gf.cdi;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.tyrus.core.ComponentProvider;

/**
 * Provides the instance for CDI class.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class CdiComponentProvider extends ComponentProvider {

    private final BeanManager beanManager;

    private static final Logger LOGGER = Logger.getLogger(CdiComponentProvider.class.getName());

    private final boolean managerRetrieved;

    private final Map<Object, CdiInjectionContext> cdiBeanToContext;

    /**
     * Constructor.
     * <p>
     * Looks up the {@link BeanManager} which is later used to provide the instance.
     *
     * @throws javax.naming.NamingException when Bean Manager cannot be looked up.
     */
    public CdiComponentProvider() throws NamingException {
        cdiBeanToContext = new ConcurrentHashMap<Object, CdiInjectionContext>();
        InitialContext ic = new InitialContext();
        BeanManager manager = null;

        try {
            manager = (BeanManager) ic.lookup("java:comp/BeanManager");
        } catch (Exception e) {
            LOGGER.fine(e.getMessage());
        } finally {
            beanManager = manager;
            managerRetrieved = (beanManager != null);
        }
    }

    @Override
    public boolean isApplicable(Class<?> c) {
        Annotation[] annotations = c.getAnnotations();

        for (Annotation annotation : annotations) {
            String annotationClassName = annotation.annotationType().getCanonicalName();
            if (annotationClassName.equals("javax.ejb.Singleton")
                    || annotationClassName.equals("javax.ejb.Stateful")
                    || annotationClassName.equals("javax.ejb.Stateless")) {
                return false;
            }
        }

        return managerRetrieved;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Object create(Class<T> c) {
        if (managerRetrieved) {
            synchronized (beanManager) {
                T managedObject;
                AnnotatedType annotatedType = beanManager.createAnnotatedType(c);
                InjectionTarget it = beanManager.createInjectionTarget(annotatedType);
                CreationalContext cc = beanManager.createCreationalContext(null);
                managedObject = (T) it.produce(cc);
                it.inject(managedObject, cc);
                it.postConstruct(managedObject);
                cdiBeanToContext.put(managedObject, new CdiInjectionContext(it, cc));

                return managedObject;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean destroy(Object o) {
        //if the object is not in map, nothing happens
        if (cdiBeanToContext.containsKey(o)) {
            cdiBeanToContext.get(o).cleanup(o);
            cdiBeanToContext.remove(o);
            return true;
        }

        return false;
    }

    private static class CdiInjectionContext {
        final InjectionTarget it;
        final CreationalContext cc;

        CdiInjectionContext(InjectionTarget it, CreationalContext cc) {
            this.it = it;
            this.cc = cc;
        }

        @SuppressWarnings("unchecked")
        public void cleanup(Object instance) {
            it.preDestroy(instance);
            it.dispose(instance);
            cc.release();
        }
    }
}
