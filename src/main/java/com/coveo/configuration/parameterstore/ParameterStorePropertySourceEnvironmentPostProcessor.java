package com.coveo.configuration.parameterstore;

import static com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.DEFAULT_STRATEGY;
import static com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.MULTI_REGION_STRATEGY;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor
{
    static boolean initialized;
    static ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory postProcessStrategyFactory = new ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl();

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
        String type = isMultiRegionEnabled(environment) ? MULTI_REGION_STRATEGY : DEFAULT_STRATEGY;
        return postProcessStrategyFactory.getStrategy(type);
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment)
    {
        String[] userDefinedEnabledProfiles = environment.getProperty(ParameterStorePropertySourceConfigurationProperty.ACCEPTED_PROFILES,
                                                                      String[].class);
        return environment.getProperty(ParameterStorePropertySourceConfigurationProperty.ENABLED,
                                       Boolean.class,
                                       Boolean.FALSE)
                || environment.acceptsProfiles(ParameterStorePropertySourceConfigurationProperty.ACCEPTED_PROFILE)
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
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperty.SSM_CLIENT_SIGNING_REGIONS);
    }
}
