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

package org.glassfish.tyrus.tests.qa.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class AppConfig {

    private static final Logger logger = Logger.getLogger(AppConfig.class.getCanonicalName());

    public enum AppServer {
        TYRUS,
        GLASSFISH
    }

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 8025;
    private String contextPath;
    private String endpointPath;
    private int port;
    private String host;
    private String installRoot;
    private int commPort;
    private String commHost;
    private String commScheme;

    public AppConfig(String contextPath, String endpointPath, String commScheme, String commHost, int commPort,
                     String installRoot) {
        setContextPath(contextPath);
        setEndpointPath(endpointPath);
        setCommHost(commHost);
        setCommPort(commPort);
        setCommScheme(commScheme);
        setInstallRoot(installRoot);
    }

    public final void setInstallRoot(String installRoot) {
        this.installRoot = installRoot;
    }

    public String getInstallRoot() {
        final String gfInstallRoot = System.getProperty("glassfish.installRoot");
        if (gfInstallRoot != null) {
            return gfInstallRoot;
        }
        return installRoot;
    }

    public int getCommPort() {
        return commPort;
    }

    public String getCommHost() {
        return commHost;
    }

    public String getCommScheme() {
        return commScheme;
    }

    public final void setCommPort(int commPort) {
        this.commPort = commPort;
    }

    public final void setCommHost(String commHost) {
        this.commHost = commHost;
    }

    public final void setCommScheme(String commScheme) {
        this.commScheme = commScheme;
    }

    public String getContextPath() {
        return contextPath;
    }

    public final void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public final void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public URI getURI() {
        try {
            return new URI("ws", null, getHost(), getPort(), getContextPath() + getEndpointPath(), null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static AppServer getWebSocketContainer() {
        Map<String, String> runtimeEnv = System.getenv();
        Properties props = System.getProperties();
        if (props != null && props.containsKey("com.sun.aas.installRoot")) {
            return AppServer.GLASSFISH;
        }
        //props.list(System.out);
        String container = System.getProperty("websocket.container");
        //if(container==null) {
        //    container = System.getenv("WEBSOCKET_CONTAINER");
        //}
        logger.log(Level.INFO, "getWebSocketContainer(): {0}", container);
        //logger.log(Level.INFO, "Environment: {0}", runtimeEnv);
        if (container != null && container.equals("glassfish")) {
            return AppServer.GLASSFISH;
        }
        return AppServer.TYRUS;
    }

    public static boolean isGlassFishContainer() {
        return getWebSocketContainer().equals(AppConfig.AppServer.GLASSFISH);
    }

    public static boolean isTyrusContainer() {
        return !isGlassFishContainer();
    }

    public String getHost() {
        final String host = System.getProperty("tyrus.test.host");
        if (host != null) {
            return host;
        }
        return DEFAULT_HOST;
    }

    public int getPort() {
        final String port = System.getProperty("tyrus.test.port");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException nfe) {
                // do nothing
            }
        }
        return DEFAULT_PORT;
    }
}
