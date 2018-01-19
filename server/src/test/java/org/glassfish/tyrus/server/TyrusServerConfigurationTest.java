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

package org.glassfish.tyrus.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests correct construction of {@link TyrusServerConfiguration} from scanned classes.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TyrusServerConfigurationTest {

    @Test
    public void testNoServerApplicationConfig() {
        Set<Class<?>> scanned = new HashSet<Class<?>>() {
            {
                add(AnnotatedA.class);
                add(AnnotatedB.class);
                add(ProgrammaticConfA.class);
                add(ProgrammaticConfB.class);
                add(Other.class);
            }
        };

        TyrusServerConfiguration configuration = new TyrusServerConfiguration(scanned, Collections
                .<ServerEndpointConfig>emptySet());

        Assert.assertEquals(2, configuration.getAnnotatedEndpointClasses(null).size());
        Assert.assertTrue(configuration.getAnnotatedEndpointClasses(null).contains(AnnotatedA.class));
        Assert.assertTrue(configuration.getAnnotatedEndpointClasses(null).contains(AnnotatedB.class));

        // TODO XXX FIXME
//        Assert.assertEquals(2, configuration.getEndpointConfigs(null).size());
//        Assert.assertTrue(configuration.getEndpointConfigs(null).contains(ProgrammaticConfA.class));
//        Assert.assertTrue(configuration.getEndpointConfigs(null).contains(ProgrammaticConfB.class));
    }

    @Test
    public void testOneServerApplicationConfig() {
        Set<Class<?>> scanned = new HashSet<Class<?>>() {
            {
                add(ApplicationConfA.class);
                add(ProgrammaticConfB.class);
                add(AnnotatedB.class);
                add(Other.class);
            }
        };

        TyrusServerConfiguration configuration = new TyrusServerConfiguration(scanned, Collections
                .<ServerEndpointConfig>emptySet());

        Assert.assertEquals(1, configuration.getAnnotatedEndpointClasses(null).size());
        Assert.assertTrue(configuration.getAnnotatedEndpointClasses(null).contains(AnnotatedA.class));

        // TODO XXX FIXME
//        Assert.assertEquals(1, configuration.getEndpointConfigs(null).size());
//        Assert.assertTrue(configuration.getEndpointConfigs(null).contains(ProgrammaticConfA.class));
    }

    @Test
    public void testTwoServerApplicationConfig() {
        Set<Class<?>> scanned = new HashSet<Class<?>>() {
            {
                add(ApplicationConfA.class);
                add(ApplicationConfB.class);
                add(AnnotatedC.class);
                add(ProgrammaticC.class);
                add(ProgrammaticConfC.class);
                add(Other.class);
            }
        };

        TyrusServerConfiguration configuration = new TyrusServerConfiguration(scanned, Collections
                .<ServerEndpointConfig>emptySet());

        Assert.assertEquals(1, configuration.getAnnotatedEndpointClasses(null).size());
        Assert.assertTrue(configuration.getAnnotatedEndpointClasses(null).contains(AnnotatedA.class));

        // TODO XXX FIXME
//        Assert.assertEquals(1, configuration.getEndpointConfigs(null).size());
//        Assert.assertTrue(configuration.getEndpointConfigs(null).contains(ProgrammaticConfA.class));
    }

    @ServerEndpoint(value = "/AA")
    public class AnnotatedA {
    }

    @ServerEndpoint(value = "/AB")
    public class AnnotatedB {
    }

    @ServerEndpoint(value = "/AC")
    public class AnnotatedC {
    }

    public class ProgrammaticA extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
        }
    }

    public class ProgrammaticB extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
        }
    }

    public class ProgrammaticC extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
        }
    }

    public static class ProgrammaticConfA extends ServerEndpointConfigAdapter {
        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticA.class;
        }
    }

    public static class ProgrammaticConfB extends ServerEndpointConfigAdapter {
        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticB.class;
        }
    }

    public static class ProgrammaticConfC extends ServerEndpointConfigAdapter {
        @Override
        public Class<?> getEndpointClass() {
            return ProgrammaticC.class;
        }
    }

    public class Other {
    }

    public static class ApplicationConfA implements ServerApplicationConfig {
        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned) {
            return new HashSet<ServerEndpointConfig>() {
                {
                    add(new ProgrammaticConfA());
                }
            };

        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
            return new HashSet<Class<?>>() {
                {
                    add(AnnotatedA.class);
                }
            };
        }
    }

    public static class ApplicationConfB implements ServerApplicationConfig {
        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned) {
            return new HashSet<ServerEndpointConfig>() {
                {
                    add(new ProgrammaticConfA());
                }
            };
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
            return new HashSet<Class<?>>() {
                {
                    add(AnnotatedA.class);
                }
            };
        }
    }
}
