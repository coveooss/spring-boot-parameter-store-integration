package com.coveo.configuration.parameterstore.strategy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.util.ReflectionTestUtils;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import software.amazon.awssdk.services.ssm.SsmClient;

@RunWith(MockitoJUnitRunner.class)
public class MultiRegionParameterStorePropertySourceConfigurationStrategyTest
{
    private static final String[] SIGNING_REGIONS = { "ownRegion", "mainRegion", "defaultRegion" };
    private static final String[] SINGLE_SIGNING_REGIONS = { "ownRegion" };
    private static final String[] EMPTY_REGIONS = {};

    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private MutablePropertySources mutablePropertySourcesMock;

    @Captor
    private ArgumentCaptor<ParameterStorePropertySource> parameterStorePropertySourceArgumentCaptor;

    private MultiRegionParameterStorePropertySourceConfigurationStrategy strategy;

    @Before
    public void setUp()
    {
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);
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
        strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                SsmClient.builder());

        verify(mutablePropertySourcesMock, times(3)).addFirst(parameterStorePropertySourceArgumentCaptor.capture());

        List<ParameterStorePropertySource> propertySources = parameterStorePropertySourceArgumentCaptor.getAllValues();
        verifyParameterStorePropertySource(propertySources.get(0), SIGNING_REGIONS[0], Boolean.FALSE);
        verifyParameterStorePropertySource(propertySources.get(1), SIGNING_REGIONS[1], Boolean.FALSE);
        verifyParameterStorePropertySource(propertySources.get(2), SIGNING_REGIONS[2], Boolean.FALSE);
    }

    @Test
    public void testHaltBootIsTrueThenOnlyLastRegionShouldHaltBoot()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                        SsmClient.builder());

        verify(mutablePropertySourcesMock, times(3)).addFirst(parameterStorePropertySourceArgumentCaptor.capture());

        List<ParameterStorePropertySource> propertySources = parameterStorePropertySourceArgumentCaptor.getAllValues();
        verifyParameterStorePropertySource(propertySources.get(0), SIGNING_REGIONS[0], Boolean.TRUE);
        verifyParameterStorePropertySource(propertySources.get(1), SIGNING_REGIONS[1], Boolean.FALSE);
        verifyParameterStorePropertySource(propertySources.get(2), SIGNING_REGIONS[2], Boolean.FALSE);
    }

    @Test
    public void testWithSingleRegion()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                     String[].class)).thenReturn(SINGLE_SIGNING_REGIONS);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                        SsmClient.builder());

        verify(mutablePropertySourcesMock).addFirst(parameterStorePropertySourceArgumentCaptor.capture());
        verifyParameterStorePropertySource(parameterStorePropertySourceArgumentCaptor.getValue(),
                                           SINGLE_SIGNING_REGIONS[0],
                                           Boolean.TRUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShouldThrowWhenRegionsIsEmpty()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                     String[].class)).thenReturn(EMPTY_REGIONS);

        strategy.configureParameterStorePropertySources(configurableEnvironmentMock,
                                                        SsmClient.builder());
    }

    private void verifyParameterStorePropertySource(ParameterStorePropertySource actual,
                                                    String region,
                                                    Boolean haltBoot)
    {
        assertThat(actual.getName(), endsWith("_" + region));
        assertThat(ReflectionTestUtils.getField(actual.getSource(), "haltBoot"), is(haltBoot));
    }
}
