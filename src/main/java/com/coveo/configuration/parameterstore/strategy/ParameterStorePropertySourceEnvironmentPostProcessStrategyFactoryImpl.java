package com.coveo.configuration.parameterstore.strategy;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.regions.DefaultAwsRegionProviderChain;

public class ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl
        implements ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory
{
    public static final String DEFAULT_STRATEGY = "Default";
    public static final String MULTI_REGION_STRATEGY = "MultiRegion";

    private static Map<String, ParameterStorePropertySourceEnvironmentPostProcessStrategy> strategies = new HashMap<>();

    static {
        strategies.put(DEFAULT_STRATEGY,
                       new DefaultParameterStorePropertySourceEnvironmentPostProcessStrategy(new DefaultAwsRegionProviderChain()));
        strategies.put(MULTI_REGION_STRATEGY,
                       new MultiRegionParameterStorePropertySourceEnvironmentPostProcessStrategy());
    }

    @Override
    public ParameterStorePropertySourceEnvironmentPostProcessStrategy getStrategy(String type)
    {
        return strategies.get(type);
    }
}
