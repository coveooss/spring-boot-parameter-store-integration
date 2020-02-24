package com.coveo.configuration.parameterstore.strategy;

import org.springframework.core.env.ConfigurableEnvironment;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.AwsRegionProviderChain;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import com.coveo.configuration.parameterstore.ParameterStoreSource;

public class DefaultParameterStorePropertySourceConfigurationStrategy
        implements ParameterStorePropertySourceConfigurationStrategy
{
    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "AWSParameterStorePropertySource";

    private AwsRegionProviderChain awsRegionProviderChain;

    public DefaultParameterStorePropertySourceConfigurationStrategy(AwsRegionProviderChain awsRegionProviderChain)
    {
        this.awsRegionProviderChain = awsRegionProviderChain;
    }

    @Override
    public void configureParameterStorePropertySources(ConfigurableEnvironment environment)
    {
        boolean haltBoot = environment.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                   Boolean.class,
                                                   Boolean.FALSE);
        environment.getPropertySources()
                   .addFirst(buildParameterStorePropertySource(buildSSMClient(environment), haltBoot));
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(AWSSimpleSystemsManagement ssmClient,
                                                                           boolean haltBoot)
    {
        return new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME,
                                                new ParameterStoreSource(ssmClient, haltBoot));
    }

    private AWSSimpleSystemsManagement buildSSMClient(ConfigurableEnvironment environment)
    {
        if (hasCustomEndpoint(environment)) {
            return AWSSimpleSystemsManagementClientBuilder.standard()
                                                          .withEndpointConfiguration(new EndpointConfiguration(getCustomEndpoint(environment),
                                                                                                               getSigningRegion(environment)))
                                                          .build();
        }
        return AWSSimpleSystemsManagementClientBuilder.defaultClient();
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
                                       awsRegionProviderChain.getRegion());
    }
}
