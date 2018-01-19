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

package org.glassfish.tyrus.core.uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusEndpointWrapper;
import org.glassfish.tyrus.core.uri.internal.PathSegment;
import org.glassfish.tyrus.core.uri.internal.UriComponent;

/**
 * Defines a match on an endpoint. The match is either exact, or is not exact.
 * If the match is not exact, it means that some of the path segments on the endpoint
 * are variables. In this case, the Match object carries the indices of the variable
 * segments in the endpoint path, the map of the parameter names and values.
 *
 * @author dannycoward
 */
public class Match {

    private final TyrusEndpointWrapper endpointWrapper; // the endpoint that has the path
    private final Map<String, String> parameters = new HashMap<String, String>();
    //list of all segment indices in the path with variables
    private final List<Integer> variableSegmentIndices = new ArrayList<Integer>();

    private static final Logger LOGGER = Logger.getLogger(Match.class.getName());

    /**
     * Constructor.
     *
     * @param endpointWrapper {@link TyrusEndpointWrapper} instance.
     */
    private Match(TyrusEndpointWrapper endpointWrapper) {
        this.endpointWrapper = endpointWrapper;
    }

    /**
     * Get variable segment indices (indexes).
     *
     * @return list of variable segment indices.
     */
    List<Integer> getVariableSegmentIndices() {
        return this.variableSegmentIndices;
    }

    /**
     * Get the index of the left most path variable.
     *
     * @return the index of the left most path variable.
     */
    int getLowestVariableSegmentIndex() {
        if (this.getVariableSegmentIndices().isEmpty()) {
            return -1;
        } else {
            return this.getVariableSegmentIndices().get(0);
        }
    }

    /**
     * Add new parameter.
     *
     * @param name  parameter name.
     * @param value parameter value.
     * @param index parameter index.
     */
    void addParameter(String name, String value, int index) {
        this.parameters.put(name, value);
        this.variableSegmentIndices.add(index);
    }

    /**
     * Get map of parameter names-value pairs.
     *
     * @return map of parameter names-value pairs.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Get endpoint wrapper.
     *
     * @return endpoint wrapper.
     */
    public TyrusEndpointWrapper getEndpointWrapper() {
        return this.endpointWrapper;
    }

    @Override
    public String toString() {
        return endpointWrapper.getEndpointPath();
    }

    /**
     * {@code true} if the path of the matched endpoint does not contain any variables, {@code false} otherwise.
     *
     * @return {@code true} if the path of the matched endpoint does not contain any variables, {@code false} otherwise.
     */
    boolean isExact() {
        return this.getLowestVariableSegmentIndex() == -1;
    }

    /**
     * Return a list of all endpoints with path matching the request path. The endpoints are in order of match
     * preference, best match first.
     *
     * @param requestPath  request path.
     * @param endpoints    endpoints.
     * @param debugContext debug context.
     * @return a list of all endpoints with path matching the request path. The endpoints are in order of match
     * preference, best match first.
     */
    public static List<Match> getAllMatches(String requestPath, Set<TyrusEndpointWrapper> endpoints,
                                            DebugContext debugContext) {
        List<Match> matches = new ArrayList<Match>();

        for (TyrusEndpointWrapper endpoint : endpoints) {
            Match m = matchPath(requestPath, endpoint, debugContext);

            if (m != null) {
                matches.add(m);
            }
        }

        Collections.sort(matches, new MatchComparator(debugContext));
        debugContext.appendTraceMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_IN,
                                        "Endpoints matched to the request URI: ", matches);
        return matches;
    }

    private static Match matchPath(String requestPath, TyrusEndpointWrapper endpoint, DebugContext debugContext) {
        debugContext.appendTraceMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_IN, "Matching request URI ",
                                        requestPath, " against ", endpoint.getEndpointPath());
        List<PathSegment> requestPathSegments = UriComponent.decodePath(requestPath, true);
        List<PathSegment> endpointPathSegments = UriComponent.decodePath(endpoint.getEndpointPath(), true);

        if (requestPathSegments.size() != endpointPathSegments.size()) {
            debugContext
                    .appendTraceMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_IN, "URIs ", requestPath, " and ",
                                        endpoint.getEndpointPath(), " have different length");
            return null;
        } else {
            Match m = new Match(endpoint);
            boolean somethingMatched = false;

            for (int i = 0; i < requestPathSegments.size(); i++) {
                String requestSegment = requestPathSegments.get(i).getPath();
                String endpointSegment = endpointPathSegments.get(i).getPath();

                if (requestSegment.equals(endpointSegment)) {
                    somethingMatched = true;
                    // continue...
                } else if (isVariable(endpointSegment)) {
                    somethingMatched = true;
                    m.addParameter(getVariableName(endpointSegment), requestSegment, i);
                } else {
                    debugContext.appendTraceMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_IN, "Segment \"",
                                                    endpointSegment, "\" does not match");
                    return null; // no match
                }
            }

            if (somethingMatched) {
                return m;
            } else {
                return null;
            }
        }
    }

    /**
     * Check for equivalent paths.
     *
     * @param path1 path to be checked.
     * @param path2 path to be checked.
     * @return {@code true} when provided path are equivalent, {@code false} otherwise.
     */
    public static boolean isEquivalent(String path1, String path2) {
        List<String> path1EList = asEquivalenceList(path1);
        List<String> path2EList = asEquivalenceList(path2);
        return path1EList.equals(path2EList);
    }

    /**
     * Decodes the path and replaces all variables with {x}.
     */
    private static List<String> asEquivalenceList(String path) {
        List<String> equivalenceList = new ArrayList<String>();
        List<PathSegment> segments = UriComponent.decodePath(path, true);

        for (PathSegment next : segments) {
            if (isVariable(next.getPath())) {
                equivalenceList.add("{x}");
            } else {
                equivalenceList.add(next.getPath());
            }
        }

        return equivalenceList;
    }

    private static boolean isVariable(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static String getVariableName(String segment) {
        return segment.substring(1, segment.length() - 1);
    }
}

