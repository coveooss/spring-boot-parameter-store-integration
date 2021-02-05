package com.coveo.configuration.parameterstore.strategy;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import com.coveo.configuration.parameterstore.ParameterStoreSource;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryEndpoint;
import software.amazon.awssdk.core.endpointdiscovery.providers.DefaultEndpointDiscoveryProviderChain;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

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
            try {
                return ssmClientBuilder.endpointOverride(new URI(getCustomEndpoint(environment))).build();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
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
                                       awsRegionProviderChain.getRegion().id());
    }
}
