var WebSocketClient = (function () {
    var socket;
    var isConnected = false;

    function connectWithToken(url, token) {
        var fullUrl = url;
        if (token) {
            fullUrl += "?token=" + encodeURIComponent(token);
        }
        _connect(fullUrl);
    }

    function connectWithSession(url) {
        // Relies on browser's automatic cookie sending (JSESSIONID)
        _connect(url);
    }

    // Internal function to handle the actual WebSocket connection
    function _connect(fullUrl) {
        socket = new WebSocket(fullUrl);

        socket.onopen = function (event) {
            console.log("Connected to WebSocket at " + fullUrl);
            isConnected = true;
        };

        socket.onmessage = function (event) {
            var msg = event.data;
            console.log("Received Message: " + msg);
            if (typeof onMessageReceived === 'function') {
                onMessageReceived(msg);
            }
        };

        socket.onclose = function (event) {
            console.log("WebSocket Disconnected. Reason: " + event.reason);
            isConnected = false;
        };

        socket.onerror = function (error) {
            console.error("WebSocket Error: " + error);
        };
    }

    function send(message) {
        if (isConnected) {
            socket.send(message);
        } else {
            console.warn("WebSocket is not connected");
        }
    }

    return {
        connectWithToken: connectWithToken,
        connectWithSession: connectWithSession,
        send: send
    };
})();
