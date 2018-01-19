/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

var output;
var debug = false;
var websocket;
var separator = ":";
var id = 0;

var endpointPath = "/auction";
var wsUri = getRootUri() + endpointPath;

/**
 * Get application root uri with ws/wss protocol.
 *
 * @returns {string}
 */
function getRootUri() {
    var uri = "ws://" + (document.location.hostname == "" ? "localhost" : document.location.hostname) + ":" +
        (document.location.port == "" ? "8080" : document.location.port);

    var pathname = window.location.pathname;

    if (endsWith(pathname, "/index.html")) {
        uri = uri + pathname.substring(0, pathname.length - 11);
    } else if (endsWith(pathname, "/")) {
        uri = uri + pathname.substring(0, pathname.length - 1);
    }

    return uri;
}

function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

function init() {
    output = document.getElementById("output");
    websocket = new WebSocket(wsUri);
    websocket.onopen = function (evt) {
        login();
    };
    websocket.onmessage = function (evt) {
        handleResponse(evt)
    };
    websocket.onerror = function (evt) {
        onError(evt)
    };
}

function login() {
}

function doLogin() {
    var myStr = "lreq" + separator + id + separator + document.getElementById("loginID").value;
    websocket.send(myStr);
    window.setTimeout('to_select()', 10);
}

function to_select() {
    var link = "select.html?name=" + document.getElementById("loginID").value;
    window.location = link;
}


function onError(evt) {
    writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function writeToScreen(message) {
    if (debug) {
        var pre = document.createElement("p");
        pre.style.wordWrap = "break-word";
        pre.innerHTML = message;
        output.appendChild(pre);
    }
}

window.addEventListener("load", init, false);
