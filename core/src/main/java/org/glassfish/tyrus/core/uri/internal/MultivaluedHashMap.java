/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.uri.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A hash table based implementation of {@link MultivaluedMap} interface.
 * <p>
 * This implementation provides all of the optional map operations. This class
 * makes no guarantees as to the order of the map; in particular, it does not
 * guarantee that the order will remain constant over time. The implementation
 * permits {@code null} key. By default the implementation does also permit
 * {@code null} values, but ignores them. This behavior can be customized
 * by overriding the protected {@link #addNull(List) addNull(...)} and
 * {@link #addFirstNull(List) addFirstNull(...)} methods.
 * <p>
 * This implementation provides constant-time performance for the basic
 * operations (<code>get</code> and <code>put</code>), assuming the hash function
 * disperses the elements properly among the buckets. Iteration over
 * collection views requires time proportional to the "capacity" of the
 * map instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 * <p>
 * An instance of <code>MultivaluedHashMap</code> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>. The <i>capacity</i>
 * is the number of buckets in the hash table, and the initial capacity is simply
 * the capacity at the time the hash table is created. The <i>load factor</i> is
 * a measure of how full the hash table is allowed to get before its capacity is
 * automatically increased. When the number of entries in the hash table exceeds
 * the product of the load factor and the current capacity, the hash table is
 * <i>rehashed</i> (that is, internal data structures are rebuilt) so that the
 * hash table has approximately twice the number of buckets.
 * <p>
 * As a general rule, the default load factor (.75) offers a good tradeoff
 * between time and space costs. Higher values decrease the space overhead
 * but increase the lookup cost (reflected in most of the operations of the
 * <code>HashMap</code> class, including <code>get</code> and <code>put</code>). The
 * expected number of entries in the map and its load factor should be taken
 * into account when setting its initial capacity, so as to minimize the
 * number of rehash operations. If the initial capacity is greater
 * than the maximum number of entries divided by the load factor, no
 * rehash operations will ever occur.
 * <p>
 * If many mappings are to be stored in a <code>MultivaluedHashMap</code> instance,
 * creating it with a sufficiently large capacity will allow the mappings to
 * be stored more efficiently than letting it perform automatic rehashing as
 * needed to grow the table.
 * <p>
 * <strong>Note that this implementation is not guaranteed to be synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally. (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.) This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 * <p>
 * The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <code>remove</code> method, the iterator will throw a {@link java.util.ConcurrentModificationException}.
 * Thus, in the face of concurrent modification, the iterator fails quickly and
 * cleanly, rather than risking arbitrary, non-deterministic behavior at an
 * undetermined time in the future.
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators
 * throw <code>ConcurrentModificationException</code> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * @param <K> the type of keys maintained by this map.
 * @param <V> the type of mapped values.
 * @author Paul Sandoz
 * @author Marek Potociar
 * @since 2.0
 */
public class MultivaluedHashMap<K, V> extends AbstractMultivaluedMap<K, V> implements Serializable {

    private static final long serialVersionUID = -6052320403766368902L;

    /**
     * Constructs an empty multivalued hash map with the default initial capacity
     * ({@code 16}) and the default load factor ({@code 0.75}).
     */
    public MultivaluedHashMap() {
        super(new HashMap<K, List<V>>());
    }

    /**
     * Constructs an empty multivalued hash map with the specified initial
     * capacity and the default load factor ({@code 0.75}).
     *
     * @param initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public MultivaluedHashMap(int initialCapacity) {
        super(new HashMap<K, List<V>>(initialCapacity));
    }

    /**
     * Constructs an empty multivalued hash map with the specified initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *                                  or the load factor is nonpositive
     */
    public MultivaluedHashMap(int initialCapacity, float loadFactor) {
        super(new HashMap<K, List<V>>(initialCapacity, loadFactor));
    }

    /**
     * Constructs a new multivalued hash map with the same mappings as the
     * specified {@link MultivaluedMap }. The {@link List} instances holding
     * the values of each key are created anew instead of being reused.
     *
     * @param map the multivalued map whose mappings are to be placed in this
     *            multivalued map.
     * @throws NullPointerException if the specified map is {@code null}
     */
    public MultivaluedHashMap(MultivaluedMap<? extends K, ? extends V> map) {
        this();
        putAll(map);
    }

    /**
     * This private method is used by the copy constructor to avoid exposing
     * additional generic parameters through the public API documentation.
     *
     * @param <T> any subclass of K
     * @param <U> any subclass of V
     * @param map the map
     */
    private <T extends K, U extends V> void putAll(MultivaluedMap<T, U> map) {
        for (Entry<T, List<U>> e : map.entrySet()) {
            store.put(e.getKey(), new ArrayList<V>(e.getValue()));
        }
    }

    /**
     * Constructs a new multivalued hash map with the same mappings as the
     * specified single-valued {@link Map }.
     *
     * @param map the single-valued map whose mappings are to be placed in this
     *            multivalued map.
     * @throws NullPointerException if the specified map is {@code null}
     */
    public MultivaluedHashMap(Map<? extends K, ? extends V> map) {
        this();
        for (Entry<? extends K, ? extends V> e : map.entrySet()) {
            this.putSingle(e.getKey(), e.getValue());
        }
    }
}
