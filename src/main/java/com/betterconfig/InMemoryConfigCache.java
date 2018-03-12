package com.betterconfig;

/**
 * An in-memory cache implementation used to store the fetched configurations.
 */
public class InMemoryConfigCache extends ConfigCache {

    @Override
    protected String read() {
        return super.inMemoryValue();
    }

    @Override
    protected void write(String value) { }
}
