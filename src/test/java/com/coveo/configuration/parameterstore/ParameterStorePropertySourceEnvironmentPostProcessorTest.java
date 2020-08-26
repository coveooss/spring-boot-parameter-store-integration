package com.coveo.configuration.parameterstore;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStorePropertySourceEnvironmentPostProcessorTest
{
    private static final int SPECIFIC_MAX_ERROR_RETRY = 10;
    private static final String[] EMPTY_CUSTOM_PROFILES = new String[] {};
    private static final String[] CUSTOM_PROFILES = new String[] { "open", "source", "this" };

    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private SpringApplication applicationMock;
    @Mock
    private ParameterStorePropertySourceConfigurationStrategyFactory strategyFactoryMock;
    @Mock
    private ParameterStorePropertySourceConfigurationStrategy defaultPostProcessStrategyMock;
    @Mock
    private ParameterStorePropertySourceConfigurationStrategy multiRegionPostProcessStrategyMock;

    @Captor
    private ArgumentCaptor<AWSSimpleSystemsManagementClientBuilder> ssmClientBuilderCaptor;

    private ParameterStorePropertySourceEnvironmentPostProcessor parameterStorePropertySourceEnvironmentPostProcessor = new ParameterStorePropertySourceEnvironmentPostProcessor();

    @Before
    public void setUp()
    {
        when(strategyFactoryMock.getStrategy(StrategyType.DEFAULT)).thenReturn(defaultPostProcessStrategyMock);
        when(strategyFactoryMock.getStrategy(StrategyType.MULTI_REGION)).thenReturn(multiRegionPostProcessStrategyMock);
        ParameterStorePropertySourceEnvironmentPostProcessor.strategyFactory = strategyFactoryMock;

        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY,
                                                     Integer.class,
                                                     PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY)).thenReturn(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY);

        System.setProperty(ACCESS_KEY_ENV_VAR, "id");
        System.setProperty(SECRET_KEY_ENV_VAR, "secret");
        System.setProperty(AWS_REGION_SYSTEM_PROPERTY, "region");
    }

    @Test
    public void testParameterStoreIsDisabledByDefault()
    {
        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(applicationMock);
        verifyZeroInteractions(defaultPostProcessStrategyMock);
        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithPropertySetToTrue()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      ssmClientBuilderCaptor.capture());
        assertThat(ssmClientBuilderCaptor.getValue().getClientConfiguration().getRetryPolicy().getMaxErrorRetry(),
                   is(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY));

        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithProfile()
    {
        when(configurableEnvironmentMock.acceptsProfiles(ParameterStorePropertySourceConfigurationProperties.ENABLED_PROFILE)).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      any(AWSSimpleSystemsManagementClientBuilder.class));
        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithCustomProfiles()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      any(AWSSimpleSystemsManagementClientBuilder.class));
        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesEmpty()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(EMPTY_CUSTOM_PROFILES);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(configurableEnvironmentMock, never()).acceptsProfiles(EMPTY_CUSTOM_PROFILES);
        verifyZeroInteractions(defaultPostProcessStrategyMock);
        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesButNoneOfTheProfilesActive()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(false);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(defaultPostProcessStrategyMock);
        verifyZeroInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testWhenMultiRegionIsEnabled()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(multiRegionPostProcessStrategyMock,
               times(1)).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                ssmClientBuilderCaptor.capture());
        assertThat(ssmClientBuilderCaptor.getValue().getClientConfiguration().getRetryPolicy().getMaxErrorRetry(),
                   is(PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY));

        verifyZeroInteractions(defaultPostProcessStrategyMock);
    }

    @Test
    public void testWhenMaxRetryErrorIsSpecified()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY,
                                                     Integer.class,
                                                     PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY)).thenReturn(SPECIFIC_MAX_ERROR_RETRY);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      ssmClientBuilderCaptor.capture());
        assertThat(ssmClientBuilderCaptor.getValue().getClientConfiguration().getRetryPolicy().getMaxErrorRetry(),
                   is(SPECIFIC_MAX_ERROR_RETRY));
    }

    @Test
    public void testWhenMaxRetryErrorIsSpecifiedAndMultiRegionIsEnabled()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY,
                                                     Integer.class,
                                                     PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY)).thenReturn(SPECIFIC_MAX_ERROR_RETRY);
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(multiRegionPostProcessStrategyMock,
               times(1)).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                ssmClientBuilderCaptor.capture());
        assertThat(ssmClientBuilderCaptor.getValue().getClientConfiguration().getRetryPolicy().getMaxErrorRetry(),
                   is(SPECIFIC_MAX_ERROR_RETRY));

        verifyZeroInteractions(defaultPostProcessStrategyMock);
    }
}
