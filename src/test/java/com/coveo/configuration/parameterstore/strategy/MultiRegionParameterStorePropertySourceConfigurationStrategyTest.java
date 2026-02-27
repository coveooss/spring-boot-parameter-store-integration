package com.coveo.configuration.parameterstore.strategy;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

@ExtendWith(MockitoExtension.class)
public class MultiRegionParameterStorePropertySourceConfigurationStrategyTest
{
    private static final String[] SIGNING_REGIONS = { "ownRegion", "mainRegion", "defaultRegion" };
    private static final String[] SINGLE_SIGNING_REGIONS = { "ownRegion" };

    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private SsmClientBuilder ssmClientBuilderMock;
    @Mock
    private SsmClient ssmClientMock;

    private Map<String, Object> propertyMap;
    private MutablePropertySources propertySources;

    private MultiRegionParameterStorePropertySourceConfigurationStrategy strategy;

    @BeforeEach
    public void setUp()
    {
        propertyMap = new HashMap<>();
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                        String.join(",", SIGNING_REGIONS));
        propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test", propertyMap));
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(propertySources);

        strategy = new MultiRegionParameterStorePropertySourceConfigurationStrategy();
    }

    @Test
    public void testShouldAddPropertySourceForEverySigningRegionsInOrderOfPrecedence()
    {
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        List<ParameterStorePropertySource> added = getAddedParameterStorePropertySources();
        assertThat(added).hasSize(3);
        // addFirst means the last added is first in the list, so the order in propertySources
        // is: ownRegion (highest precedence), mainRegion, defaultRegion (lowest precedence)
        verifyParameterStorePropertySource(added.get(0), SIGNING_REGIONS[0], Boolean.FALSE);
        verifyParameterStorePropertySource(added.get(1), SIGNING_REGIONS[1], Boolean.FALSE);
        verifyParameterStorePropertySource(added.get(2), SIGNING_REGIONS[2], Boolean.FALSE);
    }

    @Test
    public void testHaltBootIsTrueThenOnlyLastRegionShouldHaltBoot()
    {
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT, "true");

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        List<ParameterStorePropertySource> added = getAddedParameterStorePropertySources();
        assertThat(added).hasSize(3);
        // After reversing: defaultRegion is added first (with haltBoot), then mainRegion, then ownRegion
        // In the propertySources list (addFirst ordering), ownRegion is first (highest precedence)
        verifyParameterStorePropertySource(added.get(0), SIGNING_REGIONS[0], Boolean.FALSE);
        verifyParameterStorePropertySource(added.get(1), SIGNING_REGIONS[1], Boolean.FALSE);
        verifyParameterStorePropertySource(added.get(2), SIGNING_REGIONS[2], Boolean.TRUE);
    }

    @Test
    public void testWithSingleRegion()
    {
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT, "true");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                        String.join(",", SINGLE_SIGNING_REGIONS));

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        List<ParameterStorePropertySource> added = getAddedParameterStorePropertySources();
        assertThat(added).hasSize(1);
        verifyParameterStorePropertySource(added.get(0), SINGLE_SIGNING_REGIONS[0], Boolean.TRUE);
    }

    @Test
    public void testShouldThrowWhenRegionsIsEmpty()
    {
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS, "");

        assertThrows(IllegalArgumentException.class,
                     () -> strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                                           ssmClientBuilderMock));
    }

    @Test
    public void testShouldThrowWhenRegionsIsNotSet()
    {
        propertyMap.remove(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS);

        assertThrows(IllegalArgumentException.class,
                     () -> strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                                           ssmClientBuilderMock));
    }

    private List<ParameterStorePropertySource> getAddedParameterStorePropertySources()
    {
        List<ParameterStorePropertySource> result = new ArrayList<>();
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof ParameterStorePropertySource) {
                result.add((ParameterStorePropertySource) propertySource);
            }
        }
        return result;
    }

    private void verifyParameterStorePropertySource(ParameterStorePropertySource actual,
                                                    String region,
                                                    Boolean haltBoot)
    {
        assertThat(actual.getName()).endsWith("_" + region);
        assertThat(ReflectionTestUtils.getField(actual.getSource(), "haltBoot")).isEqualTo(haltBoot);
    }
}
