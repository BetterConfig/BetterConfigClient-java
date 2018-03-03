# BetterConfig client for Java
BetterConfig is a cloud based configuration as a service. It integrates with your apps, backends, websites, and other programs, so you can configure them using an easy to follow online User Interface (UI). [https://betterconfig.com](https://betterconfig.com)

[![Build Status](https://travis-ci.org/BetterConfig/BetterConfigClient-java.svg?branch=master)](https://travis-ci.org/BetterConfig/BetterConfigClient-java) [![Coverage Status](https://img.shields.io/codecov/c/github/BetterConfig/BetterConfigClient-java.svg)](https://codecov.io/gh/BetterConfig/BetterConfigClient-java) [![Javadocs](http://javadoc.io/badge/com.betterconfig/betterconfig-client.svg)](http://javadoc.io/doc/com.betterconfig/betterconfig-client)

## Getting started

**1. Add the package to your project**

*Maven:*
```xml
<dependency>
    <groupId>com.betterconfig</groupId>
    <artifactId>betterconfig-client</artifactId>
    <version>1.0.5</version>
</dependency>
```
*Gradle:*
```groovy
compile 'com.betterconfig:betterconfig-client:1.0.5'
```
**2. Get your Project Secret from [BetterConfig.com](https://betterconfig.com) portal**
![YourConnectionUrl](https://raw.githubusercontent.com/BetterConfig/BetterConfigClient-dotnet/master/media/readme01.png  "YourProjectToken")

**3. Import the BetterConfig package**
```java
import com.betterconfig.*;
```

**4. Create a BetterConfigClient instance**
```java
BetterConfigClient client = new BetterConfigClient("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
**5. Get your config value**
```java
boolean isMyAwesomeFeatureEnabled = client.getBooleanValue("key-of-my-awesome-feature", false);
if(isMyAwesomeFeatureEnabled) {
    //show your awesome feature to the world!
}
```
## Configuration
### HttpClient
The BetterConfig client internally uses an [OkHttpClient](https://github.com/square/okhttp) instance to fetch the latest configuration over HTTP. You have the option to override the internal HttpClient with your customized one. For example if your application runs behind a proxy you can do the following:
```java
Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxyHost", proxyPort));

BetterConfigClient client = BetterConfigClient.newBuilder()
                .httpClient(new OkHttpClient.Builder()
                            .proxy(proxy)
                            .build())
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
> As the BetterConfig client maintains the whole lifetime of the internal HttpClient, it's being closed simultaneously with the BetterConfig client, refrain from closing the HttpClient manually.

### Default cache
By default the BetterConfig client uses a built in in-memory cache implementation, which can be customized with the following configurations:
#### Cache refresh interval
You can define the refresh rate of the cache in seconds, after the initial cached value is set this value will be used to determine how much time must pass before initiating a new configuration fetch request through the `ConfigFetcher`.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .cache(configFetcher -> InMemoryConfigCache.newBuilder()
                                            .cacheRefreshRateInSeconds(2) // the cache will expire in 2 seconds
                                            .build(configFetcher))
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
#### Async / Sync refresh
You can define how do you want to handle the expiration of the cached configuration. If you choose asynchronous refresh then 
when a request is being made on the cache while it's expired, the previous value will be returned without blocking the caller, 
until the fetching of the new configuration is completed.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .cache(configFetcher -> InMemoryConfigCache.newBuilder()
                                            .asyncRefresh(true) // the refresh will be executed asynchronously
                                            .build(configFetcher))
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
If you set the `.asyncRefresh()` to be `false`, the refresh operation will be executed synchronously, 
so the caller thread will be blocked until the fetching of the new configuration is completed.

### Custom Cache
You also have the option to inject your custom cache implementation into the client. All you have to do is to inherit from the `ConfigCache` abstract class:
```java
public class MyCustomCache extends ConfigCache {

    // the cache gets a ConfigFetcher argument used to fetch
    // the latest configuration over HTTP
    public MyCustomCache(ConfigFetcher configFetcher) {
        super(configFetcher);
    }

    @Override
    public String get() {
        // here you have to return with the cached value,
        // or you can initiate the fetching of the latest config
        // with the given ConfigFetcher: String config = super.fetcher().getConfigurationJsonString();
    }

    @Override
    public void invalidateCache() {
        // here you can invalidate the cached value
    }

    // optional, in case if you have any resources that should be closed
    @Override
    public void close() throws IOException {
        super.close();
        // here you can close your resources
    }
}
```
> If you decide to override the `close()` method, you also have to call the `super.close()` to tear down the cache appropriately.

Then you can simply inject your custom cache implementation into the BetterConfig client:
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .cache(configFetcher -> new MyCustomCache(configFetcher)) // inject your custom cache
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```

## Logging
The BetterConfig client uses the [slf4j](https://www.slf4j.org)'s facade for logging.
