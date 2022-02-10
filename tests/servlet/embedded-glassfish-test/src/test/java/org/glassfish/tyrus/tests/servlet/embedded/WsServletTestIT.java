/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.embedded;

import jakarta.websocket.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * Unsecured (not using SSL) run of tests in {@link ServletTestBase}
 */
@ExtendWith(ArquillianExtension.class)
public class WsServletTestIT extends ServletTestBase {

    private static final String CONTEXT_PATH = "servlet-test";

    public WsServletTestIT() {
        setContextPath(CONTEXT_PATH);
    }

    @Override
    protected String getScheme() {
        return "ws";
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws IOException {
        return ServletTestBase.createDeployment(
                "glassfish-web.xml",
                CONTEXT_PATH,
                OnOpenCloseEndpoint.class, PlainEchoEndpoint.class,
                DispatchingServletFilter.class,
                DispatchingServletFilter.OP.class,
                DispatchingServletFilter.ProgramaticEndpoint.class,
                DispatchingServletFilter.ProgramaticEndpoint.ProgramaticEndpointMessageHandler.class,
                DispatchingServletProgrammaticEndpointConfig.class,
                DispatchingServletProgrammaticEndpointConfig.ProgrammaticEndpointConfigurator.class
                );
    }

    @Test
    public void testPlainEchoShort() throws DeploymentException, IOException, InterruptedException {
        super.testPlainEchoShort();
    }

    @Test
    public void testAddEndpointTestUpgradeHttpToWebSocket() throws DeploymentException, InterruptedException, IOException {
        super.testAddEndpoint();
        super.testUpgradeHttpToWebSocket();
    }
}


