package com.github.im.server.config.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "group.ai")
public class AiProperties {

    private boolean enabled = false;
    private final Webhook webhook = new Webhook();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public static class Webhook {
        private boolean enabled = true;
        private List<String> tokens = new ArrayList<>();
        private boolean signatureEnabled = false;
        private String secret = "";
        private String signatureHeader = "X-Group-Signature";
        private String timestampHeader = "X-Group-Timestamp";
        private long timestampToleranceSeconds = 300L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getTokens() {
            return tokens;
        }

        public void setTokens(List<String> tokens) {
            this.tokens = tokens;
        }

        public boolean isSignatureEnabled() {
            return signatureEnabled;
        }

        public void setSignatureEnabled(boolean signatureEnabled) {
            this.signatureEnabled = signatureEnabled;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getSignatureHeader() {
            return signatureHeader;
        }

        public void setSignatureHeader(String signatureHeader) {
            this.signatureHeader = signatureHeader;
        }

        public String getTimestampHeader() {
            return timestampHeader;
        }

        public void setTimestampHeader(String timestampHeader) {
            this.timestampHeader = timestampHeader;
        }

        public long getTimestampToleranceSeconds() {
            return timestampToleranceSeconds;
        }

        public void setTimestampToleranceSeconds(long timestampToleranceSeconds) {
            this.timestampToleranceSeconds = timestampToleranceSeconds;
        }
    }
}
