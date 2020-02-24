package com.coveo.configuration.parameterstore.strategy;

import java.util.EnumMap;

import com.amazonaws.regions.DefaultAwsRegionProviderChain;

public class ParameterStorePropertySourceConfigurationStrategyFactory
{
    private static EnumMap<StrategyType, ParameterStorePropertySourceConfigurationStrategy> strategies = new EnumMap<>(StrategyType.class);

    static {
        strategies.put(StrategyType.DEFAULT,
                       new DefaultParameterStorePropertySourceConfigurationStrategy(new DefaultAwsRegionProviderChain()));
        strategies.put(StrategyType.MULTI_REGION,
                       new MultiRegionParameterStorePropertySourceConfigurationStrategy());
    }

    public ParameterStorePropertySourceConfigurationStrategy getStrategy(StrategyType strategyType)
    {
        return strategies.get(strategyType);
    }
}
