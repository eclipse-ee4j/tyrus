/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.cdi;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import javax.ejb.Local;
import javax.ejb.Remote;

/**
 * Remote intefrace used with programmatic endpoints MUST declare all methods
 * from {@link javax.websocket.Endpoint}. This is a must because implementation
 * can call any of these methods and declared endpoint must have implementation
 * for all of these methods.
 * <p>
 * Failure to do so would end with an Exception thrown during runtime, because
 * invoked method (from {@link javax.websocket.Endpoint} wouldn't be found in
 * instance provided by EJB container.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Remote
@Local
public interface RemoteServiceProgrammatic {

    public void onOpen(Session session, EndpointConfig config);

    public void onClose(Session session, CloseReason closeReason);

    public void onError(Session session, Throwable thr);
}
