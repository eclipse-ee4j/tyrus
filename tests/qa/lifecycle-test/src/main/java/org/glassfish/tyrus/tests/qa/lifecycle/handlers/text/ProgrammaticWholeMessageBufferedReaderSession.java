/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa.lifecycle.handlers.text;

import java.io.Reader;

import javax.websocket.MessageHandler;

import org.glassfish.tyrus.tests.qa.lifecycle.ProgrammaticWholeMessageEndpoint;
import org.glassfish.tyrus.tests.qa.lifecycle.handlers.BufferedReaderSessionImpl;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class ProgrammaticWholeMessageBufferedReaderSession extends ProgrammaticWholeMessageEndpoint<Reader>
        implements MessageHandler.Whole<Reader> {

    @Override
    public void createLifeCycle() {
        lifeCycle = new BufferedReaderSessionImpl(1024);
    }

}
