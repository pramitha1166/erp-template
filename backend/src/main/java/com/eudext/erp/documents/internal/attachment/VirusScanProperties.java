package com.eudext.erp.documents.internal.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** DOC-4. Off by default so no environment needs a reachable ClamAV daemon just to boot — see {@link #enabled}. */
@ConfigurationProperties(prefix = "eudext.documents.attachments.virus-scan")
public class VirusScanProperties {

    /** Master switch — off by default; the {@code docker} profile turns it on against the local ClamAV instance. */
    private boolean enabled = false;

    private String host = "localhost";
    private int port = 3310;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
