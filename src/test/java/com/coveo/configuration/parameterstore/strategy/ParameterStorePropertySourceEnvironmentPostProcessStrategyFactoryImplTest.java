package com.coveo.configuration.parameterstore.strategy;

import static com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.DEFAULT_STRATEGY;
import static com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.MULTI_REGION_STRATEGY;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImplTest
{
    private ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory factory = new ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl();

    @Test
    public void testGettingDefaultStrategy()
    {
        assertThat(factory.getStrategy(DEFAULT_STRATEGY),
                   is(instanceOf(DefaultParameterStorePropertySourceEnvironmentPostProcessStrategy.class)));
    }

    @Test
    public void testGettingMultiRegionStrategy()
    {
        assertThat(factory.getStrategy(MULTI_REGION_STRATEGY),
                   is(instanceOf(MultiRegionParameterStorePropertySourceEnvironmentPostProcessStrategy.class)));
    }
}
