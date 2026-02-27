package com.coveo.configuration.parameterstore.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

        propertyMap.put("aws-parameter-store-source.ssm-client.endpoint-configuration.endpoint", "customEndpoint");
        propertyMap.put("aws-parameter-store-source.ssm-client.endpoint-configuration.signing-region",
                        PROVIDER_CHAIN_REGION.toString());

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        assertPropertySourceAdded();
    }

    private void assertPropertySourceAdded()
    {
        // The strategy adds property sources via addFirst on the real MutablePropertySources,
        // so we verify by checking the property sources contain the expected entry
        boolean found = false;
        for (org.springframework.core.env.PropertySource<?> ps : propertySources) {
            if (ps instanceof ParameterStorePropertySource) {
                found = true;
                break;
            }
        }
        assert found : "Expected a ParameterStorePropertySource to be added";
    }
}
