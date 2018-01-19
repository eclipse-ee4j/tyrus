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

import org.glassfish.tyrus.spi.UpgradeRequest;

/**
 * TODO
 */
public enum Version {

    DRAFT17("13") {
        @Override
        public ProtocolHandler createHandler(boolean mask, MaskingKeyGenerator maskingKeyGenerator) {
            return new ProtocolHandler(mask, maskingKeyGenerator);
        }

        @Override
        public boolean validate(UpgradeRequest request) {
            return this.wireProtocolVersion.equals(request.getHeader(UpgradeRequest.SEC_WEBSOCKET_VERSION));
        }
    };

    public abstract ProtocolHandler createHandler(boolean mask, MaskingKeyGenerator maskingKeyGenerator);

    public abstract boolean validate(UpgradeRequest request);

    final String wireProtocolVersion;

    private Version(final String wireProtocolVersion) {
        this.wireProtocolVersion = wireProtocolVersion;
    }

    @Override
    public String toString() {
        return name();
    }

    public static String getSupportedWireProtocolVersions() {
        final StringBuilder sb = new StringBuilder();
        for (Version v : Version.values()) {
            if (v.wireProtocolVersion.length() > 0) {
                sb.append(v.wireProtocolVersion).append(", ");
            }
        }
        return sb.substring(0, sb.length() - 2);
    }
}
