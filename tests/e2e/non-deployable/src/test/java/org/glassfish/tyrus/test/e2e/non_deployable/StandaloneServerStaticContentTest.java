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

package org.glassfish.tyrus.test.e2e.non_deployable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URLConnection;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class StandaloneServerStaticContentTest extends TestContainer {

    public StandaloneServerStaticContentTest() {
        getServerProperties().put(Server.STATIC_CONTENT_ROOT, "./");
        setContextPath("/");
    }

    @Test
    public void testStaticContent() throws DeploymentException {
        final Server server = startServer();

        try {
            final URLConnection urlConnection = getURI("/pom.xml", "http").toURL().openConnection();

            urlConnection.connect();

            final BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String s;
            do {
                s = bufferedReader.readLine();
                System.out.println(s);
            } while (bufferedReader.ready());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopServer(server);
        }
    }
}
