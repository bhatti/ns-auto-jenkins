package com.plexobject.nsauto.jenkins.plugin;

import hudson.util.Secret;

public interface NSAutoParameters {
    String getApiUrl();

    String getGroup();

    Secret getApiKey();

    String getBinaryName();

    String getDescription();
}
