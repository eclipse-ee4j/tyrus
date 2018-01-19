/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.shared.collection;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Path("collection")
public class SharedCollectionResource {

    private static final SseBroadcaster broadcaster = new SseBroadcaster();

    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput serverSentEventsSource() {

        final EventOutput eventOutput = new EventOutput();

        new Thread() {
            @Override
            public void run() {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }

                final JsonObjectBuilder mapRepresentation = Json.createObjectBuilder();

                for (Map.Entry<String, String> entry : SharedCollection.map.entrySet()) {
                    mapRepresentation.add(entry.getKey(), entry.getValue());
                }

                final JsonObjectBuilder event = Json.createObjectBuilder();
                event.add("event", "init");
                event.add("map", mapRepresentation.build());

                try {
                    eventOutput.write(new OutboundEvent.Builder().name("update")
                                                                 .data(String.class, event.build().toString()).build());
                } catch (IOException e) {
                    // we don't care about that for now.
                }

                broadcaster.add(eventOutput);
            }
        }.start();

        return eventOutput;
    }

    @POST
    public void onMessage(String message) {

        final JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
        final String event = jsonObject.getString("event");

        switch (event) {
            case "put":
                SharedCollection.map.put(jsonObject.getString("key"), jsonObject.getString("value"));
                SharedCollection.broadcast(jsonObject);
                break;
            case "remove":
                SharedCollection.map.remove(jsonObject.getString("key"));
                SharedCollection.broadcast(jsonObject);
                break;
            case "clear":
                SharedCollection.map.clear();
                SharedCollection.broadcast(jsonObject);
                break;
        }
    }

    static void broadcast(JsonObject object) {
        broadcaster.broadcast(new OutboundEvent.Builder().name("update").data(String.class, object.toString()).build());
    }
}
