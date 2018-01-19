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

package org.glassfish.tyrus.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation may be used to annotate server endpoints as a optional annotation
 * to {@link javax.websocket.server.ServerEndpoint}. When number of maximal open
 * sessions is exceeded every new attempt to open session is closed with
 * {@link javax.websocket.CloseReason.CloseCodes#TRY_AGAIN_LATER}.
 * If value less then 1 is specified, no limit will be applied.
 * Annotation example:
 * <pre><code>
 * &#64;MaxSessions(100)
 * &#64;ServerEndpoint("/limited-resources")
 * public class LimitedEndpoint {
 * }
 * </code></pre>
 * <p>
 * Maximal number of open sessions can be also specified programmatically
 * using {@link org.glassfish.tyrus.core.TyrusServerEndpointConfig.Builder#maxSessions(int)}.
 * <p>
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MaxSessions {

    /**
     * Maximal number of open sessions.
     *
     * @return maximal number of open sessions.
     */
    public int value();

}
