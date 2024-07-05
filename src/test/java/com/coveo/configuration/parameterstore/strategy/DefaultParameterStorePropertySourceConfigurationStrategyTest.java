package com.coveo.configuration.parameterstore.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

@ExtendWith(MockitoExtension.class)
public class DefaultParameterStorePropertySourceConfigurationStrategyTest
{
    private static final Region PROVIDER_CHAIN_REGION = Region.US_EAST_1;
    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private MutablePropertySources mutablePropertySourcesMock;
    @Mock
    private AwsRegionProviderChain awsRegionProviderChain;
    @Mock
    private SsmClientBuilder ssmClientBuilderMock;
    @Mock
    private SsmClient ssmClientMock;

    private DefaultParameterStorePropertySourceConfigurationStrategy strategy;

    @BeforeEach
    public void setUp()
    {
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);

        strategy = new DefaultParameterStorePropertySourceConfigurationStrategy(awsRegionProviderChain);
    }

    @Test
    public void testShouldAddPropertySource()
    {
        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testWhenCustomEndpointShouldAddPropertySource()
    {
        when(ssmClientBuilderMock.endpointOverride(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(awsRegionProviderChain.getRegion()).thenReturn(PROVIDER_CHAIN_REGION);
        when(configurableEnvironmentMock.getProperty(eq(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_SIGNING_REGION),
                                                     any(String.class))).thenReturn(PROVIDER_CHAIN_REGION.toString());
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT)).thenReturn("customEndpoint");

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }
}
