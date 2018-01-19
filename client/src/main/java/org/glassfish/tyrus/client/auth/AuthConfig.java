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

package org.glassfish.tyrus.client.auth;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.Beta;

/**
 * AuthConfig serves as a configuration of HTTP authentication.
 * <p>
 * An instance of this class can be created by {@link AuthConfig} and it must be registered to property bag in {@link
 * ClientManager}.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see Authenticator
 * @see ClientManager#getProperties()
 */
@Beta
public class AuthConfig {

    /**
     * Encoding used for authentication calculations.
     */
    static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");

    /**
     * Basic authentication scheme key.
     */
    static final String BASIC = "Basic";

    /**
     * Digest authentication scheme key.
     */
    static final String DIGEST = "Digest";

    private final Map<String, Authenticator> authenticators;

    private AuthConfig(Map<String, Authenticator> authenticators) {
        TreeMap<String, Authenticator> map = new TreeMap<String, Authenticator>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(authenticators);
        this.authenticators = Collections.unmodifiableMap(map);
    }

    /**
     * Get an unmodifiable map of authenticators, where case insensitive authentication scheme to {@link
     * Authenticator}.
     *
     * @return unmodifiable map of authenticators. Case insensitive authentication scheme is mapped to {@link
     * Authenticator}.
     */
    public Map<String, Authenticator> getAuthenticators() {
        return authenticators;
    }

    /**
     * Create new {@link Builder} instance, which contains provided Basic and Digest authenticators.
     *
     * @return builder instance.
     */
    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AuthConfig{");
        boolean first = true;
        for (Map.Entry<String, Authenticator> authenticator : authenticators.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(authenticator.getKey());
            sb.append("->");
            sb.append(authenticator.getValue().getClass().getName());
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * The AuthConfig.Builder is a class used for creating an instance of {@link AuthConfig} for purpose of HTTP
     * Authentication.
     * <p>
     * Example 1 - building an authentication configuration enhanced with user defined NTLM authentication and
     * overridden Basic Authentication:
     * <pre><code>
     * AuthConfig authConfig = AuthConfig.Builder.create().
     *                          registerAuthProvider("NTLM", myAuthenticator).
     *                          registerAuthProvider("Basic", myBasicAuthenticator).
     *                          build();
     * </code></pre>
     * Example 2 - Building an authentication configuration with disabled Basic scheme authenticator:
     * <pre><code>
     * AuthConfig authConfig = AuthConfig.Builder.create().
     *                          disableProvidedBasicAuth().
     *                          build();
     * </code></pre>
     *
     * @see Credentials
     * @see Authenticator
     */
    public static final class Builder {

        private final Map<String, Authenticator> authenticators =
                new TreeMap<String, Authenticator>(String.CASE_INSENSITIVE_ORDER);

        private Builder() {
            authenticators.put(BASIC, new BasicAuthenticator());
            authenticators.put(DIGEST, new DigestAuthenticator());
        }

        /**
         * Create new {@link Builder} instance, which contains provided Basic and Digest authenticators.
         *
         * @return {@link AuthConfig.Builder} instance.
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * Register {@link Authenticator} for provided authentication scheme.
         * <p>
         * Only one {@link Authenticator} for one authentication scheme can be registered. If current builder instance
         * already contains {@link Authenticator} for provided scheme, existing authenticator will be replaced. Note
         * that schemes are compared in case insensitive manner.
         *
         * @param scheme        authentication scheme for which the registered authenticator will be used. Scheme is
         *                      compared case insensitive.
         * @param authenticator {@link Authenticator} instance to be registered.
         * @return updated {@link AuthConfig.Builder} instance.
         */
        public final Builder registerAuthProvider(final String scheme, final Authenticator authenticator) {
            this.authenticators.put(scheme, authenticator);
            return this;
        }

        /**
         * Disable provided Basic {@link Authenticator}.
         *
         * @return updated {@link AuthConfig.Builder} instance.
         */
        public final Builder disableProvidedBasicAuth() {
            if (authenticators.get(BASIC) != null && authenticators.get(BASIC) instanceof BasicAuthenticator) {
                authenticators.remove(BASIC);
            }
            return this;
        }

        /**
         * Disable provided Digest {@link Authenticator}.
         *
         * @return updated {@link AuthConfig.Builder} instance.
         */
        public final Builder disableProvidedDigestAuth() {
            if (authenticators.get(DIGEST) != null && authenticators.get(DIGEST) instanceof DigestAuthenticator) {
                authenticators.remove(DIGEST);
            }
            return this;
        }

        /**
         * Build an instance of {@link AuthConfig}.
         *
         * @return an instance of {@link AuthConfig}.
         */
        public AuthConfig build() {
            return new AuthConfig(authenticators);
        }
    }
}
