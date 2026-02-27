package com.coveo.configuration.parameterstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.util.ObjectUtils;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered
{
    static ParameterStorePropertySourceConfigurationStrategyFactory strategyFactory = new ParameterStorePropertySourceConfigurationStrategyFactory();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        Binder binder = Binder.get(environment);
        if (isParameterStorePropertySourceEnabled(environment, binder)) {
            getParameterStorePropertySourceConfigurationStrategy(binder).configureParameterStorePropertySources(environment.getPropertySources(),
                                                                                                                binder,
                                                                                                                preconfigureSSMClientBuilder(binder));
        }
    }

    private SsmClientBuilder preconfigureSSMClientBuilder(Binder binder)
    {
        Integer maxRetries = binder.bind(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY,
                                         Integer.class)
                                   .orElse(SdkDefaultRetrySetting.maxAttempts(RetryMode.STANDARD));
        ClientOverrideConfiguration clientOverrideConfiguration = ClientOverrideConfiguration.builder()
                                                                                             .retryStrategy(configurator -> configurator.maxAttempts(maxRetries
                                                                                                     + 1))
                                                                                             .build();
        return SsmClient.builder().overrideConfiguration(clientOverrideConfiguration);
    }

    private ParameterStorePropertySourceConfigurationStrategy getParameterStorePropertySourceConfigurationStrategy(Binder binder)
    {
        StrategyType type = isMultiRegionEnabled(binder) ? StrategyType.MULTI_REGION : StrategyType.DEFAULT;
        return strategyFactory.getStrategy(type);
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment, Binder binder)
    {
        String[] userDefinedEnabledProfiles = binder.bind(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                          String[].class)
                                                    .orElse(null);
        return binder.bind(ParameterStorePropertySourceConfigurationProperties.ENABLED, Boolean.class)
                     .orElse(Boolean.FALSE)
                || environment.acceptsProfiles(Profiles.of(ParameterStorePropertySourceConfigurationProperties.ENABLED_PROFILE))
                || (!ObjectUtils.isEmpty(userDefinedEnabledProfiles)
                        && environment.acceptsProfiles(Profiles.of(userDefinedEnabledProfiles)));
    }

    private boolean isMultiRegionEnabled(Binder binder)
    {
        return binder.bind(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                           String[].class)
                     .isBound();
    }

    @Override
    public int getOrder()
    {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
