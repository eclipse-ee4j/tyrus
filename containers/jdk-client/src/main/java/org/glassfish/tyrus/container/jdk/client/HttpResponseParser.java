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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;

/**
 * @author Petr Janouch
 */
class HttpResponseParser {

    private static final String ENCODING = "ISO-8859-1";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final int BUFFER_STEP_SIZE = 256;
    // this is package private because of the test
    static final int BUFFER_MAX_SIZE = 16384;

    private volatile boolean complete = false;
    private volatile ByteBuffer buffer;
    private volatile State findEndState = State.INIT;

    HttpResponseParser() {
        buffer = ByteBuffer.allocate(1024);
        buffer.flip(); //buffer created for read
    }

    TyrusUpgradeResponse parseUpgradeResponse() throws ParseException {
        String response = bufferToString();
        String[] tokens = response.split(LINE_SEPARATOR);
        TyrusUpgradeResponse tyrusUpgradeResponse = new TyrusUpgradeResponse();
        parseFirstLine(tokens, tyrusUpgradeResponse);
        List<String> lines = new LinkedList<>();
        lines.addAll(Arrays.asList(tokens).subList(1, tokens.length));
        Map<String, String> headers = parseHeaders(lines);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            final List<String> values = tyrusUpgradeResponse.getHeaders().get(entry.getKey());
            if (values == null) {
                tyrusUpgradeResponse.getHeaders().put(entry.getKey(), Utils.parseHeaderValue(entry.getValue()));
            } else {
                values.addAll(Utils.parseHeaderValue(entry.getValue()));
            }
        }
        return tyrusUpgradeResponse;
    }

    boolean isComplete() {
        return complete;
    }

    void appendData(ByteBuffer data) throws ParseException {
        if (buffer == null) {
            // parser was already destroyed.
            return;
        }

        int responseEndPosition = getEndPosition(data);
        if (responseEndPosition == -1) {
            checkResponseSize(data);
            buffer = Utils.appendBuffers(buffer, data, BUFFER_MAX_SIZE, BUFFER_STEP_SIZE);
            return;
        }

        int limit = data.limit();
        data.limit(responseEndPosition + 1);
        checkResponseSize(data);
        buffer = Utils.appendBuffers(buffer, data, BUFFER_MAX_SIZE, BUFFER_STEP_SIZE);
        data.limit(limit);
        data.position(responseEndPosition + 1);
        complete = true;
    }

    private void checkResponseSize(ByteBuffer partToBeAppended) throws ParseException {
        if (buffer.remaining() + partToBeAppended.remaining() > BUFFER_MAX_SIZE) {
            throw new ParseException(
                    "Upgrade response too big, sizes only up to " + BUFFER_MAX_SIZE + "B are supported.");
        }
    }

    private void parseFirstLine(String[] responseLines, TyrusUpgradeResponse tyrusUpgradeResponse) throws
            ParseException {
        if (responseLines.length == 0) {
            throw new ParseException("Empty HTTP response");
        }
        String firstLine = responseLines[0];
        int versionEndIndex = firstLine.indexOf(' ');
        if (versionEndIndex == -1) {
            throw new ParseException("Unexpected format of the first line of a HTTP response: " + firstLine);
        }
        int statusCodeEndIndex = firstLine.indexOf(' ', versionEndIndex + 1);
        if (statusCodeEndIndex == -1) {
            throw new ParseException("Unexpected format of the first line of a HTTP response: " + firstLine);
        }
        String statusCode = firstLine.substring(versionEndIndex + 1, statusCodeEndIndex);
        String reasonPhrase = firstLine.substring(statusCodeEndIndex + 1);
        int status;
        try {
            status = Integer.parseInt(statusCode);
        } catch (Exception e) {
            throw new ParseException("Invalid format of status code: " + statusCode);
        }
        tyrusUpgradeResponse.setStatus(status);
        tyrusUpgradeResponse.setReasonPhrase(reasonPhrase);

    }

    private Map<String, String> parseHeaders(List<String> headerLines) {
        Map<String, String> headers = new HashMap<>();
        for (String headerLine : headerLines) {
            int separatorIndex = headerLine.indexOf(':');
            if (separatorIndex != -1) {
                String headerKey = headerLine.substring(0, separatorIndex);
                String headerValue = headerLine.substring(separatorIndex + 1);

                if (headers.containsKey(headerKey)) {
                    headers.put(headerKey, headers.get(headerKey) + ", " + headerValue);
                } else {
                    headers.put(headerKey, headerValue);
                }
            }
        }
        return headers;
    }

    private String bufferToString() {
        byte[] bytes = Utils.getRemainingArray(buffer);
        String str;
        try {
            str = new String(bytes, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding" + ENCODING, e);
        }
        return str;
    }

    void destroy() {
        buffer = null;
    }

    void clear() {
        buffer.clear();
        buffer.flip();
        complete = false;
        findEndState = State.INIT;
    }

    private int getEndPosition(ByteBuffer buffer) {
        byte[] bytes = buffer.array();

        for (int i = buffer.position(); i < buffer.limit(); i++) {
            byte b = bytes[i];
            switch (findEndState) {
                case INIT: {
                    if (b == '\r') {
                        findEndState = State.R;
                    }
                    break;
                }
                case R: {
                    if (b == '\n') {
                        findEndState = State.RN;
                    } else {
                        findEndReset(b);
                    }
                    break;
                }
                case RN: {
                    if (b == '\r') {
                        findEndState = State.RNR;
                    } else {
                        findEndState = State.INIT;
                    }
                    break;
                }
                case RNR: {
                    if (b == '\n') {
                        return i;
                    } else {
                        findEndReset(b);
                    }
                    break;
                }
            }
        }
        return -1;
    }

    private void findEndReset(byte b) {
        findEndState = State.INIT;
        if (b == '\r') {
            findEndState = State.R;
        }
    }

    private enum State {
        INIT,
        R,
        RN,
        RNR,
    }
}
