package com.betterconfig;

public interface ConfigurationChangeListener {
    void onConfigurationChanged(ConfigurationParser parser, String newConfiguration);
}
