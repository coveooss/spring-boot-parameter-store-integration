package com.coveo.configuration.parameterstore.strategy;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
import static com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.DEFAULT_STRATEGY;
import static com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl.MULTI_REGION_STRATEGY;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImplTest
{
    private ParameterStorePropertySourceEnvironmentPostProcessStrategyFactory factory;

    @Before
    public void setUp()
    {
        factory = new ParameterStorePropertySourceEnvironmentPostProcessStrategyFactoryImpl();

        System.setProperty(ACCESS_KEY_ENV_VAR, "id");
        System.setProperty(SECRET_KEY_ENV_VAR, "secret");
        System.setProperty(AWS_REGION_SYSTEM_PROPERTY, "region");
    }

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
