/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * Copyright (c) 2013, 2025 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.tyrus.core.ComponentProvider;

/**
 * Provides the instance for CDI class.
 *
 * @author Stepan Kopriva
 */
public class CdiComponentProvider extends ComponentProvider {

    private final BeanManager beanManager;

    private static final Logger LOGGER = Logger.getLogger(CdiComponentProvider.class.getName());

    private final boolean managerRetrieved;

    private static final Map<Object, CdiInjectionContext> cdiBeanToContext = new ConcurrentHashMap<Object, CdiInjectionContext>();

    /**
     * Constructor.
     * <p>
     * Looks up the {@link BeanManager} which is later used to provide the instance.
     *
     * @throws javax.naming.NamingException when Bean Manager cannot be looked up.
     */
    public CdiComponentProvider() throws NamingException {
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

    /**
     * Constructor used for testing, which avoids the JNDI lookup of the {@link BeanManager}.
     *
     * @param beanManager the bean manager to use, or {@code null} to simulate an environment without CDI.
     */
    CdiComponentProvider(BeanManager beanManager) {
        this.beanManager = beanManager;
        this.managerRetrieved = (beanManager != null);
    }

    @Override
    public boolean isApplicable(Class<?> c) {
        Annotation[] annotations = c.getAnnotations();

        for (Annotation annotation : annotations) {
            String annotationClassName = annotation.annotationType().getCanonicalName();
            if (annotationClassName.equals("jakarta.ejb.Singleton")
                    || annotationClassName.equals("jakarta.ejb.Stateful")
                    || annotationClassName.equals("jakarta.ejb.Stateless")) {
                return false;
            }
        }

        return managerRetrieved;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Object create(Class<T> c) {
        if (!managerRetrieved) {
            return null;
        }

        // If the endpoint is a managed bean with a normal scope (e.g.
        // @ApplicationScoped, @SessionScoped) or @Singleton, return the shared
        // contextual reference so that WebSocket lifecycle callbacks, injected
        // collaborators and CDI event observers all operate on the same bean
        // instance. Producing a fresh instance per connection (as done below for
        // dependent beans) would otherwise desynchronize the endpoint state from
        // the contextual bean seen by observers and other injection points.
        // See https://github.com/eclipse-ee4j/tyrus/issues/961
        Bean<?> bean = resolveBean(c);
        if (bean != null && isShared(bean)) {
            // Not registered in cdiBeanToContext on purpose: a shared bean must
            // not be destroyed when a single connection is closed - its lifecycle
            // is managed by the CDI container.
            CreationalContext<?> cc = beanManager.createCreationalContext(bean);
            return beanManager.getReference(bean, c, cc);
        }

        // Dependent-scoped (or non-bean) endpoints keep the per-connection
        // instance and are cleaned up by destroy() when the connection closes.
        T managedObject;
        AnnotatedType annotatedType = beanManager.createAnnotatedType(c);
        InjectionTargetFactory<T> injectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
        InjectionTarget<T> it = injectionTargetFactory.createInjectionTarget(null);
        CreationalContext cc = beanManager.createCreationalContext(null);
        managedObject = (T) it.produce(cc);
        it.inject(managedObject, cc);
        it.postConstruct(managedObject);
        cdiBeanToContext.put(managedObject, new CdiInjectionContext(it, cc));

        return managedObject;
    }

    /**
     * Resolves the unique managed {@link Bean} for the given endpoint class, or {@code null} when the class is not a
     * managed bean or cannot be unambiguously resolved (in which case a per-connection instance is created instead).
     */
    private Bean<?> resolveBean(Class<?> c) {
        try {
            return beanManager.resolve(beanManager.getBeans(c));
        } catch (Exception e) {
            // e.g. AmbiguousResolutionException - fall back to a per-connection instance
            LOGGER.fine(e.getMessage());
            return null;
        }
    }

    /**
     * @return {@code true} if instances of the given bean are shared, i.e. the bean has a normal scope (such as
     * {@code @ApplicationScoped} or {@code @SessionScoped}) or is a {@code @Singleton}.
     */
    private boolean isShared(Bean<?> bean) {
        Class<? extends Annotation> scope = bean.getScope();
        return beanManager.isNormalScope(scope) || Singleton.class.equals(scope);
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
