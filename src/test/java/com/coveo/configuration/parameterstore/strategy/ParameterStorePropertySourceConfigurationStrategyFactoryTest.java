package com.coveo.configuration.parameterstore.strategy;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParameterStorePropertySourceConfigurationStrategyFactoryTest
{
    private ParameterStorePropertySourceConfigurationStrategyFactory factory;

    @BeforeEach
    void setUp()
    {
        factory = new ParameterStorePropertySourceConfigurationStrategyFactory();
    }

    @Test
    void testGettingDefaultStrategy()
    {
        assertThat(factory.getStrategy(StrategyType.DEFAULT)).isInstanceOf(DefaultParameterStorePropertySourceConfigurationStrategy.class);
    }

    @Test
    void testGettingMultiRegionStrategy()
    {
        assertThat(factory.getStrategy(StrategyType.MULTI_REGION)).isInstanceOf(MultiRegionParameterStorePropertySourceConfigurationStrategy.class);
    }
}
