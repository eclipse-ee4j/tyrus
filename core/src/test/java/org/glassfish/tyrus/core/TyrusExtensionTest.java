/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.websocket.Extension;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class TyrusExtensionTest {

    @Test
    public void simple() {
        final TyrusExtension test = new TyrusExtension("test");
        assertEquals("test", test.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidName1() {
        new TyrusExtension("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidName2() {
        new TyrusExtension(null);
    }

    @Test
    public void params() {
        final List<Extension.Parameter> parameters = new ArrayList<Extension.Parameter>() {
            {
                add(new Extension.Parameter() {
                    @Override
                    public String getName() {
                        return "Quote";
                    }

                    @Override
                    public String getValue() {
                        return "Mmm. Lost a planet, Master Obi-Wan has. How embarrassing. How embarrassing.";
                    }
                });
            }
        };
        final TyrusExtension test = new TyrusExtension("test", parameters);

        assertNotNull(test.getParameters());

        assertEquals("Quote", test.getParameters().get(0).getName());
        assertEquals("Mmm. Lost a planet, Master Obi-Wan has. How embarrassing. How embarrassing.", test
                .getParameters().get(0).getValue());
    }

    @Test
    public void error1() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(Arrays.asList("ext1;param=val\"ue"));

        assertEquals(0, extensions.size());
    }

    @Test
    public void error2() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(Arrays.asList("ext1;param=value=value"));

        assertEquals(0, extensions.size());
    }

    @Test
    public void error3() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(Arrays.asList("ext1,param=value"));

        assertEquals(1, extensions.size());
    }

    @Test
    public void error4() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(
                Arrays.asList("ext1,param=value,ext2;param=value"));

        assertEquals(2, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertEquals("ext2", extensions.get(1).getName());
        assertTrue(extensions.get(1).getParameters().size() == 1);
        assertEquals("param", extensions.get(1).getParameters().get(0).getName());
        assertEquals("value", extensions.get(1).getParameters().get(0).getValue());
    }

    @Test
    public void testParseHeaders1() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(Arrays.asList("ext1;param=value"));

        assertEquals(1, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertTrue(extensions.get(0).getParameters().size() == 1);
        assertEquals("param", extensions.get(0).getParameters().get(0).getName());
        assertEquals("value", extensions.get(0).getParameters().get(0).getValue());
    }

    @Test
    public void testParseHeaders2() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(
                Arrays.asList("ext1;param=value,ext2;param=value"));

        assertEquals(2, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertTrue(extensions.get(0).getParameters().size() == 1);
        assertEquals("param", extensions.get(0).getParameters().get(0).getName());
        assertEquals("value", extensions.get(0).getParameters().get(0).getValue());
        assertEquals("ext2", extensions.get(1).getName());
        assertTrue(extensions.get(1).getParameters().size() == 1);
        assertEquals("param", extensions.get(1).getParameters().get(0).getName());
        assertEquals("value", extensions.get(1).getParameters().get(0).getValue());

    }

    @Test
    public void testParseHeaders3() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(
                Arrays.asList("ext1;param=value", "ext2;param=value"));

        assertEquals(2, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertTrue(extensions.get(0).getParameters().size() == 1);
        assertEquals("param", extensions.get(0).getParameters().get(0).getName());
        assertEquals("value", extensions.get(0).getParameters().get(0).getValue());
        assertEquals("ext2", extensions.get(1).getName());
        assertTrue(extensions.get(1).getParameters().size() == 1);
        assertEquals("param", extensions.get(1).getParameters().get(0).getName());
        assertEquals("value", extensions.get(1).getParameters().get(0).getValue());
    }

    @Test
    public void testParseHeadersQuoted1() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(Arrays.asList("ext1;param=\"  value  \""));

        assertEquals(1, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertTrue(extensions.get(0).getParameters().size() == 1);
        assertEquals("param", extensions.get(0).getParameters().get(0).getName());
        assertEquals("  value  ", extensions.get(0).getParameters().get(0).getValue());
    }

    @Test
    public void testParseHeadersQuoted2() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(
                Arrays.asList("ext1;param=\"  value  \",ext2;param=\"  value \\\" \""));

        assertEquals(2, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertTrue(extensions.get(0).getParameters().size() == 1);
        assertEquals("param", extensions.get(0).getParameters().get(0).getName());
        assertEquals("  value  ", extensions.get(0).getParameters().get(0).getValue());
        assertEquals("ext2", extensions.get(1).getName());
        assertTrue(extensions.get(1).getParameters().size() == 1);
        assertEquals("param", extensions.get(1).getParameters().get(0).getName());
        assertEquals("  value \" ", extensions.get(1).getParameters().get(0).getValue());
    }

    @Test
    public void testParseHeadersQuoted3() {
        final List<Extension> extensions = TyrusExtension.fromHeaders(
                Arrays.asList("ext1;param=\"  value  \"", "ext2;param=\"  value \\\\ \""));

        assertEquals(2, extensions.size());
        assertEquals("ext1", extensions.get(0).getName());
        assertTrue(extensions.get(0).getParameters().size() == 1);
        assertEquals("param", extensions.get(0).getParameters().get(0).getName());
        assertEquals("  value  ", extensions.get(0).getParameters().get(0).getValue());
        assertEquals("ext2", extensions.get(1).getName());
        assertTrue(extensions.get(1).getParameters().size() == 1);
        assertEquals("param", extensions.get(1).getParameters().get(0).getName());
        assertEquals("  value \\ ", extensions.get(1).getParameters().get(0).getValue());
    }

    @Test
    public void testToString() {
        TyrusExtension.toString(new Extension() {
            @Override
            public String getName() {
                return "name";
            }

            @Override
            public List<Parameter> getParameters() {
                return null;
            }
        });
    }
}
