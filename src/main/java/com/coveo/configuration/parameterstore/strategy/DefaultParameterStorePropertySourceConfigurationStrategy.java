package com.coveo.configuration.parameterstore.strategy;

import org.springframework.core.env.ConfigurableEnvironment;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import com.coveo.configuration.parameterstore.ParameterStoreSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

import java.net.URI;
import java.util.Objects;

public class DefaultParameterStorePropertySourceConfigurationStrategy
        implements ParameterStorePropertySourceConfigurationStrategy
{
    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "AWSParameterStorePropertySource";

    private final AwsRegionProviderChain awsRegionProviderChain;

    public DefaultParameterStorePropertySourceConfigurationStrategy(AwsRegionProviderChain awsRegionProviderChain)
    {
        this.awsRegionProviderChain = Objects.requireNonNull(awsRegionProviderChain);
    }

    @Override
    public void configureParameterStorePropertySources(ConfigurableEnvironment environment,
                                                       SsmClientBuilder ssmClientBuilder)
    {
        boolean haltBoot = environment.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                   Boolean.class,
                                                   Boolean.FALSE);
        environment.getPropertySources()
                   .addFirst(buildParameterStorePropertySource(buildSSMClient(environment, ssmClientBuilder),
                                                               haltBoot));
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(SsmClient ssmClient, boolean haltBoot)
    {
        return new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME,
                                                new ParameterStoreSource(ssmClient, haltBoot));
    }

    private SsmClient buildSSMClient(ConfigurableEnvironment environment, SsmClientBuilder ssmClientBuilder)
    {
        if (hasCustomEndpoint(environment)) {
            return ssmClientBuilder.endpointOverride(URI.create(getCustomEndpoint(environment)))
                                   .region(Region.of(getSigningRegion(environment)))
                                   .build();
        }
        return ssmClientBuilder.build();
    }

    private boolean hasCustomEndpoint(ConfigurableEnvironment environment)
    {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT);
    }

    private String getCustomEndpoint(ConfigurableEnvironment environment)
    {
        return environment.getProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT);
    }

    private String getSigningRegion(ConfigurableEnvironment environment)
    {
        return environment.getProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_SIGNING_REGION,
                                       awsRegionProviderChain.getRegion().toString());
    }
}
