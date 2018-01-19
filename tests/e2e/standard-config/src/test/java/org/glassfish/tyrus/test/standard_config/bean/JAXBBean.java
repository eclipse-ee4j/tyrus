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

package org.glassfish.tyrus.test.standard_config.bean;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@XmlRootElement
public class JAXBBean {

    public String string1;
    public String string2;

    public JAXBBean(String string1, String string2) {
        this.string1 = string1;
        this.string2 = string2;
    }

    @SuppressWarnings("UnusedDeclaration")
    public JAXBBean() {
    }
}
