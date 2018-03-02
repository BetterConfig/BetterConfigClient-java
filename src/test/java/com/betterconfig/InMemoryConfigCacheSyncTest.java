package com.betterconfig;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryConfigCacheSyncTest {
    private InMemoryConfigCache cache;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        fetcher.setUrl(this.server.url("/").toString());
        this.cache = InMemoryConfigCache.newBuilder()
                .cacheRefreshRateInSeconds(5)
                .asyncRefresh(false)
                .build(fetcher);
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.cache.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", this.cache.get());

        //wait for cache invalidation
        Thread.sleep(6000);

        //next call will block until the new value is fetched
        assertEquals("test2", this.cache.get());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        assertEquals("test", this.cache.get());

        //wait for cache invalidation
        Thread.sleep(6000);

        //previous value returned because of the refresh failure
        assertEquals("test", this.cache.get());
    }
}