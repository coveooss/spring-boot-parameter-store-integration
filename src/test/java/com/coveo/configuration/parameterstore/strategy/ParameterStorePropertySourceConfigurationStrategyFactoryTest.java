package com.coveo.configuration.parameterstore.strategy;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParameterStorePropertySourceConfigurationStrategyFactoryTest
{
    private ParameterStorePropertySourceConfigurationStrategyFactory factory;

    @BeforeEach
    public void setUp()
    {
        factory = new ParameterStorePropertySourceConfigurationStrategyFactory();
    }

    @Test
    public void testGettingDefaultStrategy()
    {
        assertThat(factory.getStrategy(StrategyType.DEFAULT)).isInstanceOf(DefaultParameterStorePropertySourceConfigurationStrategy.class);
    }

    @Test
    public void testGettingMultiRegionStrategy()
    {
        assertThat(factory.getStrategy(StrategyType.MULTI_REGION)).isInstanceOf(MultiRegionParameterStorePropertySourceConfigurationStrategy.class);
    }
}
