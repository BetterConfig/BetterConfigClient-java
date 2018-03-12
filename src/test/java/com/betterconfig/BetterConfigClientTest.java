package com.betterconfig;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class BetterConfigClientTest {

    private static final String SECRET = "TEST_SECRET";

    @Test
    public void ensuresProjectSecretIsNotNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new BetterConfigClient(null));

        assertEquals("projectSecret is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> BetterConfigClient.newBuilder().build(null));

        assertEquals("projectSecret is null or empty", builderException.getMessage());
    }

    @Test
    public void ensuresProjectSecretIsNotEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new BetterConfigClient(""));

        assertEquals("projectSecret is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> BetterConfigClient.newBuilder().build(""));

        assertEquals("projectSecret is null or empty", builderException.getMessage());
    }

    @Test
    public void ensuresMaxWaitTimeoutGreaterThanTwoSeconds() {
        assertThrows(IllegalArgumentException.class, () -> BetterConfigClient
                .newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(1));
    }

    @Test
    public void getConfigurationJsonStringWithDefaultConfigTimeout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, null expected
        String config = cl.getConfigurationJsonString();
        assertEquals(null, config);
    }

    @Test
    public void getConfigurationJsonStringWithDefaultConfig() {
        BetterConfigClient cl = new BetterConfigClient(SECRET);

        // makes a call to a real url which would fail, timeout expected
        assertThrows(TimeoutException.class, () -> cl.getConfigurationJsonStringAsync().get(2, TimeUnit.SECONDS));
    }

    @Test
    public void getConfigurationJsonWithDefaultConfigTimeout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, default expected
        BetterConfigClientIntegrationTest.Sample config = cl.getConfiguration(BetterConfigClientIntegrationTest.Sample.class, BetterConfigClientIntegrationTest.Sample.Empty);
        assertEquals(BetterConfigClientIntegrationTest.Sample.Empty, config);
    }

    @Test
    public void getValueWithDefaultConfigTimeout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);
    }

    @Test
    public void getPolicy() {
        BetterConfigClient cl = new BetterConfigClient(SECRET);
        assertNotNull(cl.getRefreshPolicy(AutoPollingPolicy.class));
    }

    @Test
    public void getConfigurationWithFailingCache() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .cache(new FailingCache())
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new AlwaysFetchingPolicy(f,c);
                })
                .build(SECRET);

        String result = "{ \"fakeKey\":\"fakeValue\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));

        assertEquals(result, cl.getConfigurationJsonString());

        server.close();
        cl.close();
    }

    @Test
    public void getConfigurationReturnsPreviousCachedOnFail() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new AlwaysFetchingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        String result = "{ \"fakeKey\":\"fakeValue\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("delayed").setBodyDelay(5, TimeUnit.SECONDS));

        assertEquals(result, cl.getConfigurationJsonString());
        assertEquals(result, cl.getConfigurationJsonString());

        server.close();
        cl.close();
    }
}