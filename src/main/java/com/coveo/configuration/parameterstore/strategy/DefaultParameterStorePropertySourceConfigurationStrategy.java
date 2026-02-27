package com.coveo.configuration.parameterstore.strategy;

import org.springframework.boot.context.properties.bind.Binder;
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
        Binder binder = Binder.get(environment);
        boolean haltBoot = binder.bind(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT, Boolean.class)
                                 .orElse(Boolean.FALSE);
        environment.getPropertySources()
                   .addFirst(buildParameterStorePropertySource(buildSSMClient(environment, ssmClientBuilder, binder),
                                                               haltBoot));
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(SsmClient ssmClient, boolean haltBoot)
    {
        return new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME,
                                                new ParameterStoreSource(ssmClient, haltBoot));
    }

    private SsmClient buildSSMClient(ConfigurableEnvironment environment,
                                     SsmClientBuilder ssmClientBuilder,
                                     Binder binder)
    {
        if (hasCustomEndpoint(binder)) {
            return ssmClientBuilder.endpointOverride(URI.create(getCustomEndpoint(binder)))
                                   .region(Region.of(getSigningRegion(binder)))
                                   .build();
        }
        return ssmClientBuilder.build();
    }

    private boolean hasCustomEndpoint(Binder binder)
    {
        return binder.bind(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT, String.class)
                     .isBound();
    }

    private String getCustomEndpoint(Binder binder)
    {
        return binder.bind(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT, String.class)
                     .get();
    }

    private String getSigningRegion(Binder binder)
    {
        return binder.bind(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_SIGNING_REGION, String.class)
                     .orElse(awsRegionProviderChain.getRegion().toString());
    }
}
