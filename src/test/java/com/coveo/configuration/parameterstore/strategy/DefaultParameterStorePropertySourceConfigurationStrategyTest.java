package com.coveo.configuration.parameterstore.strategy;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

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
    private AwsRegionProviderChain awsRegionProviderChain;
    @Mock
    private SsmClientBuilder ssmClientBuilderMock;
    @Mock
    private SsmClient ssmClientMock;

    private Map<String, Object> propertyMap;
    private MutablePropertySources propertySources;

    private DefaultParameterStorePropertySourceConfigurationStrategy strategy;

    @BeforeEach
    public void setUp()
    {
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);

        propertyMap = new HashMap<>();
        propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test", propertyMap));
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(propertySources);

        strategy = new DefaultParameterStorePropertySourceConfigurationStrategy(awsRegionProviderChain);
    }

    @Test
    public void testShouldAddPropertySource()
    {
        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        assertPropertySourceAdded();
    }

    @Test
    public void testWhenCustomEndpointShouldAddPropertySource()
    {
        when(ssmClientBuilderMock.endpointOverride(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(awsRegionProviderChain.getRegion()).thenReturn(PROVIDER_CHAIN_REGION);

        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_CUSTOM_ENDPOINT,
                        "customEndpoint");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_SIGNING_REGION,
                        PROVIDER_CHAIN_REGION.toString());

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        assertPropertySourceAdded();
    }

    private void assertPropertySourceAdded()
    {
        boolean found = false;
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof ParameterStorePropertySource) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
