package com.coveo.configuration.parameterstore.strategy;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ParameterStorePropertySourceConfigurationStrategyFactoryTest
{
    private ParameterStorePropertySourceConfigurationStrategyFactory factory;

    @Before
    public void setUp()
    {
        factory = new ParameterStorePropertySourceConfigurationStrategyFactory();

        System.setProperty("AWS_ACCESS_KEY_ID", "id");
        System.setProperty("AWS_SECRET_KEY", "secret");
        System.setProperty("aws.region", "region");
    }

    @Test
    public void testGettingDefaultStrategy()
    {
        assertThat(factory.getStrategy(StrategyType.DEFAULT),
                   is(instanceOf(DefaultParameterStorePropertySourceConfigurationStrategy.class)));
    }

    @Test
    public void testGettingMultiRegionStrategy()
    {
        assertThat(factory.getStrategy(StrategyType.MULTI_REGION),
                   is(instanceOf(MultiRegionParameterStorePropertySourceConfigurationStrategy.class)));
    }
}
