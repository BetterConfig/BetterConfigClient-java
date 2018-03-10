package com.betterconfig;

public class FailingCache extends ConfigCache {

    @Override
    protected String getInternal() throws Exception {
        throw new Exception();
    }

    @Override
    protected void setInternal(String value) throws Exception {
        throw new Exception();
    }
}
