/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Primarily meant to test servlet integration, might be someday used for simple stress testing.
 * <p/>
 * Tests are executed from descendant classes, which must implement {@link #getScheme()} method. This is used to enable
 * testing with {@code ws} and {@code wss} schemes.
 */
public abstract class ServletTestBase {

    private static final String CONTEXT_PATH = "/servlet-test";
    private static final String defaultHost = "localhost";
    private static final int defaultPort = 8025;
    private String contextPath;

    public ServletTestBase() {
        setContextPath(CONTEXT_PATH);
    }

    protected abstract String getScheme();

    public void testPlainEchoShort() throws DeploymentException, InterruptedException, IOException {
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final String MESSAGE = "Do or do not, there is no try.";

        final ClientManager client = createClient();
        client.connectToServer(new Endpoint() {
                                   @Override
                                   public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                       try {
                                           session.addMessageHandler(new MessageHandler.Whole<String>() {
                                               @Override
                                               public void onMessage(String message) {
                                                   assertEquals(message, MESSAGE);
                                                   messageLatch.countDown();
                                               }
                                           });

                                           session.getBasicRemote().sendText(MESSAGE);
                                       } catch (IOException e) {
                                           // do nothing
                                       }
                                   }
                               }, ClientEndpointConfig.Builder.create().build(),
                               getURI(PlainEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(),
                                      getScheme()));

        messageLatch.await(1, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());
    }

    public void testUpgradeHttpToWebSocket() throws DeploymentException, InterruptedException, IOException {
        final CountDownLatch messageLatch = new CountDownLatch(2);
        final StringBuilder messageBuilder = new StringBuilder();

        final ClientManager client = createClient();
        client.connectToServer(new Endpoint() {
                                   @Override
                                   public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                       try {
                                           session.addMessageHandler(new MessageHandler.Whole<String>() {
                                               @Override
                                               public void onMessage(String message) {
                                                   messageBuilder.append(message);
                                                   messageLatch.countDown();
                                               }
                                           });

                                           session.getBasicRemote().sendText("MESSAGE");
                                       } catch (IOException e) {
                                           // do nothing
                                       }
                                   }
                               }, ClientEndpointConfig.Builder.create().build(),
                getURI("/test?" + DispatchingServletFilter.OP.UpgradeHttpToWebSocket, getScheme()));

        messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());
        assertEquals(messageBuilder.toString(), "helloworld");
    }

    public void testAddEndpoint() throws DeploymentException, InterruptedException, IOException {
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final StringBuilder messageBuilder = new StringBuilder();

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                                       @Override
                                       public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                           try {
                                               session.getBasicRemote().sendText("MESSAGE");
                                               session.close();
                                           } catch (IOException e) {
                                               // do nothing
                                           }
                                       }
                                   }, ClientEndpointConfig.Builder.create().build(),
                    getURI("/anything?" + DispatchingServletFilter.OP.AddEndpoint, getScheme()));
        } catch (DeploymentException e) {
           // Ignore - not WebSocket handshake, just HTTP, used for adding the Endpoint
        }

        final ClientManager client = createClient();
        client.connectToServer(new Endpoint() {
                                   @Override
                                   public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                       try {
                                           session.addMessageHandler(new MessageHandler.Whole<String>() {
                                               @Override
                                               public void onMessage(String message) {
                                                   messageBuilder.append(message);
                                                   messageLatch.countDown();
                                               }
                                           });

                                           session.getBasicRemote().sendText("MESSAGE");
                                       } catch (IOException e) {
                                           // do nothing
                                       }
                                   }
                               }, ClientEndpointConfig.Builder.create().build(),
                getURI("/test/Tyrus/Rocks", getScheme()));

        messageLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());
        assertEquals("TyrusRocks", messageBuilder.toString());
    }

    public static WebArchive createDeployment() throws IOException {

        InputStream inputStream = ServletTestBase.class.getClassLoader().getResourceAsStream("WEB-INF/glassfish-web.xml");
        // Replace the servlet_adaptor in web.xml.template with the System variable set as servlet adaptor
        String webXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);;

        WebArchive archive = ShrinkWrap.create(WebArchive.class, "servlet-test.war");
        archive.addClasses(OnOpenCloseEndpoint.class, PlainEchoEndpoint.class,
                DispatchingServletFilter.class,
                DispatchingServletFilter.OP.class,
                DispatchingServletFilter.ProgramaticEndpoint.class,
                DispatchingServletFilter.ProgramaticEndpoint.ProgramaticEndpointMessageHandler.class,
                DispatchingServletProgrammaticEndpointConfig.class,
                DispatchingServletProgrammaticEndpointConfig.ProgrammaticEndpointConfigurator.class
        );
        archive.addAsWebInfResource(new StringAsset(webXml), "glassfish-web.xml");
        System.out.println(archive.toString(true));

        return archive;

    }

    /**
     * Get the {@link ClientManager} instance.
     *
     * @return {@link ClientManager} which can be used to connect to a server
     */
    protected ClientManager createClient() {
        final String clientContainerClassName = System.getProperty("tyrus.test.container.client");
        if (clientContainerClassName != null) {
            return ClientManager.createClient(clientContainerClassName);
        } else {
            return ClientManager.createClient();
        }
    }

    /**
     * Get the {@link URI} for the given {@link String} path.
     *
     * @param endpointPath the path the {@link URI} is computed for.
     * @param scheme       scheme of newly created {@link URI}. If {@code null}, "ws" will be used.
     * @return {@link URI} which is used to connect to the given path.
     */
    protected URI getURI(String endpointPath, String scheme) {
        try {
            String currentScheme = scheme == null ? "ws" : scheme;
            int port = getPort();

            if ((port == 80 && "ws".equalsIgnoreCase(currentScheme))
                    || (port == 443 && "wss".equalsIgnoreCase(currentScheme))) {
                port = -1;
            }

            return new URI(currentScheme, null, getHost(), port, contextPath + endpointPath, null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get port used for creating remote endpoint {@link URI}.
     *
     * @return port used for creating remote endpoint {@link URI}.
     */
    protected int getPort() {
        final String port = System.getProperty("tyrus.test.port");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException nfe) {
                // do nothing
            }
        }
        return defaultPort;
    }

    protected String getHost() {
        final String host = System.getProperty("tyrus.test.host");
        if (host != null) {
            return host;
        }
        return defaultHost;
    }

    protected void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
