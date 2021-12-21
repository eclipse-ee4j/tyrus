/*
 * Copyright (c) 2008, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.jdk.client;

import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * SSLEngineConfigurator class from Grizzly project.
 * <p>
 * Utility class, which helps to configure {@link SSLEngine}. Should be passed to client via configuration properties.
 * Example:
 * <pre>
 *      SslContextConfigurator sslContextConfigurator = new SslContextConfigurator();
 *      sslContextConfigurator.setTrustStoreFile("...");
 *      sslContextConfigurator.setTrustStorePassword("...");
 *      sslContextConfigurator.setTrustStoreType("...");
 *      sslContextConfigurator.setKeyStoreFile("...");
 *      sslContextConfigurator.setKeyStorePassword("...");
 *      sslContextConfigurator.setKeyStoreType("...");
 *      SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(sslContextConfigurator, true, false,
 * false);
 *      client.getProperties().put(ClientManager.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
 * </pre>
 *
 * @author Alexey Stashok
 * @deprecated Please use {@link org.glassfish.tyrus.client.SslEngineConfigurator}.
 */
public class SslEngineConfigurator {
    private final Object sync = new Object();

    protected volatile SslContextConfigurator sslContextConfiguration;

    protected volatile SSLContext sslContext;

    /**
     * The list of cipher suites.
     */
    protected String[] enabledCipherSuites = null;
    /**
     * The list of protocols.
     */
    protected String[] enabledProtocols = null;
    /**
     * Client mode when handshaking.
     */
    protected boolean clientMode;
    /**
     * Require client Authentication.
     */
    protected boolean needClientAuth;
    /**
     * True when requesting authentication.
     */
    protected boolean wantClientAuth;
    /**
     * Has the enabled protocol configured.
     */
    private boolean isProtocolConfigured = false;
    /**
     * Has the enabled Cipher configured.
     */
    private boolean isCipherConfigured = false;

    /**
     * Create SSL Engine configuration basing on passed {@link SSLContext}.
     *
     * @param sslContext {@link SSLContext}.
     */
    public SslEngineConfigurator(SSLContext sslContext) {
        this(sslContext, true, false, false);
    }

    /**
     * Create SSL Engine configuration basing on passed {@link SSLContext},
     * using passed client mode, need/want client auth parameters.
     *
     * @param sslContext     {@link SSLContext}.
     * @param clientMode     will be configured to work in client mode.
     * @param needClientAuth client authentication is required.
     * @param wantClientAuth client should authenticate.
     */
    public SslEngineConfigurator(final SSLContext sslContext, final boolean clientMode, final boolean needClientAuth,
                                 final boolean wantClientAuth) {
        if (sslContext == null) {
            throw new IllegalArgumentException("SSLContext can not be null");
        }

        this.sslContextConfiguration = null;
        this.sslContext = sslContext;
        this.clientMode = clientMode;
        this.needClientAuth = needClientAuth;
        this.wantClientAuth = wantClientAuth;
    }

    /**
     * Create SSL Engine configuration basing on passed {@link SslContextConfigurator}.
     * This constructor makes possible to initialize SSLEngine and SSLContext in lazy
     * fashion on first {@link #createSSLEngine()} call.
     *
     * @param sslContextConfiguration {@link SslContextConfigurator}.
     */
    public SslEngineConfigurator(SslContextConfigurator sslContextConfiguration) {
        this(sslContextConfiguration, true, false, false);
    }

    /**
     * Create SSL Engine configuration basing on passed {@link SslContextConfigurator}.
     * This constructor makes possible to initialize SSLEngine and SSLContext in lazy
     * fashion on first {@link #createSSLEngine()} call.
     *
     * @param sslContextConfiguration {@link SslContextConfigurator}.
     * @param clientMode              will be configured to work in client mode.
     * @param needClientAuth          client authentication is required.
     * @param wantClientAuth          client should authenticate.
     */
    public SslEngineConfigurator(SslContextConfigurator sslContextConfiguration, boolean clientMode,
                                 boolean needClientAuth, boolean wantClientAuth) {
        if (sslContextConfiguration == null) {
            throw new IllegalArgumentException("SSLContextConfigurator can not be null");
        }

        this.sslContextConfiguration = sslContextConfiguration;
        this.clientMode = clientMode;
        this.needClientAuth = needClientAuth;
        this.wantClientAuth = wantClientAuth;
    }

    public SslEngineConfigurator(SslEngineConfigurator pattern) {
        this.sslContextConfiguration = pattern.sslContextConfiguration;
        this.sslContext = pattern.sslContext;
        this.clientMode = pattern.clientMode;
        this.needClientAuth = pattern.needClientAuth;
        this.wantClientAuth = pattern.wantClientAuth;

        this.enabledCipherSuites = pattern.enabledCipherSuites;
        this.enabledProtocols = pattern.enabledProtocols;

        this.isCipherConfigured = pattern.isCipherConfigured;
        this.isProtocolConfigured = pattern.isProtocolConfigured;
    }

    /**
     * Default constructor.
     */
    protected SslEngineConfigurator() {
    }

    /**
     * Create and configure {@link SSLEngine}, basing on current settings.
     *
     * @return {@link SSLEngine}.
     */
    public SSLEngine createSSLEngine() {
        if (sslContext == null) {
            synchronized (sync) {
                if (sslContext == null) {
                    sslContext = sslContextConfiguration.createSSLContext();
                }
            }
        }

        final SSLEngine sslEngine = sslContext.createSSLEngine();
        configure(sslEngine);

        return sslEngine;
    }

