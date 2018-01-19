/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.chat.chatdata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

abstract class ListMessage extends ChatMessage {
    List dataList = new ArrayList();

    ListMessage(String type) {
        super(type);
    }

    void parseDataString(String dataString) {
        StringTokenizer st = new StringTokenizer(dataString, SEP);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (!"".equals(s)) {
                dataList.add(s);
            }
        }
    }

    ListMessage(String type, List dataList) {
        super(type);
        this.dataList = dataList;
    }

    ListMessage(String type, Set dataSet) {
        this(type, new ArrayList(dataSet));
    }

    ListMessage(String type, String elt1, String elt2) {
        this(type, new ArrayList());
        dataList.add(elt1);
        dataList.add(elt2);
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder(type);

        for (Iterator itr = dataList.iterator(); itr.hasNext(); ) {
            builder.append(SEP);
            builder.append(itr.next());
        }

        return builder.toString();
    }

    @Override
    List getData() {
        return dataList;
    }
}
