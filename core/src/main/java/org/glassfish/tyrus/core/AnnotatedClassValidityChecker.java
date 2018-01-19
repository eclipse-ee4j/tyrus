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

package org.glassfish.tyrus.core;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * Used when processing a class annotated with {@link @ServerEndpoint} to check that it complies with specification.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
class AnnotatedClassValidityChecker {

    private final Class<?> annotatedClass;
    private final List<Class<? extends Encoder>> encoders;
    private final ErrorCollector collector;
    private final MessageHandlerManager handlerManager;

    /**
     * Construct the class validity checker.
     *
     * @param annotatedClass class for which this checker is constructed.
     */
    public AnnotatedClassValidityChecker(Class<?> annotatedClass, List<Class<? extends Encoder>> encoders,
                                         List<Class<? extends Decoder>> decoders, ErrorCollector collector) {
        this.annotatedClass = annotatedClass;
        this.encoders = encoders;
        this.collector = collector;
        this.handlerManager = new MessageHandlerManager(decoders);
    }

    /**
     * Checks whether the params of the method annotated with {@link javax.websocket.OnMessage} comply with the
     * specification.
     * <p>
     * Voluntary parameters of type {@link javax.websocket.Session} and parameters annotated with {@link
     * javax.websocket.server.PathParam} are checked in advance in {@link AnnotatedEndpoint}.
     */
    public void checkOnMessageParams(Method method, MessageHandler handler) {
        try {
            handlerManager.addMessageHandler(handler);
        } catch (IllegalStateException ise) {
            collector.addException(new DeploymentException(
                    LocalizationMessages.CLASS_CHECKER_ADD_MESSAGE_HANDLER_ERROR(
                            annotatedClass.getCanonicalName(), ise.getMessage()), ise.getCause()));
        }

        checkOnMessageReturnType(method);
    }

    private void checkOnMessageReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        if (returnType != void.class && returnType != String.class && returnType != ByteBuffer.class
                && returnType != byte[].class && !returnType.isPrimitive() && checkEncoders(returnType)
                && !PrimitivesToWrappers.isPrimitiveWrapper(returnType)) {
            logDeploymentException(new DeploymentException(
                    LocalizationMessages.CLASS_CHECKER_FORBIDDEN_RETURN_TYPE(annotatedClass.getName(), method.getName())
            ));
        }
    }

    /**
     * Checks whether the params of method annotated with {@link javax.websocket.OnOpen} comply with the specification.
     * <p>
     * Voluntary parameters of type {@link javax.websocket.Session} and parameters annotated with {@link
     * javax.websocket.server.PathParam} are checked in advance in {@link AnnotatedEndpoint}.
     *
     * @param params to be checked.
     */
    public void checkOnOpenParams(Method method, Map<Integer, Class<?>> params) {
        for (Class<?> value : params.values()) {
            if (value != EndpointConfig.class) {
                logDeploymentException(new DeploymentException(
                        LocalizationMessages.CLASS_CHECKER_FORBIDDEN_WEB_SOCKET_OPEN_PARAM(
                                annotatedClass.getName(), method.getName(), value)));
            }
        }
    }

    /**
     * Checks whether the params of method annotated with {@link javax.websocket.OnClose} comply with the
     * specification.
     *
     * @param params unknown params of the method.
     */
    public void checkOnCloseParams(Method method, Map<Integer, Class<?>> params) {
        for (Class<?> value : params.values()) {
            if (value != CloseReason.class) {
                logDeploymentException(new DeploymentException(
                        LocalizationMessages.CLASS_CHECKER_FORBIDDEN_WEB_SOCKET_CLOSE_PARAM(
                                annotatedClass.getName(), method.getName())));
            }
        }
    }

    /**
     * Checks whether the params of method annotated with {@link javax.websocket.OnError} comply with the
     * specification.
     *
     * @param params unknown params of the method.
     */
    public void checkOnErrorParams(Method method, Map<Integer, Class<?>> params) {
        boolean throwablePresent = false;

        for (Class<?> value : params.values()) {
            if (value != Throwable.class) {
                logDeploymentException(new DeploymentException(
                        LocalizationMessages.CLASS_CHECKER_FORBIDDEN_WEB_SOCKET_ERROR_PARAM(
                                annotatedClass.getName(), method.getName(), value)));
            } else {
                if (throwablePresent) {
                    logDeploymentException(new DeploymentException(
                            LocalizationMessages.CLASS_CHECKER_MULTIPLE_IDENTICAL_PARAMS(
                                    annotatedClass.getName(), method.getName())));
                }
                throwablePresent = true;
            }
        }

        if (!throwablePresent) {
            logDeploymentException(new DeploymentException(
                    LocalizationMessages.CLASS_CHECKER_MANDATORY_PARAM_MISSING(
                            annotatedClass.getName(), method.getName())));
        }
    }

    private String getPrefix(String methodName) {
        return String.format("Method:  %s.%s:", annotatedClass.getName(), methodName);
    }

    private boolean checkEncoders(Class<?> requiredType) {
        for (Class<? extends Encoder> encoderClass : encoders) {
            if (AnnotatedEndpoint.getEncoderClassType(encoderClass).isAssignableFrom(requiredType)) {
                return false;
            }
        }

        return true;
    }

    private void logDeploymentException(DeploymentException de) {
        collector.addException(de);
    }
}