    /**
     * Configure passed {@link SSLEngine}, using current configurator settings
     *
     * @param sslEngine {@link SSLEngine} to configure.
     * @return configured {@link SSLEngine}.
     */
    public SSLEngine configure(final SSLEngine sslEngine) {
        if (enabledCipherSuites != null) {
            if (!isCipherConfigured) {
                enabledCipherSuites = configureEnabledCiphers(sslEngine, enabledCipherSuites);
                isCipherConfigured = true;
            }
            sslEngine.setEnabledCipherSuites(enabledCipherSuites);
        }

        if (enabledProtocols != null) {
            if (!isProtocolConfigured) {
                enabledProtocols = configureEnabledProtocols(sslEngine, enabledProtocols);
                isProtocolConfigured = true;
            }
            sslEngine.setEnabledProtocols(enabledProtocols);
        }

        sslEngine.setUseClientMode(clientMode);
        if (wantClientAuth) {
            sslEngine.setWantClientAuth(true);
        }
        if (needClientAuth) {
            sslEngine.setNeedClientAuth(true);
        }

        return sslEngine;
    }

    /**
     * Will {@link SSLEngine} be configured to work in client mode.
     *
     * @return <code>true</code>, if {@link SSLEngine} will be configured to work
     * in <code>client</code> mode, or <code>false</code> for <code>server</code> mode.
     */
    public boolean isClientMode() {
        return clientMode;
    }

    /**
     * Set {@link SSLEngine} to be configured to work in client mode.
     *
     * @param clientMode <code>true</code>, if {@link SSLEngine} will be configured
     *                   to work in <code>client</code> mode, or <code>false</code> for <code>server</code>
     *                   mode.
     * @return updated configurator instance.
     */
    public SslEngineConfigurator setClientMode(boolean clientMode) {
        this.clientMode = clientMode;
        return this;
    }


    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public SslEngineConfigurator setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
        return this;
    }

    public boolean isWantClientAuth() {
        return wantClientAuth;
    }

    public SslEngineConfigurator setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
        return this;
    }

    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites.clone();
    }

    public SslEngineConfigurator setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites.clone();
        return this;
    }

    public String[] getEnabledProtocols() {
        return enabledProtocols.clone();
    }

    public SslEngineConfigurator setEnabledProtocols(String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols.clone();
        return this;
    }

    public boolean isCipherConfigured() {
        return isCipherConfigured;
    }

    public SslEngineConfigurator setCipherConfigured(boolean isCipherConfigured) {
        this.isCipherConfigured = isCipherConfigured;
        return this;
    }

    public boolean isProtocolConfigured() {
        return isProtocolConfigured;
    }

    public SslEngineConfigurator setProtocolConfigured(boolean isProtocolConfigured) {
        this.isProtocolConfigured = isProtocolConfigured;
        return this;
    }

    public SSLContext getSslContext() {
        if (sslContext == null) {
            synchronized (sync) {
                if (sslContext == null) {
                    sslContext = sslContextConfiguration.createSSLContext();
                }
            }
        }

        return sslContext;
    }

    /**
     * Return the list of allowed protocol.
     *
     * @return String[] an array of supported protocols.
     */
    private static String[] configureEnabledProtocols(SSLEngine sslEngine, String[] requestedProtocols) {

        String[] supportedProtocols = sslEngine.getSupportedProtocols();
        String[] protocols = null;
        ArrayList<String> list = null;
        for (String supportedProtocol : supportedProtocols) {
            /*
             * Check to see if the requested protocol is among the
             * supported protocols, i.e., may be enabled
             */
            for (String protocol : requestedProtocols) {
                protocol = protocol.trim();
                if (supportedProtocol.equals(protocol)) {
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    list.add(protocol);
                    break;
                }
            }
        }

        if (list != null) {
            protocols = list.toArray(new String[list.size()]);
        }

        return protocols;
    }

    /**
     * Determines the SSL cipher suites to be enabled.
     *
     * @return Array of SSL cipher suites to be enabled, or null if none of the
     * requested ciphers are supported.
     */
    private static String[] configureEnabledCiphers(SSLEngine sslEngine, String[] requestedCiphers) {

        String[] supportedCiphers = sslEngine.getSupportedCipherSuites();
        String[] ciphers = null;
        ArrayList<String> list = null;
        for (String supportedCipher : supportedCiphers) {
            /*
             * Check to see if the requested protocol is among the
             * supported protocols, i.e., may be enabled
             */
            for (String cipher : requestedCiphers) {
                cipher = cipher.trim();
                if (supportedCipher.equals(cipher)) {
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    list.add(cipher);
                    break;
                }
            }
        }

        if (list != null) {
            ciphers = list.toArray(new String[list.size()]);
        }

        return ciphers;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SSLEngineConfigurator");
        sb.append("{clientMode=").append(clientMode);
        sb.append(", enabledCipherSuites=")
          .append(enabledCipherSuites == null ? "null" : Arrays.asList(enabledCipherSuites).toString());
        sb.append(", enabledProtocols=")
          .append(enabledProtocols == null ? "null" : Arrays.asList(enabledProtocols).toString());
        sb.append(", needClientAuth=").append(needClientAuth);
        sb.append(", wantClientAuth=").append(wantClientAuth);
        sb.append(", isProtocolConfigured=").append(isProtocolConfigured);
        sb.append(", isCipherConfigured=").append(isCipherConfigured);
        sb.append('}');
        return sb.toString();
    }

    public SslEngineConfigurator copy() {
        return new SslEngineConfigurator(this);
    }
}
