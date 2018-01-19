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

package org.glassfish.tyrus.core.coder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;

import org.glassfish.tyrus.core.ReflectionHelper;

/**
 * Collection of decoders for all primitive types.
 *
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public abstract class PrimitiveDecoders<T> extends CoderAdapter implements Decoder.Text<T> {
    public static final List<Class<? extends Decoder>> ALL;
    public static final Map<Class<?>, Decoder.Text<?>> ALL_INSTANCES;

    static {
        ALL = Collections.unmodifiableList(Arrays.<Class<? extends Decoder>>asList(
                BooleanDecoder.class,
                ByteDecoder.class,
                CharacterDecoder.class,
                DoubleDecoder.class,
                FloatDecoder.class,
                IntegerDecoder.class,
                LongDecoder.class,
                ShortDecoder.class
        ));

        ALL_INSTANCES = getAllInstances();
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    public static class BooleanDecoder extends PrimitiveDecoders<Boolean> {
        @Override
        public Boolean decode(String s) throws DecodeException {
            Boolean result;

            try {
                result = Boolean.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class ByteDecoder extends PrimitiveDecoders<Byte> {
        @Override
        public Byte decode(String s) throws DecodeException {
            Byte result;

            try {
                result = Byte.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class CharacterDecoder extends PrimitiveDecoders<Character> {
        @Override
        public Character decode(String s) throws DecodeException {
            Character result;

            try {
                result = s.charAt(0);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class DoubleDecoder extends PrimitiveDecoders<Double> {
        @Override
        public Double decode(String s) throws DecodeException {
            Double result;

            try {
                result = Double.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class FloatDecoder extends PrimitiveDecoders<Float> {
        @Override
        public Float decode(String s) throws DecodeException {
            Float result;

            try {
                result = Float.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class IntegerDecoder extends PrimitiveDecoders<Integer> {
        @Override
        public Integer decode(String s) throws DecodeException {
            Integer result;

            try {
                result = Integer.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class LongDecoder extends PrimitiveDecoders<Long> {
        @Override
        public Long decode(String s) throws DecodeException {
            Long result;

            try {
                result = Long.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    public static class ShortDecoder extends PrimitiveDecoders<Short> {
        @Override
        public Short decode(String s) throws DecodeException {
            Short result;

            try {
                result = Short.valueOf(s);
            } catch (Exception e) {
                throw new DecodeException(s, "Decoding failed", e);
            }

            return result;
        }
    }

    private static Map<Class<?>, Decoder.Text<?>> getAllInstances() {
        Map<Class<?>, Decoder.Text<?>> map = new HashMap<Class<?>, Decoder.Text<?>>();

        for (Class<? extends Decoder> dec : ALL) {
            Class<?> type = ReflectionHelper.getClassType(dec, Decoder.Text.class);
            try {
                map.put(type, (Decoder.Text<?>) dec.newInstance());
            } catch (Exception e) {
                Logger.getLogger(PrimitiveDecoders.class.getName())
                      .log(Level.WARNING, String.format("Decoder %s could not have been instantiated.", dec));
            }
        }

        return map;
    }

}
