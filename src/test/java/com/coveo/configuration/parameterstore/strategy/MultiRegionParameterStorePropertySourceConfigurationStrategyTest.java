package com.coveo.configuration.parameterstore.strategy;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
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
    private static final String[] EMPTY_REGIONS = {};

    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private MutablePropertySources mutablePropertySourcesMock;
    @Mock
    private SsmClientBuilder ssmClientBuilderMock;
    @Mock
    private SsmClient ssmClientMock;

    @Captor
    private ArgumentCaptor<ParameterStorePropertySource> parameterStorePropertySourceArgumentCaptor;

    private MultiRegionParameterStorePropertySourceConfigurationStrategy strategy;

    @BeforeEach
    public void setUp()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                     String[].class)).thenReturn(SIGNING_REGIONS);

        strategy = new MultiRegionParameterStorePropertySourceConfigurationStrategy();
    }

    @Test
    public void testShouldAddPropertySourceForEverySigningRegionsInOrderOfPrecedence()
    {
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        verify(mutablePropertySourcesMock, times(3)).addFirst(parameterStorePropertySourceArgumentCaptor.capture());

        List<ParameterStorePropertySource> propertySources = parameterStorePropertySourceArgumentCaptor.getAllValues();
        verifyParameterStorePropertySource(propertySources.get(0), SIGNING_REGIONS[0], Boolean.FALSE);
        verifyParameterStorePropertySource(propertySources.get(1), SIGNING_REGIONS[1], Boolean.FALSE);
        verifyParameterStorePropertySource(propertySources.get(2), SIGNING_REGIONS[2], Boolean.FALSE);
    }

    @Test
    public void testHaltBootIsTrueThenOnlyLastRegionShouldHaltBoot()
    {
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        verify(mutablePropertySourcesMock, times(3)).addFirst(parameterStorePropertySourceArgumentCaptor.capture());

        List<ParameterStorePropertySource> propertySources = parameterStorePropertySourceArgumentCaptor.getAllValues();
        verifyParameterStorePropertySource(propertySources.get(0), SIGNING_REGIONS[0], Boolean.TRUE);
        verifyParameterStorePropertySource(propertySources.get(1), SIGNING_REGIONS[1], Boolean.FALSE);
        verifyParameterStorePropertySource(propertySources.get(2), SIGNING_REGIONS[2], Boolean.FALSE);
    }

    @Test
    public void testWithSingleRegion()
    {
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
        when(ssmClientBuilderMock.region(any())).thenReturn(ssmClientBuilderMock);
        when(ssmClientBuilderMock.build()).thenReturn(ssmClientMock);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                     String[].class)).thenReturn(SINGLE_SIGNING_REGIONS);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock, ssmClientBuilderMock);

        verify(mutablePropertySourcesMock).addFirst(parameterStorePropertySourceArgumentCaptor.capture());
        verifyParameterStorePropertySource(parameterStorePropertySourceArgumentCaptor.getValue(),
                                           SINGLE_SIGNING_REGIONS[0],
                                           Boolean.TRUE);
    }

    @Test
    public void testShouldThrowWhenRegionsIsEmpty()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                     String[].class)).thenReturn(EMPTY_REGIONS);

        assertThrows(IllegalArgumentException.class,
                     () -> strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                                           ssmClientBuilderMock));
    }

    private void verifyParameterStorePropertySource(ParameterStorePropertySource actual,
                                                    String region,
                                                    Boolean haltBoot)
    {
        assertThat(actual.getName()).endsWith("_" + region);
        assertThat(ReflectionTestUtils.getField(actual.getSource(), "haltBoot")).isEqualTo(haltBoot);
    }
}
