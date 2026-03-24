package com.itcall.modules.communication.websocketflux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    private String uri = "/wss";
    private Heartbeat heartbeat = new Heartbeat();

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Heartbeat getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Heartbeat heartbeat) {
        this.heartbeat = heartbeat;
    }

    public static class Heartbeat {
        private long intervalSeconds = 10;
        private int timeoutThreshold = 3;

        public long getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(long intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }

        public int getTimeoutThreshold() {
            return timeoutThreshold;
        }

        public void setTimeoutThreshold(int timeoutThreshold) {
            this.timeoutThreshold = timeoutThreshold;
        }
    }
}
