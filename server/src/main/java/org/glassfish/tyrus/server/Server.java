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

package org.glassfish.tyrus.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.spi.ServerContainer;
import org.glassfish.tyrus.spi.ServerContainerFactory;

/**
 * Implementation of the WebSocket Server.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class Server {

    /**
     * Path to static content to be served by created Server instance.
     * <p>
     * Value have to be non-empty {@link String} and should represent content root of static content (file system
     * path).
     *
     * @see Server#Server(java.util.Map, Class[])
     * @see Server#Server(String, int, String, java.util.Map, Class[])
     * @see Server#Server(String, int, String, java.util.Map, java.util.Set)
     */
    public static final String STATIC_CONTENT_ROOT = "org.glassfish.tyrus.server.staticContentRoot";

    private static final Logger LOGGER = Logger.getLogger(Server.class.getClass().getName());
    private static final int DEFAULT_PORT = 8025;
    private static final String DEFAULT_HOST_NAME = "localhost";
    private static final String DEFAULT_CONTEXT_PATH = "/";

    private final Map<String, Object> properties;
    private final Set<Class<?>> configuration;
    private final String hostName;
    private volatile int port;
    private final String contextPath;

    private ServerContainer server;

    /**
     * Create new server instance.
     *
     * @param configuration to be registered with the server. Classes annotated with {@link
     *                      javax.websocket.server.ServerEndpoint}, implementing
     *                      {@link javax.websocket.server.ServerApplicationConfig} or extending {@link
     *                      javax.websocket.server.ServerEndpointConfig}
     *                      are supported.
     */
    public Server(Class<?>... configuration) {
        this(null, 0, null, null, configuration);
    }

    /**
     * Create new server instance.
     *
     * @param properties    properties used as a parameter to {@link ServerContainerFactory#createServerContainer
     *                      (java.util.Map)} call.
     * @param configuration to be registered with the server. Classes annotated with {@link
     *                      javax.websocket.server.ServerEndpoint}, implementing {@link
     *                      javax.websocket.server.ServerApplicationConfig} or extending {@link
     *                      javax.websocket.server.ServerEndpointConfig}
     *                      are supported.
     */
    public Server(Map<String, Object> properties, Class<?>... configuration) {
        this(null, 0, null, properties, configuration);
    }

    /**
     * Construct new server.
     *
     * @param hostName      hostName of the server.
     * @param port          port of the server. When provided value is {@code 0}, default port ({@value #DEFAULT_PORT})
     *                      will be used, when {@code -1}, ephemeral port number will be used.
     * @param contextPath   root path to the server App.
     * @param properties    properties used as a parameter to {@link ServerContainerFactory#createServerContainer
     *                      (java.util.Map)} call.
     * @param configuration to be registered with the server. Classes annotated with {@link
     *                      javax.websocket.server.ServerEndpoint}, implementing
     *                      {@link javax.websocket.server.ServerApplicationConfig} or extending {@link
     *                      javax.websocket.server.ServerEndpointConfig}
     *                      are supported.
     * @see #getPort()
     */
    public Server(String hostName, int port, String contextPath, Map<String, Object> properties,
                  Class<?>... configuration) {
        this(hostName, port, contextPath, properties, new HashSet<Class<?>>(Arrays.asList(configuration)));
    }

    /**
     * Construct new server.
     *
     * @param hostName      hostName of the server.
     * @param port          port of the server. When provided value is {@code 0}, default port ({@value #DEFAULT_PORT})
     *                      will be used, when {@code -1}, ephemeral port number will be used.
     * @param contextPath   root path to the server App.
     * @param properties    properties used as a parameter to {@link ServerContainerFactory#createServerContainer
     *                      (java.util.Map)} call.
     * @param configuration to be registered with the server. Classes annotated with {@link
     *                      javax.websocket.server.ServerEndpoint}, implementing {@link
     *                      javax.websocket.server.ServerApplicationConfig}
     *                      or extending {@link javax.websocket.server.ServerEndpointConfig}
     *                      are supported.
     * @see #getPort()
     */
    public Server(String hostName, int port, String contextPath, Map<String, Object> properties,
                  Set<Class<?>> configuration) {
        this.hostName = hostName == null ? DEFAULT_HOST_NAME : hostName;
        if (port == 0) {
            this.port = DEFAULT_PORT;
        } else if (port == -1) {
            // OS selected (ephemeral) port.
            this.port = 0;
        } else {
            this.port = port;
        }
        this.contextPath = contextPath == null ? DEFAULT_CONTEXT_PATH : contextPath;
        this.configuration = configuration;
        this.properties = properties == null ? null : new HashMap<String, Object>(properties);
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println(
                    "Please provide: (<hostname>, <port>, <websockets root path>, <;-sep fully qualfied classnames of"
                            + " your bean>) in the command line");
            System.out.println("e.g. localhost 8021 /websockets/myapp myapp.Bean1;myapp.Bean2");
            System.exit(1);
        }
        Set<Class<?>> beanClasses = getClassesFromString(args[3]);
        int port = Integer.parseInt(args[1]);
        String hostname = args[0];
        String wsroot = args[2];

        Server server = new Server(hostname, port, wsroot, null, beanClasses);

        try {
            server.start();
            System.out.println("Press any key to stop the WebSocket server...");
            //noinspection ResultOfMethodCallIgnored
            System.in.read();
        } catch (IOException ioe) {
            System.err.println("IOException during server run");
            ioe.printStackTrace();
        } catch (DeploymentException de) {
            de.printStackTrace();
        } finally {
            server.stop();
        }
    }

    private static Set<Class<?>> getClassesFromString(String rawString) {
        Set<Class<?>> beanClasses = new HashSet<Class<?>>();
        StringTokenizer st = new StringTokenizer(rawString, ";");
        while (st.hasMoreTokens()) {
            String nextClassname = st.nextToken().trim();
            if (!"".equals(nextClassname)) {
                try {
                    beanClasses.add(Class.forName(nextClassname));
                } catch (ClassNotFoundException cnfe) {
                    throw new RuntimeException("Stop: cannot load class: " + nextClassname);
                }
            }
        }
        return beanClasses;
    }

    /**
     * Start the server.
     */
    public void start() throws DeploymentException {
        try {
            if (server == null) {
                server = ServerContainerFactory.createServerContainer(properties);

                for (Class<?> clazz : configuration) {
                    server.addEndpoint(clazz);
                }

                server.start(contextPath, port);

                if (server instanceof TyrusServerContainer) {
                    this.port = ((TyrusServerContainer) server).getPort();
                }

                LOGGER.info("WebSocket Registered apps: URLs all start with ws://" + this.hostName + ":" + getPort());
                LOGGER.info("WebSocket server started.");

            }
        } catch (IOException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    /**
     * Get the port which was used to start the container.
     *
     * @return the port which was used to start the container.
     */
    public int getPort() {
        return port;
    }

    /**
     * Stop the server.
     */
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
            LOGGER.info("Websocket Server stopped.");
        }
    }
}
