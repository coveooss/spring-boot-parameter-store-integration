package com.coveo.configuration.parameterstore.strategy;

import org.springframework.core.env.ConfigurableEnvironment;

public interface ParameterStorePropertySourceConfigurationStrategy
{
    void configureParameterStorePropertySources(ConfigurableEnvironment environment);
}
