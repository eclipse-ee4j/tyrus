/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * Get root Uri.
 *
 * @param {string} protocol
 * @returns {string}
 */
window.getRootUri = function (protocol) {
    if (!protocol || protocol === null || typeof protocol !== "string") {
        throw new Error("Parameter 'postUrl' must be present and must be a string.")
    }

    return protocol + "://" + (document.location.hostname == "" ? "localhost" : document.location.hostname) +
        ":" + (document.location.port == "" ? "8085" : document.location.port);
};

window.boardUpdate = function () {
    var content = "";

    content += "<div class='item'><span class='key title'>" + "Key" +
        "</span><span class='value title'>" + "Value" +
        "</span><span class='remove'></span></div>";

    for (var key in window.collection.keySet()) {
        //noinspection JSUnfilteredForInLoop
        var val = window.collection.get(key);
        content += "<div class='item' onclick='document.getElementById(\"key\").value = \"" + key + "\"; document.getElementById(\"value\").value = \"" + val + "\"'>" +
            "<span class='key'>" + key +
            "</span><span class='value'>" + val +
            "</span><span class='remove' onclick='window.collection.remove(\"" + key + "\")'>&#10008;</span></div>";
    }

    document.getElementById("board").innerHTML = content;
};

window.change = function () {
    var key = document.getElementById("key").value;
    if (key && key !== null && key !== "") {
        window.collection.put(key, document.getElementById("value").value);
    }
};

window.wsUri = window.getRootUri("ws") + "/sample-shared-collection/ws/collection";
window.restUri = window.getRootUri("http") + "/sample-shared-collection/rest/collection";

window.test = function () {
    console.time("test");
    var exceptions = 0;
    for (i = 0; i < 10000; i++) {
        try {
            window.collection.put("test", i.toString());
        } catch (excetion) {
            exceptions++;
            // console.log(excetion);
        }
    }
    console.timeEnd("test");
    console.log("exceptions: " + exceptions)
};
