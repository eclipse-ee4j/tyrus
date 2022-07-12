/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.collection;

/**
 * A collection of {@link Value Value provider} factory &amp; utility methods.
 *
 * @author Marek Potociar
 */
public final class Values {

    private static final LazyValue EMPTY = new LazyValue() {
        @Override
        public Object get() {
            return null;
        }

        @Override
        public boolean isInitialized() {
            return true;
        }
    };

    private Values() {
        // prevents instantiation.
    }

    /**
     * Get an empty {@link Value value provider} whose {@link Value#get() get()}
     * method always returns {@code null}.
     *
     * @param <T> value type.
     * @return empty value provider.
     */
    public static <T> Value<T> empty() {
        //noinspection unchecked
        return (Value<T>) EMPTY;
    }

    /**
     * <p>
     * Get a new constant {@link Value value provider} whose {@link Value#get() get()}
     * method always returns the instance supplied to the {@code value} parameter.
     * </p>
     * <p>
     * In case the supplied value constant is {@code null}, an {@link #empty() empty} value
     * provider is returned.
     * </p>
     * @param <T>   value type.
     * @param value value instance to be provided.
     * @return constant value provider.
     */
    public static <T> Value<T> of(final T value) {
        return (value == null) ? Values.<T>empty() : new InstanceValue<T>(value);
    }

    private static class InstanceValue<T> implements Value<T> {

        private final T value;

        public InstanceValue(final T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return value.equals(((InstanceValue) o).value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "InstanceValue{value=" + value + '}';
        }
    }

    /**
     * Get a new lazily initialized {@link Value value provider}.
     * <p>
     * The value returned by its {@link Value#get() get()} method is lazily retrieved during a first
     * call to the method from the supplied {@code delegate} value provider and is then cached for
     * a subsequent retrieval.
     * </p>
     * <p>
     * The implementation of the returned lazy value provider is thread-safe and is guaranteed to
     * invoke the {@code get()} method on the supplied {@code delegate} value provider instance at
     * most once.
     * </p>
     * <p>
     * If the supplied value provider is {@code null}, an {@link #empty() empty} value
     * provider is returned.
     * </p>
     *
     * @param <T>      value type.
     * @param delegate value provider delegate that will be used to lazily initialize the value provider.
     * @return lazily initialized value provider.
     */
    public static <T> LazyValue<T> lazy(final Value<T> delegate) {
        //noinspection unchecked
        return (delegate == null) ? (LazyValue<T>) EMPTY : new LazyValueImpl<T>(delegate);
    }

    /**
     * Get a new eagerly initialized {@link Value value provider}.
     * <p>
     * The value returned by its {@link Value#get() get()} method is eagerly computed from the supplied
     * {@code delegate} value provider and is then stored in a final field for a subsequent retrieval.
     * </p>
     * <p>
     * The implementation of the returned eager value provider is thread-safe and is guaranteed to
     * invoke the {@code get()} method on the supplied {@code delegate} value provider instance once
     * and only once.
     * </p>
     * <p>
     * If the supplied value provider is {@code null}, an {@link #empty() empty} value
     * provider is returned.
     * </p>
     *
     * @param <T>      value type.
     * @param delegate value provider delegate that will be used to eagerly initialize the value provider.
     * @return eagerly initialized, constant value provider.
     */
    public static <T> Value<T> eager(final Value<T> delegate) {
        return (delegate == null) ? Values.<T>empty() : new EagerValue<T>(delegate);
    }

    private static class EagerValue<T> implements Value<T> {

        private final T result;

        private EagerValue(final Value<T> value) {
            this.result = value.get();
        }

        @Override
        public T get() {
            return result;
        }
    }

    private static class LazyValueImpl<T> implements LazyValue<T> {

        private final Object lock;
        private final Value<T> delegate;

        private volatile Value<T> value;

        public LazyValueImpl(final Value<T> delegate) {
            this.delegate = delegate;
            this.lock = new Object();
        }

        @Override
        public T get() {
            Value<T> result = value;
            if (result == null) {
                synchronized (lock) {
                    result = value;
                    if (result == null) {
                        value = result = Values.of(delegate.get());
                    }
                }
            }
            return result.get();
        }

        @Override
        public boolean isInitialized() {
            return value != null;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return delegate.equals(((LazyValueImpl) o).delegate);
        }

        @Override
        public int hashCode() {
            return delegate != null ? delegate.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "LazyValue{delegate=" + delegate.toString() + '}';
        }
    }

}
