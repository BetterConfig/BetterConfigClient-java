package com.betterconfig;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class FetchAlwaysPolicyTest {
    private RefreshPolicy policy;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        ConfigCache cache = new InMemoryConfigCache();
        fetcher.setUrl(this.server.url("/").toString());
        this.policy = new FetchAlwaysPolicy(fetcher,cache);
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.policy.close();
        this.server.shutdown();
    }

    @Test
    public void get() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //next call will get the new value
        assertEquals("test2", this.policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getCacheFails() throws InterruptedException, ExecutionException {
        ConfigFetcher fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        fetcher.setUrl(this.server.url("/").toString());
        FetchAlwaysPolicy lPolicy = new FetchAlwaysPolicy(fetcher, new FailingCache());

        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test2").setBodyDelay(3, TimeUnit.SECONDS));

        //first call
        assertEquals("test", lPolicy.getConfigurationJsonAsync().get());

        //next call will get the new value
        assertEquals("test2", lPolicy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getWithFailedRefresh() throws InterruptedException, ExecutionException {
        this.server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        this.server.enqueue(new MockResponse().setResponseCode(500));

        //first call
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());

        //previous value returned because of the refresh failure
        assertEquals("test", this.policy.getConfigurationJsonAsync().get());
    }
}