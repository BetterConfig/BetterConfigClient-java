package com.betterconfig;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
    public void getConfigurationJsonWithDefaultConfigTimeout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, default expected
        BetterConfigClientIntegrationTest.Sample config = cl.getConfiguration(BetterConfigClientIntegrationTest.Sample.class, BetterConfigClientIntegrationTest.Sample.Empty);
        assertEquals(BetterConfigClientIntegrationTest.Sample.Empty, config);
    }

    @Test
    public void getValueWithDefaultConfigTimeout() throws IOException {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);

        cl.close();
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
                    return new ManualPollingPolicy(f,c);
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
                    return new ManualPollingPolicy(f,c);
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

    @Test
    public void getConfigurationReturnsDefaultOnExceptionRepeatedly() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        String badJson = "{ test: test] }";
        BetterConfigClientIntegrationTest.Sample def = new BetterConfigClientIntegrationTest.Sample();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson).setBodyDelay(5, TimeUnit.SECONDS));

        assertSame(def, cl.getConfiguration(BetterConfigClientIntegrationTest.Sample.class, def));

        assertSame(def, cl.getConfiguration(BetterConfigClientIntegrationTest.Sample.class, def));

        server.shutdown();
        cl.close();
    }

    @Test
    public void getValueReturnsDefaultOnExceptionRepeatedly() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        String badJson = "{ test: test] }";
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson).setBodyDelay(5, TimeUnit.SECONDS));

        assertSame(def, cl.getValue(String.class, "test", def));

        assertSame(def, cl.getValue(String.class, "test", def));

        server.shutdown();
        cl.close();
    }

    @Test
    public void forceRefreshWithTimeout() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .refreshPolicy((f, c) -> {
                    f.setUrl(server.url("/").toString());
                    return new ManualPollingPolicy(f,c);
                })
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        server.enqueue(new MockResponse().setResponseCode(200).setBody("test").setBodyDelay(5, TimeUnit.SECONDS));

        cl.forceRefresh();

        server.shutdown();
        cl.close();
    }

    @Test
    public void getValueInvalidArguments() {
        BetterConfigClient client = new BetterConfigClient("secret");
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class,null, false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(Boolean.class,"", false));
        assertThrows(IllegalArgumentException.class, () -> client.getValue(BetterConfigClientIntegrationTest.Sample.class,"key", BetterConfigClientIntegrationTest.Sample.Empty));

        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class,null, false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(Boolean.class,"", false).get());
        assertThrows(IllegalArgumentException.class, () -> client.getValueAsync(BetterConfigClientIntegrationTest.Sample.class,"key", BetterConfigClientIntegrationTest.Sample.Empty).get());
    }
}