package com.coveo.configuration.parameterstore.strategy;

public interface ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory
{
    ParameterStorePropertySourceEnvironmentPostProcessStrategy getStrategy(String type);
}
