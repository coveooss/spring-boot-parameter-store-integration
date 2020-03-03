package com.coveo.configuration.parameterstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered
{
    static ParameterStorePropertySourceConfigurationStrategyFactory strategyFactory = new ParameterStorePropertySourceConfigurationStrategyFactory();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (isParameterStorePropertySourceEnabled(environment)) {
            getParameterStorePropertySourceConfigurationStrategy(environment).configureParameterStorePropertySources(environment);
        }
    }

    private ParameterStorePropertySourceConfigurationStrategy getParameterStorePropertySourceConfigurationStrategy(ConfigurableEnvironment environment)
    {
        StrategyType type = isMultiRegionEnabled(environment) ? StrategyType.MULTI_REGION : StrategyType.DEFAULT;
        return strategyFactory.getStrategy(type);
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment)
    {
        String[] userDefinedEnabledProfiles = environment.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                                      String[].class);
        return environment.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                       Boolean.class,
                                       Boolean.FALSE)
                || environment.acceptsProfiles(ParameterStorePropertySourceConfigurationProperties.ENABLED_PROFILE)
                || (!ObjectUtils.isEmpty(userDefinedEnabledProfiles)
                        && environment.acceptsProfiles(userDefinedEnabledProfiles));
    }

    private boolean isMultiRegionEnabled(ConfigurableEnvironment environment)
    {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS);
    }

    @Override
    public int getOrder()
    {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
