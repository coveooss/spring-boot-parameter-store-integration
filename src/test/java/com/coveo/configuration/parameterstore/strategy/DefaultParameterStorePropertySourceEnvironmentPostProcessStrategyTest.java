package com.coveo.configuration.parameterstore.strategy;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import com.amazonaws.regions.AwsRegionProviderChain;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty;

@RunWith(MockitoJUnitRunner.class)
public class DefaultParameterStorePropertySourceEnvironmentPostProcessStrategyTest
{
    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private MutablePropertySources mutablePropertySourcesMock;
    @Mock
    private AwsRegionProviderChain awsRegionProviderChain;

    private DefaultParameterStorePropertySourceEnvironmentPostProcessStrategy postProcessStrategy;

    @Before
    public void setUp()
    {
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperty.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(awsRegionProviderChain.getRegion()).thenReturn("aRegion");
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperty.SSM_CLIENT_SIGNING_REGION,
                                                     awsRegionProviderChain.getRegion())).thenReturn("aRegion");

        postProcessStrategy = new DefaultParameterStorePropertySourceEnvironmentPostProcessStrategy(awsRegionProviderChain);
    }

    @Test
    public void testShouldAddPropertySource()
    {
        postProcessStrategy.postProcess(configurableEnvironmentMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testWhenCustomEndpointShouldAddPropertySource()
    {
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperty.SSM_CLIENT_CUSTOM_ENDPOINT)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperty.SSM_CLIENT_CUSTOM_ENDPOINT)).thenReturn("customEndpoint");

        postProcessStrategy.postProcess(configurableEnvironmentMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }
}
