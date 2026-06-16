/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Tests that {@link CdiComponentProvider#create(Class)} honours the CDI scope of the endpoint bean: shared scopes
 * ({@code @ApplicationScoped}, {@code @Singleton}, ...) must reuse a single contextual instance across connections,
 * while {@code @Dependent} endpoints keep the per-connection instance.
 *
 * @see <a href="https://github.com/eclipse-ee4j/tyrus/issues/961">Tyrus issue 961</a>
 */
public class CdiComponentProviderTest {

    @ApplicationScoped
    public static class ApplicationScopedEndpoint {
    }

    @Singleton
    public static class SingletonEndpoint {
    }

    @Dependent
    public static class DependentEndpoint {
    }

    @Test
    public void applicationScopedEndpointReturnsSharedContextualReference() {
        Object contextualReference = new ApplicationScopedEndpoint();
        CdiComponentProvider provider =
                new CdiComponentProvider(fakeBeanManager(ApplicationScoped.class, true, contextualReference));

        // Every connection must observe the same (shared) bean instance.
        assertSame(contextualReference, provider.create(ApplicationScopedEndpoint.class));
        assertSame(contextualReference, provider.create(ApplicationScopedEndpoint.class));
    }

    @Test
    public void singletonEndpointReturnsSharedContextualReference() {
        Object contextualReference = new SingletonEndpoint();
        // @Singleton is a pseudo-scope, so isNormalScope() returns false; it must still be treated as shared.
        CdiComponentProvider provider =
                new CdiComponentProvider(fakeBeanManager(Singleton.class, false, contextualReference));

        assertSame(contextualReference, provider.create(SingletonEndpoint.class));
        assertSame(contextualReference, provider.create(SingletonEndpoint.class));
    }

    @Test
    public void dependentEndpointReturnsNewInstancePerConnection() {
        CdiComponentProvider provider = new CdiComponentProvider(fakeBeanManager(Dependent.class, false, null));

        Object first = provider.create(DependentEndpoint.class);
        Object second = provider.create(DependentEndpoint.class);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame("@Dependent endpoints must get a fresh instance per connection", first, second);
    }

    /**
     * Builds a minimal {@link BeanManager} that exercises only the methods invoked by
     * {@link CdiComponentProvider#create(Class)}. The shared path returns {@code contextualReference} from
     * {@code getReference()}; the dependent path produces a fresh instance of the requested class on every call.
     */
    private static BeanManager fakeBeanManager(Class<? extends Annotation> scope, boolean normalScope,
                                               Object contextualReference) {
        return (BeanManager) newProxy(BeanManager.class, new FakeHandler(scope, normalScope, contextualReference));
    }

    private static Object newProxy(Class<?> iface, InvocationHandler handler) {
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, handler);
    }

    /**
     * Single {@link InvocationHandler} backing the fake {@link BeanManager} and the nested CDI SPI objects it returns
     * ({@link Bean}, {@link AnnotatedType}, the injection-target factory and target, and the creational context). The
     * method names used by {@code create()} do not collide across those interfaces, so one handler can serve them all.
     */
    private static final class FakeHandler implements InvocationHandler {

        private final Class<? extends Annotation> scope;
        private final boolean normalScope;
        private final Object contextualReference;
        private Class<?> producedType;

        private FakeHandler(Class<? extends Annotation> scope, boolean normalScope, Object contextualReference) {
            this.scope = scope;
            this.normalScope = normalScope;
            this.contextualReference = contextualReference;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                // --- shared-scope resolution path ---
                case "getBeans":
                    return Collections.singleton(newProxy(Bean.class, this));
                case "resolve":
                    return newProxy(Bean.class, this);
                case "getScope":
                    return scope;
                case "isNormalScope":
                    return normalScope;
                case "getReference":
                    return contextualReference;
                // --- per-connection (dependent) injection-target path ---
                case "createAnnotatedType":
                    producedType = (Class<?>) args[0];
                    return newProxy(AnnotatedType.class, this);
                case "getInjectionTargetFactory":
                    return newProxy(jakarta.enterprise.inject.spi.InjectionTargetFactory.class, this);
                case "createInjectionTarget":
                    return newProxy(jakarta.enterprise.inject.spi.InjectionTarget.class, this);
                case "createCreationalContext":
                    return newProxy(jakarta.enterprise.context.spi.CreationalContext.class, this);
                case "produce":
                    return producedType.getDeclaredConstructor().newInstance();
                case "inject":
                case "postConstruct":
                case "preDestroy":
                case "dispose":
                    return null;
                // --- Object methods ---
                case "toString":
                    return "FakeBeanManager";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
            }
        }
    }
}
