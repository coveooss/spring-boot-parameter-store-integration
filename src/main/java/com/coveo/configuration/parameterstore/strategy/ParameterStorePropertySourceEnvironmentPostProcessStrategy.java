package com.coveo.configuration.parameterstore.strategy;

import org.springframework.core.env.ConfigurableEnvironment;

public interface ParameterStorePropertySourceEnvironmentPostProcessStrategy
{
    void postProcess(ConfigurableEnvironment environment);
}
