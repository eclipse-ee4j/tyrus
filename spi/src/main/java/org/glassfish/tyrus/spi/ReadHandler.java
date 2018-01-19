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

package org.glassfish.tyrus.spi;

import java.nio.ByteBuffer;

/**
 * Read handler provides a way for a transport to pass websocket
 * connection data to tyrus runtime. A transport reads websocket data for an
 * connection and passes the data to tyrus runtime for invoking endpoint.
 * <p>
 * An implementation of this interface is created by tyrus runtime. Once a
 * transport completes a successful upgrade for a connection, the transport
 * can get hold of the handler using {@link Connection#getReadHandler()}.
 */
public interface ReadHandler {

    /**
     * A transport reads websocket data and invokes this method to handover
     * websocket data for a connection to tyrus runtime. The runtime consumes
     * as much data as possible from the byte buffer. If there is some
     * remaining data in the buffer, transport needs pass those bytes in
     * more call (along with more data) in the same byte buffer or in a newer
     * byte buffer.
     *
     * @param data websocket data of a connection.
     */
    void handle(ByteBuffer data);
}
