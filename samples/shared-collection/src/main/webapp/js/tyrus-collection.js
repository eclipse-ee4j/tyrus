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
 * Tyrus namespace.
 *
 * @namespace
 */
Tyrus = {};

/**
 * Tyrus.Collection namespace.
 *
 * @namespace
 */
Tyrus.Collection = {};

/**
 * Create new Tyrus map.
 *
 * @param {string} url
 * @param {function} updateListener
 * @constructor
 */
Tyrus.Collection.Map = function (url, updateListener) {
    var self = this;

    if (!(self instanceof Tyrus.Collection.Map)) {
        throw new Error("You must use 'new' to construct Tyrus collection.");
    }

    if (!url || url === null || typeof url !== "string") {
        throw new Error("Parameter 'url' must be present and must be a string.")
    }

    if (updateListener && updateListener !== null && typeof updateListener !== "function") {
        throw new Error("Parameter 'updateListener' must be a function.")
    }


    var map = {};
    var online = false;

    var _websocket = new WebSocket(url);

    // private methods

    var _onOpen = function () {
        online = true;
    };

    var _onMessage = function (event) {
        var message = JSON.parse(event.data);
        switch (message.event) {
            case "init":
                map = message.map;
                break;
            case "put":
                map[message.key] = message.value;
                break;
            case "remove":
                delete map[message.key];
                break;
            case "clear":
                map = {};
                break;
        }

        updateListener();
    };

    var _onError = function (event) {
        console.log("_onError " + event);
    };

    var _onClose = function () {
        online = false;
    };

    _websocket.onopen = _onOpen;
    _websocket.onmessage = _onMessage;
    _websocket.onerror = _onError;
    _websocket.onclose = _onClose;

    var _validateKey = function (key) {
        if (!key || key === null || typeof key !== "string") {
            throw new Error("Parameter 'key' must be present and must be a string.")
        }
    };

    var _send = function (message) {
        if (online) {
            _websocket.send(JSON.stringify(message));
        }
        updateListener();
    };

    // "privileged" methods.

    /**
     * Get size of the map.
     *
     * @returns {Number} number of records in the map.
     */
    self.size = function () {
        return Object.keys(map).length;
    };

    /**
     * Return {@code true} when the map is empty.
     *
     * @returns {boolean} {@code true} when the map is empty, {@code false} otherwise.
     */
    self.isEmpty = function () {
        return self.size() === 0;
    };

    /**
     * Get value corresponding to provided key from a map.
     *
     * @param {string} key key.
     * @returns {*} value for corresponding key or {@code null} when there is no such key.
     */
    self.get = function (key) {
        _validateKey(key);

        if (map.hasOwnProperty(key)) {
            return map[key];
        }

        return null;
    };

    /**
     * Put an item into the map.
     *
     * @param {string} key key.
     * @param {*} value value.
     */
    self.put = function (key, value) {
        _validateKey(key);

        map[key] = value;

        _send({event: "put", key: key, value: value});
    };

    /**
     * Remove key (and corresponding value) from the map.
     *
     * @param {string} key key to be removed.
     */
    self.remove = function (key) {
        _validateKey(key);

        delete map[key];

        _send({event: "remove", key: key});
    };

    /**
     * Clear the map.
     */
    self.clear = function () {
        map = {};

        _send({event: "clear"});
    };

    /**
     * Get the key set.
     *
     * @returns {Array} array containing all keys from the map (as indexes AND values - TODO).
     */
    self.keySet = function () {
        var result = [];

        for (var key in map) {
            if (map.hasOwnProperty(key)) {
                result[key] = key;
            }
        }

        return result;
    };
};

/**
 * Create new rest map.
 *
 * @param {string} restUrl
 * @param {function} updateListener
 * @constructor
 */
Tyrus.Collection.RestMap = function (restUrl, updateListener) {
    var self = this;

    if (!(self instanceof Tyrus.Collection.RestMap)) {
        throw new Error("You must use 'new' to construct Tyrus collection.");
    }

    if (!restUrl || restUrl === null || typeof restUrl !== "string") {
        throw new Error("Parameter 'postUrl' must be present and must be a string.")
    }

    if (updateListener && updateListener !== null && typeof updateListener !== "function") {
        throw new Error("Parameter 'updateListener' must be a function.")
    }

    var map = {};
    var online = false;

    var source = new EventSource(restUrl);

    // private methods

    var _onOpen = function () {
        online = true;
    };

    var _onMessage = function (event) {
        var message = JSON.parse(event.data);

        switch (message.event) {
            case "init":
                map = message.map;
                break;
            case "put":
                map[message.key] = message.value;
                break;
            case "remove":
                delete map[message.key];
                break;
            case "clear":
                map = {};
                break;
        }

        updateListener();
    };

    var _onError = function (event) {
        console.log("_onError " + event);
    };

    var _onClose = function () {
        online = false;
    };

    source.onopen = _onOpen;
    source.onerror = _onError;
    source.onclose = _onClose;
    source.addEventListener("update", _onMessage, false);

    var _validateKey = function (key) {
        if (!key || key === null || typeof key !== "string") {
            throw new Error("Parameter 'key' must be present and must be a string.")
        }
    };

    var _send = function (message) {
        var xmlhttp;
        if (window.XMLHttpRequest) {
            // code for IE7+, Firefox, Chrome, Opera, Safari
            xmlhttp = new XMLHttpRequest();
        } else {
            // code for IE6, IE5
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
        }
        xmlhttp.open("POST", restUrl, true);
        xmlhttp.send(JSON.stringify(message));
        updateListener();
    };

    // "privileged" methods.

    /**
     * Get size of the map.
     *
     * @returns {Number} number of records in the map.
     */
    self.size = function () {
        return Object.keys(map).length;
    };

    /**
     * Return {@code true} when the map is empty.
     *
     * @returns {boolean} {@code true} when the map is empty, {@code false} otherwise.
     */
    self.isEmpty = function () {
        return self.size() === 0;
    };

    /**
     * Get value corresponding to provided key from a map.
     *
     * @param {string} key key.
     * @returns {*} value for corresponding key or {@code null} when there is no such key.
     */
    self.get = function (key) {
        _validateKey(key);

        if (map.hasOwnProperty(key)) {
            return map[key];
        }

        return null;
    };

    /**
     * Put an item into the map.
     *
     * @param {string} key key.
     * @param {*} value value.
     */
    self.put = function (key, value) {
        _validateKey(key);

        map[key] = value;

        _send({event: "put", key: key, value: value});
    };

    /**
     * Remove key (and corresponding value) from the map.
     *
     * @param {string} key key to be removed.
     */
    self.remove = function (key) {
        _validateKey(key);

        delete map[key];

        _send({event: "remove", key: key});
    };

    /**
     * Clear the map.
     */
    self.clear = function () {
        map = {};

        _send({event: "clear"});
    };

    /**
     * Get the key set.
     *
     * @returns {Array} array containing all keys from the map (as indexes AND values - TODO).
     */
    self.keySet = function () {
        var result = [];

        for (var key in map) {
            if (map.hasOwnProperty(key)) {
                result[key] = key;
            }
        }

        return result;
    };
};
