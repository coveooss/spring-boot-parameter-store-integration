[![Build Status](https://api.travis-ci.org/coveo/spring-boot-parameter-store-integration.svg?branch=master)](https://travis-ci.org/coveo/spring-boot-parameter-store-integration)
[![MIT license](http://img.shields.io/badge/license-MIT-brightgreen.svg)](https://github.com/coveo/spring-boot-parameter-store-integration/blob/master/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.coveo/spring-boot-parameter-store-integration/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.coveo/spring-boot-parameter-store-integration)

# Spring Boot Parameter Store Integration

The Spring Boot Parameter Store Integration is a tiny library used to integrate AWS Parameter Store in Spring Boot's powerful property injection. For example, it allows you to fetch a property directly using the `@Value` annotation. In fact, it simply adds a PropertySource with highest precedence to the existing ones (see [Spring Boot's External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)).

## Requirements
The library uses:

- [Spring Boot](https://spring.io/projects/spring-boot) 1.5.14.RELEASE
- [AWS Java SDK](https://aws.amazon.com/sdk-for-java/) 1.11.351

Those can be overriden in your `pom.xml`.  

The library was tested and worked properly with:

- [Spring Boot](https://spring.io/projects/spring-boot) 1.4.x, 1.5.x and 2.0.x
- [AWS Java SDK](https://aws.amazon.com/sdk-for-java/) >= 1.11.164

## Unleashing the Magic

#### For your pom.xml:
```
<dependency>
    <groupId>com.coveo</groupId>
    <artifactId>spring-boot-parameter-store-integration</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### There are 3 ways to enable this lib after importing it in your pom.xml, pick yours:
- Set `awsParameterStorePropertySource.enabled` to `true` (yml, properties, or anything [here](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html))
- Add the profile `awsParameterStorePropertySourceEnabled` to your active profiles
- Set `awsParameterStorePropertySource.enabledProfiles` with some custom profiles that should integrate the AWS Parameter Store using a comma-separated list such as `MyProductionProfile,MyTestProfile`  
**Important**: using other list injecting methods like a yaml list won't work because this property gets loaded too early in the boot process.

#### Using the lib:
Use a property that is prefixed with `/` somewhere such as
```
@Value("${/my/parameter/store/property}")
String value;
```

#### You might be wondering why use slashes (`/`)?
The AWS Parameter Store already uses this naming pattern to classify your properties as you would do with folders. Using this prefix to limit the number of calls to AWS at boot seemed natural. This means properties not prefixed with `/` can't yet be fetched in the AWS Parameter Store using this lib.

## AWS Credentials

The lib uses the [DefaultAWSCredentialProviderChain](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html). This means if your code is running on an EC2 instance that has access to a Parameter Store property and its associated KMS key, the library should be able to fetch it without any configuration.

## Using Spring Boot's Placeholder Properties

Since naming properties with some `/` everywhere seems a bit awkward and not coherent with actual property keys, we suggest using [placeholder properties](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-placeholders-in-properties). This way you can use AWS Parameter Store without modifying your current property naming scheme.
Using nested properties makes things easier for multiple environments and simplifies property name changes in the Parameter Store without editing the code (using an environment variable).

So your yml could look like this:
```
my.super.duper.secret: defaultValue
``` 
And you would inject the Parameter Store key through an environment variable using a placeholder like this:
```
my.super.duper.secret: ${/my/parameter/store/secret}
``` 
When Spring Boot encounters your environment variable, it doesn't inject `${/my/parameter/store/secret}` in your property `my.super.duper.secret`, but instead tries to load the property `/my/parameter/store/secret` from its property sources, and then hits the Parameter Store source because of the prefix `/`.

## Halting the Boot to Prevent Production Incidents

The default behaviour of a PropertySource when it can't find a property is to return `null`, and then the PropertyResolver iterates on every other PropertySource to find a matching value. This is the default behaviour for this lib.

If you want to halt the boot when a property prefixed with `/` isn't found in the Parameter Store, just set `awsParameterStorePropertySource.haltBoot` to `true` in your properties. We personally use this to prevent injecting default properties in a production environment.

## Spring Cloud

TL;DR: Define the enabling properties in the bootstrap properties (`bootstrap.yml`, `bootstrap.properties`, [etc.](https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_the_bootstrap_application_context))(see [Unleashing the Magic](#there-are-3-ways-to-enable-this-lib-after-importing-it-in-your-pomxml-pick-yours)).

Spring Cloud has a second application context named bootstrap that gets initialized before Spring Boot's normal application context. Since this lib uses an EnvironmentPostPrecessor to add the Parameter Store PropertySource, it gets triggered twice. Since you probably want it to be added on the bootstrap context, we highly recommend using the bootstrap properties to enable this lib. More details on this [Stack Overflow Thread](https://stackoverflow.com/questions/50935915/adding-a-conditional-external-propertysource-in-a-spring-boot-application).

## Contributing
Open an issue to report bugs or to request additional features. Pull requests are always welcome.

# Enjoy 🍻

__UPDATE:__ I wrote a [blog post](https://source.coveo.com/2018/08/03/spring-boot-and-aws-parameter-store/) about this library on our technical blog.
