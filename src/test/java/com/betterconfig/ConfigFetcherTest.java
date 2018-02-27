package com.betterconfig;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class ConfigFetcherTest {
    private MockWebServer server;
    private ConfigFetcher fetcher;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.fetcher = new ConfigFetcher(new OkHttpClient.Builder().build(), "");
        this.fetcher.setUrl(this.server.url("/").toString());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fetcher.close();
        this.server.shutdown();
    }

    @Test
    public void getConfigurationJsonStringETag() throws InterruptedException {
        String result = "{ \"fakeKey\":\"fakeValue\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result).setHeader("ETag", "fakeETag"));
        server.enqueue(new MockResponse().setResponseCode(304));

        assertEquals(result, this.fetcher.getConfigurationJsonString().config());
        assertTrue(this.fetcher.getConfigurationJsonString().isNotModified());

        assertNull(this.server.takeRequest().getHeader("If-None-Match"));
        assertEquals("fakeETag", this.server.takeRequest().getHeader("If-None-Match"));
    }
}
