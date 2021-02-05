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

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.services.ssm.SsmClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultParameterStorePropertySourceConfigurationStrategyTest
{
    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private MutablePropertySources mutablePropertySourcesMock;
    @Mock
    private AwsRegionProviderChain awsRegionProviderChain;

    private DefaultParameterStorePropertySourceConfigurationStrategy strategy;

    @Before
    public void setUp()
    {
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(awsRegionProviderChain.getRegion()).thenReturn(Region.of("aRegion"));
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_SIGNING_REGION)).thenReturn("aRegion");

        strategy = new DefaultParameterStorePropertySourceConfigurationStrategy(awsRegionProviderChain);
    }

    @Test
    public void testShouldAddPropertySource()
    {
        strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                        SsmClient.builder());

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testWhenCustomEndpointShouldAddPropertySource()
    {
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT)).thenReturn("customEndpoint");

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                        SsmClient.builder());

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }
}
