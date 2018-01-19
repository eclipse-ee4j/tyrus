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

package org.glassfish.tyrus.core.coder;

import javax.websocket.Decoder;
import javax.websocket.Encoder;

/**
 * Wrapper of coders storing the coder coder class (and optionally coder instance), return type of the encode / decode
 * method and coder class.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class CoderWrapper<T> extends CoderAdapter implements Decoder, Encoder {

    private final Class<? extends T> coderClass;
    private final T coder;

    /**
     * Return type of the encode / decode method.
     */
    private final Class<?> type;

    /**
     * Construct new coder wrapper.
     *
     * @param coderClass coder class.
     * @param type       return type provided by the encode / decode method. Cannot be {@code null}.
     */
    public CoderWrapper(Class<? extends T> coderClass, Class<?> type) {
        this.coderClass = coderClass;
        this.coder = null;
        this.type = type;
    }

    /**
     * Construct new coder wrapper.
     *
     * @param coder cannot be {@code null}.
     * @param type  return type provided by the encode / decode method. Cannot be {@code null}.
     */
    public CoderWrapper(T coder, Class<?> type) {
        this.coder = coder;
        this.coderClass = (Class<T>) coder.getClass();
        this.type = type;
    }

    /**
     * Get the return type of the encode / decode method.
     *
     * @return return type of the encode / decode method.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Get coder class.
     *
     * @return coder class.
     */
    public Class<? extends T> getCoderClass() {
        return coderClass;
    }

    /**
     * Get coder instance.
     *
     * @return coder instance. {@code null} if registered using coder class.
     */
    public T getCoder() {
        return coder;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CoderWrapper");
        sb.append("{coderClass=").append(coderClass);
        sb.append(", coder=").append(coder);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
