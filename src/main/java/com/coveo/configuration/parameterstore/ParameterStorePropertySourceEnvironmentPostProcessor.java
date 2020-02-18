package com.coveo.configuration.parameterstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor
{
    static boolean initialized;
    protected static ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory postProcessStrategyFactory = new ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {
            getPostProcessStrategy(environment).postProcess(environment);

            if (doesNotSupportMultipleApplicationContexts(environment)) {
                initialized = true;
            }
        }
    }

    private ParameterStorePropertySourceEnvironmentPostProcessStrategy getPostProcessStrategy(ConfigurableEnvironment environment)
    {
        StrategyType type = isMultiRegionEnabled(environment) ? StrategyType.MULTI_REGION : StrategyType.DEFAULT;
        return postProcessStrategyFactory.getStrategy(type);
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment)
    {
        String[] userDefinedEnabledProfiles = environment.getProperty(ParameterStorePropertySourceConfigurationProperty.ACCEPTED_PROFILES,
                                                                      String[].class);
        return environment.getProperty(ParameterStorePropertySourceConfigurationProperty.ENABLED,
                                       Boolean.class,
                                       Boolean.FALSE)
                || environment.acceptsProfiles(ParameterStorePropertySourceConfigurationProperty.ENABLED_PROFILE)
                || (!ObjectUtils.isEmpty(userDefinedEnabledProfiles)
                        && environment.acceptsProfiles(userDefinedEnabledProfiles));
    }

    private boolean doesNotSupportMultipleApplicationContexts(ConfigurableEnvironment environment)
    {
        return !environment.getProperty(ParameterStorePropertySourceConfigurationProperty.SUPPORT_MULTIPLE_APPLICATION_CONTEXTS,
                                        Boolean.class,
                                        Boolean.FALSE);
    }

    private boolean isMultiRegionEnabled(ConfigurableEnvironment environment)
    {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperty.MULTI_REGION_SSM_CLIENT_REGIONS);
    }
}
