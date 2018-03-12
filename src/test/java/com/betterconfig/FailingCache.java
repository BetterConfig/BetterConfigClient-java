package com.betterconfig;

public class FailingCache extends ConfigCache {

    @Override
    protected String read() throws Exception {
        throw new Exception();
    }

    @Override
    protected void write(String value) throws Exception {
        throw new Exception();
    }
}
