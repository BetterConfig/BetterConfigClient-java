package com.betterconfig;

/**
 * An in-memory cache implementation used to store the fetched configurations.
 */
public class InMemoryConfigCache extends ConfigCache {

    @Override
    protected String getInternal() {
        return super.inMemoryValue();
    }

    @Override
    protected void setInternal(String value) { }
}
