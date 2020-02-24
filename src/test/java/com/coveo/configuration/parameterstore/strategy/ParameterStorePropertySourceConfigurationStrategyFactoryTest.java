package com.coveo.configuration.parameterstore.strategy;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
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

        System.setProperty(ACCESS_KEY_ENV_VAR, "id");
        System.setProperty(SECRET_KEY_ENV_VAR, "secret");
        System.setProperty(AWS_REGION_SYSTEM_PROPERTY, "region");
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
