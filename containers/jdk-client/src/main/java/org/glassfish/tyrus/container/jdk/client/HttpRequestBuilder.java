/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.jdk.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author Petr Janouch
 */
class HttpRequestBuilder {

    private static final String ENCODING = "ISO-8859-1";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String HTTP_VERSION = "HTTP/1.1";

    private static void appendUpgradeHeaders(StringBuilder request, JdkUpgradeRequest upgradeRequest) {
        for (Entry<String, List<String>> header : upgradeRequest.getHeaders().entrySet()) {
            StringBuilder value = new StringBuilder();
            for (String valuePart : header.getValue()) {
                if (value.length() != 0) {
                    value.append(", ");
                }
                value.append(valuePart);
            }
            appendHeader(request, header.getKey(), value.toString());
        }
    }

    private static void appendHeader(StringBuilder request, String key, String value) {
        request.append(key);
        request.append(":");
        request.append(value);
        request.append(LINE_SEPARATOR);
    }

    static ByteBuffer build(JdkUpgradeRequest upgradeRequest) {
        StringBuilder request = new StringBuilder();
        request.append(upgradeRequest.getHttpMethod());
        request.append(" ");
        request.append(upgradeRequest.getRequestUri());
        request.append(" ");
        request.append(HTTP_VERSION);
        request.append(LINE_SEPARATOR);
        appendUpgradeHeaders(request, upgradeRequest);
        request.append(LINE_SEPARATOR);
        String requestStr = request.toString();
        byte[] bytes = requestStr.getBytes(Charset.forName(ENCODING));
        return ByteBuffer.wrap(bytes);
    }
}
