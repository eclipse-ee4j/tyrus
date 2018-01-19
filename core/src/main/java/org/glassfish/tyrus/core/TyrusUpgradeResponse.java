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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * HTTP response representation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class TyrusUpgradeResponse extends UpgradeResponse {

    private final Map<String, List<String>> headers = new TreeMap<String, List<String>>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    });

    private int status;
    private String reasonPhrase;

    /**
     * Get HTTP status.
     *
     * @return HTTP status.
     */
    @Override
    public int getStatus() {
        return status;
    }

    /**
     * Get HTTP reason phrase.
     *
     * @return reason phrase.
     */
//    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    /**
     * Get HTTP headers.
     *
     * @return HTTP headers.
     */
    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Set HTTP status.
     *
     * @param statusCode status code to be set.
     */
    @Override
    public void setStatus(int statusCode) {
        status = statusCode;
    }

    /**
     * Set HTTP reason phrase.
     *
     * @param reasonPhrase reason phrase to be set.
     */
    @Override
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }
}
