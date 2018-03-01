# BetterConfig client for Java
BetterConfig integrates with your products to allow you to create and configure apps, backends, websites and other programs using an easy to follow online User Interface (UI). [https://betterconfig.com](https://betterconfig.com)

[![Build Status](https://travis-ci.org/BetterConfig/BetterConfigClient-java.svg?branch=master)](https://travis-ci.org/BetterConfig/BetterConfigClient-java) [![Coverage Status](https://img.shields.io/codecov/c/github/BetterConfig/BetterConfigClient-java.svg)](https://codecov.io/gh/BetterConfig/BetterConfigClient-java)

## Getting started

**1. Add the package to your project**

*Maven:* 
```xml
<dependency>
    <groupId>com.betterconfig</groupId>
    <artifactId>betterconfig-client</artifactId>
    <version>1.0.4</version>
</dependency>
```
*Gradle:*
```groovy
compile 'com.betterconfig:betterconfig-client:1.0.4'
```
**2. Get your Project token from [BetterConfig.com](https://betterconfig.com) portal**
![YourConnectionUrl](https://raw.githubusercontent.com/BetterConfig/BetterConfigClient-dotnet/master/media/readme01.png  "YourProjectToken")

**3. Import the BetterConfig package**
```java
import com.betterconfig.*;
```

**4. Create a BetterConfigClient instance**
```java
ConfigurationProvider configProvider = new BetterConfigClient("<PLACE-YOUR-PROJECT-TOKEN-HERE>");
```
**5. Get your config value**
```java
String myStringValue = configProvider.getStringValue("keySampleText", "");
System.out.format("My String value from BetterConfig: %s", myStringValue);
```
