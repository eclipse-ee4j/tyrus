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

package org.glassfish.tyrus.spi;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * Writer class that is used by tyrus runtime to pass outbound websocket data
 * for a connection to a transport. Then, the transport actually writes the
 * data eventually.
 * <p>
 * A transport creates implementation of this class and registers the writer
 * object using {@link WebSocketEngine.UpgradeInfo#createConnection} after
 * a successful upgrade.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class Writer implements Closeable {

    /**
     * Tyrus runtime calls this method to handover the data for a connection
     * to the transport. The transport writes bytes to underlying connection.
     * Tyrus runtime must not use the buffer until the write is completed.
     *
     * @param buffer            bytes to write.
     * @param completionHandler completion handler to know the write status.
     */
    public abstract void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler);
}
