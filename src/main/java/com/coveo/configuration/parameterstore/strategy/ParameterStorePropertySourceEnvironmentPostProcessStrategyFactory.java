package com.coveo.configuration.parameterstore.strategy;

import java.util.EnumMap;

import com.amazonaws.regions.DefaultAwsRegionProviderChain;

public class ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory
{
    private static EnumMap<StrategyType, ParameterStorePropertySourceEnvironmentPostProcessStrategy> strategies = new EnumMap<>(StrategyType.class);

    static {
        strategies.put(StrategyType.DEFAULT,
                       new DefaultParameterStorePropertySourceEnvironmentPostProcessStrategy(new DefaultAwsRegionProviderChain()));
        strategies.put(StrategyType.MULTI_REGION,
                       new MultiRegionParameterStorePropertySourceEnvironmentPostProcessStrategy());
    }

    public ParameterStorePropertySourceEnvironmentPostProcessStrategy getStrategy(StrategyType strategyType)
    {
        return strategies.get(strategyType);
    }
}
