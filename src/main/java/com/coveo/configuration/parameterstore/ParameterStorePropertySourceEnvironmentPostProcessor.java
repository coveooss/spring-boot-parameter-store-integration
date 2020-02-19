package com.coveo.configuration.parameterstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor
{
    static boolean initialized;
    protected static ParameterStorePropertySourceConfigurationStrategyFactory postProcessStrategyFactory = new ParameterStorePropertySourceConfigurationStrategyFactory();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {
            getPostProcessStrategy(environment).configureParameterStorePropertySources(environment);

            if (doesNotSupportMultipleApplicationContexts(environment)) {
                initialized = true;
            }
        }
    }

    private ParameterStorePropertySourceConfigurationStrategy getPostProcessStrategy(ConfigurableEnvironment environment)
    {
        StrategyType type = isMultiRegionEnabled(environment) ? StrategyType.MULTI_REGION : StrategyType.DEFAULT;
        return postProcessStrategyFactory.getStrategy(type);
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

    private boolean doesNotSupportMultipleApplicationContexts(ConfigurableEnvironment environment)
    {
        return !environment.getProperty(ParameterStorePropertySourceConfigurationProperties.SUPPORT_MULTIPLE_APPLICATION_CONTEXTS,
                                        Boolean.class,
                                        Boolean.FALSE);
    }

    private boolean isMultiRegionEnabled(ConfigurableEnvironment environment)
    {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS);
    }
}
