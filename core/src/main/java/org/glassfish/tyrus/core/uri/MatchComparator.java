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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.core.DebugContext;

/**
 * The comparator is used to order the best matches in a list.
 *
 * @author dannycoward
 */
class MatchComparator implements Comparator<Match>, Serializable {

    private final transient Logger LOGGER = Logger.getLogger(MatchComparator.class.getName());
    private final transient DebugContext debugContext;

    MatchComparator(DebugContext debugContext) {
        this.debugContext = debugContext;
    }

    // m1 wins = return -1
    // m2 wins = return 1
    // neither wins = return 0
    @Override
    public int compare(Match m1, Match m2) {
        debugContext
                .appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, "Choosing better match from ",
                                    m1, " and ", m2);
        boolean m1exact = m1.isExact();
        boolean m2exact = m2.isExact();

        if (m1exact) {
            if (m2exact) { // both exact matches, no-one wins
                debugContext
                        .appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, "Both ", m1, " and ", m2,
                                            " are exact matches");
                return 0;
            } else { // m2not exact, m1 is, m1 wins
                debugContext.appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, m1,
                                                " is an exact match");
                // m1 is exact match
                return -1; // m1 wins
            }
        } else { // m1 is not exact, m2 is, m2 wins
            if (m2exact) {
                debugContext.appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, m2,
                                                " is an exact match");
                //m 2 is exact match
                return 1; //m2 is exact, m1 isn't, so m2 wins
            } else { // neither are exact !
                // iterate through the variable segment indices, left to right
                // test each one: the one with the larger index wins since
                // more of the path from the left is exact.
                // if the two indices are the same, keep going
                // if all the indices are the same, they are equivalent
                // if the same, keep going.
                List<Integer> m1Indices = m1.getVariableSegmentIndices();
                List<Integer> m2Indices = m2.getVariableSegmentIndices();

                for (int i = 0; i < Math.max(m1Indices.size(), m2Indices.size()); i++) {

                    if (i > m2Indices.size() - 1) {
                        debugContext.appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, m2,
                                                        " is a  better match, because ", m1, " has more variables");
                        //m2 wins because m1 has more variables to go.
                        return 1;
                    } else if (i > m1Indices.size() - 1) {
                        debugContext.appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, m1,
                                                        " is a  better match, because ", m2, " has more variables");
                        // m1 wins because m2 has more variables to go
                        return -1; // m1 wins because m2 has more variables to go
                    } else {
                        int m1Index = m1Indices.get(i);
                        int m2Index = m2Indices.get(i);
                        if (m1Index > m2Index) {
                            // m1 wins as it has a larger exact path
                            debugContext.appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, m1,
                                                            " is a  better match, because it has longer exact path");
                            return -1;
                        } else if (m2Index > m1Index) {
                            // m2 wins as it has a larger exact path
                            debugContext.appendTraceMessage(LOGGER, Level.FINER, DebugContext.Type.MESSAGE_IN, m2,
                                                            " is a  better match, because it has longer exact path");
                            return 1;
                        }
                    }
                }
                // both had same indices
                return 0;
            }
        }
        // can't get here
    }
}
