/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.glassfish.tyrus.spi.UpgradeRequest;

/**
 * Implementation of all possible request interfaces. Should be the only point of truth.
 *
 * @author Pavel Bucek
 */
public final class RequestContext extends UpgradeRequest {

    private final URI requestURI;
    private final String queryString;
    private final Object httpSession;
    private final boolean secure;
    private final Principal userPrincipal;
    private final Builder.IsUserInRoleDelegate isUserInRoleDelegate;
    private final String remoteAddr;
    private final String serverAddr;
    private final int serverPort;

    private final TyrusConfiguration tyrusConfiguration;

    private Map<String, List<String>> headers;
    private Map<String, List<String>> parameterMap;

    private RequestContext(URI requestURI, String queryString, Object httpSession, boolean secure, Principal
            userPrincipal, Builder.IsUserInRoleDelegate IsUserInRoleDelegate, String remoteAddr, String serverAddr,
            int serverPort, Map<String, List<String>> parameterMap, Map<String, List<String>> headers,
            Map<String, Object> tyrusProperties) {
        this.requestURI = requestURI;
        this.queryString = queryString;
        this.httpSession = httpSession;
        this.secure = secure;
        this.userPrincipal = userPrincipal;
        this.isUserInRoleDelegate = IsUserInRoleDelegate;
        this.remoteAddr = remoteAddr;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.parameterMap = parameterMap;
        this.headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        this.tyrusConfiguration = new TyrusConfiguration.Builder().tyrusProperties(tyrusProperties).build();

        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    /**
     * Get headers.
     *
     * @return headers map. List items are corresponding to header declaration in HTTP request.
     */
    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns the header value corresponding to the name.
     *
     * @param name header name.
     * @return {@link List} of header values iff found, {@code null} otherwise.
     */
    @Override
    public String getHeader(String name) {
        final List<String> stringList = headers.get(name);
        if (stringList == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String s : stringList) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(s);
            }

            return sb.toString();
        }
    }

    /**
     * Make headers and parameter map read-only.
     */
    public void lock() {
        this.headers = Collections.unmodifiableMap(headers);
        this.parameterMap = Collections.unmodifiableMap(parameterMap);
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (isUserInRoleDelegate != null) {
            return isUserInRoleDelegate.isUserInRole(role);
        }

        return false;
    }

    @Override
    public Object getHttpSession() {
        return httpSession;
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRequestUri() {
        return requestURI.toString();
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    /**
     * Get the Internet Protocol (IP) address of the client or last proxy that sent the request.
     *
     * @return a {@link String} containing the IP address of the client that sent the request or {@code null} when
     * method is called on client-side.
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * Returns the host name of the server to which the request was sent.
     *
     * @return a {@link String} Returns the host name of the server to which the request was sent or {@code null} when
     * method is called on client-side.
     */
    public String getServerAddr() {
        return serverAddr;
    }

    /**
     * Get the port of the last client or proxy that sent the request.
     *
     * @return a port of the client that sent the request.
     */
    public int getServerPort() {
        return serverPort;
    }

    /* package */ TyrusConfiguration getTyrusConfiguration() {
        return tyrusConfiguration;
    }


    /**
     * {@link RequestContext} builder.
     */
    public static final class Builder {

        private URI requestURI;
        private String queryString;
        private Object httpSession;
        private boolean secure;
        private Principal userPrincipal;
        private Builder.IsUserInRoleDelegate isUserInRoleDelegate;
        private Map<String, List<String>> parameterMap;
        private String remoteAddr;
        private String serverAddr;
        private int serverPort;
        private Map<String, List<String>> headers;
        private Map<String, Object> tyrusProperties;

        /**
         * Create empty builder.
         *
         * @return empty builder instance.
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * Create builder instance based on provided {@link RequestContext}.
         *
         * @param requestContext request context.
         * @return builder instance.
         */
        public static Builder create(RequestContext requestContext) {
            Builder builder = new Builder();

            builder.requestURI = requestContext.requestURI;
            builder.queryString = requestContext.queryString;
            builder.httpSession = requestContext.httpSession;
            builder.secure = requestContext.secure;
            builder.userPrincipal = requestContext.userPrincipal;
            builder.isUserInRoleDelegate = requestContext.isUserInRoleDelegate;
            builder.parameterMap = requestContext.parameterMap;
            builder.remoteAddr = requestContext.remoteAddr;
            builder.serverAddr = requestContext.serverAddr;
            builder.serverPort = requestContext.serverPort;
            builder.headers = requestContext.headers;
            builder.tyrusProperties = requestContext.tyrusConfiguration.tyrusProperties();

            return builder;
        }

        /**
         * Set request URI.
         *
         * @param requestURI request URI to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder requestURI(URI requestURI) {
            this.requestURI = requestURI;
            return this;
        }

        /**
         * Set query string.
         *
         * @param queryString query string to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder queryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        /**
         * Set http session.
         *
         * @param httpSession {@code jakarta.servlet.http.HttpSession} session to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder httpSession(Object httpSession) {
            this.httpSession = httpSession;
            return this;
        }

        /**
         * Set secure state.
         *
         * @param secure secure state to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        /**
         * Set {@link Principal}.
         *
         * @param principal principal to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder userPrincipal(Principal principal) {
            this.userPrincipal = principal;
            return this;
        }

        /**
         * Set delegate for {@link RequestContext#isUserInRole(String)} method.
         *
         * @param isUserInRoleDelegate delegate for {@link RequestContext#isUserInRole(String)}.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder isUserInRoleDelegate(IsUserInRoleDelegate isUserInRoleDelegate) {
            this.isUserInRoleDelegate = isUserInRoleDelegate;
            return this;
        }

        /**
         * Set parameter map.
         *
         * @param parameterMap parameter map. Takes map returned from ServletRequest#getParameterMap.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder parameterMap(Map<String, String[]> parameterMap) {
            if (parameterMap != null) {
                this.parameterMap = new HashMap<String, List<String>>();
                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    this.parameterMap.put(entry.getKey(), Arrays.asList(entry.getValue()));
                }
            } else {
                this.parameterMap = null;
            }

            return this;
        }

        /**
         * Set remote address.
         *
         * @param remoteAddr remote address to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder remoteAddr(String remoteAddr) {
            this.remoteAddr = remoteAddr;
            return this;
        }

        /**
         * Set server address or hostname.
         *
         * @param serverAddr server address to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder serverAddr(String serverAddr) {
            this.serverAddr = serverAddr;
            return this;
        }

        /**
         * Set server port.
         *
         * @param serverPort server port to be set.
         * @return updated {@link RequestContext.Builder} instance.
         */
        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        /**
         * Set properties for Tyrus framework.
         * @param tyrusProperties
         */
        public Builder tyrusProperties(Map<String, Object> tyrusProperties) {
            this.tyrusProperties = tyrusProperties;
            return this;
        }

        /**
         * Build {@link RequestContext} from given properties.
         *
         * @return created {@link RequestContext}.
         */
        public RequestContext build() {
            return new RequestContext(requestURI, queryString, httpSession, secure, userPrincipal,
                                      isUserInRoleDelegate, remoteAddr, serverAddr, serverPort,
                                      parameterMap != null ? parameterMap : new HashMap<String, List<String>>(),
                                      headers, tyrusProperties);
        }

        /**
         * Is user in role delegate.
         * <p>
         * Cannot easily query ServletContext or HttpServletRequest for this information, since it is stored only as
         * object.
         */
        public interface IsUserInRoleDelegate {

            /**
             * Returns a boolean indicating whether the authenticated user is included in the specified logical "role".
             * Roles and role membership can be defined using deployment descriptors. If the user has not been
             * authenticated, the method returns false.
             *
             * @param role a String specifying the name of the role.
             * @return a boolean indicating whether the user making this request belongs to a given role; false if the
             * user has not been authenticated.
             */
            public boolean isUserInRole(String role);
        }
    }
}
