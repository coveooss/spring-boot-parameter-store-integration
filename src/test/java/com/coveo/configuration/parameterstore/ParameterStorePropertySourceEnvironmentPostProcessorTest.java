package com.coveo.configuration.parameterstore;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

@ExtendWith(MockitoExtension.class)
public class ParameterStorePropertySourceEnvironmentPostProcessorTest
{
    private static final int SPECIFIC_MAX_ERROR_RETRY = 10;
    private static final String[] EMPTY_CUSTOM_PROFILES = new String[] {};
    private static final String[] CUSTOM_PROFILES = new String[] { "open", "source", "this" };
    private static final int MAX_RETRIES = 3;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private SpringApplication applicationMock;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private ParameterStorePropertySourceConfigurationStrategyFactory strategyFactoryMock;
    @Mock
    private ParameterStorePropertySourceConfigurationStrategy defaultPostProcessStrategyMock;
    @Mock
    private ParameterStorePropertySourceConfigurationStrategy multiRegionPostProcessStrategyMock;
    @Mock
    private RetryStrategy.Builder<?, ?> retryStrategyBuilderMock;

    @Captor
    private ArgumentCaptor<SsmClientBuilder> ssmClientBuilderCaptor;

    private ParameterStorePropertySourceEnvironmentPostProcessor parameterStorePropertySourceEnvironmentPostProcessor = new ParameterStorePropertySourceEnvironmentPostProcessor();

    @BeforeEach
    public void setUp()
    {
        when(strategyFactoryMock.getStrategy(StrategyType.DEFAULT)).thenReturn(defaultPostProcessStrategyMock);
        when(strategyFactoryMock.getStrategy(StrategyType.MULTI_REGION)).thenReturn(multiRegionPostProcessStrategyMock);
        ParameterStorePropertySourceEnvironmentPostProcessor.strategyFactory = strategyFactoryMock;

        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(eq(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY),
                                                     eq(Integer.class),
                                                     any())).thenReturn(MAX_RETRIES);
    }

    @Test
    public void testParameterStoreIsDisabledByDefault()
    {
        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyNoInteractions(applicationMock);
        verifyNoInteractions(defaultPostProcessStrategyMock);
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
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
        ssmClientBuilderCaptor.getValue()
                              .overrideConfiguration()
                              .retryStrategyConfigurator()
                              .orElseThrow()
                              .accept(retryStrategyBuilderMock);
        verify(retryStrategyBuilderMock).maxAttempts(MAX_RETRIES + 1);

        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithProfile()
    {
        when(configurableEnvironmentMock.acceptsProfiles(Profiles.of(ParameterStorePropertySourceConfigurationProperties.ENABLED_PROFILE))).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      any(SsmClientBuilder.class));
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithCustomProfiles()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(Profiles.of(CUSTOM_PROFILES))).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      any(SsmClientBuilder.class));
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesEmpty()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(EMPTY_CUSTOM_PROFILES);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(configurableEnvironmentMock, never()).acceptsProfiles(EMPTY_CUSTOM_PROFILES);
        verifyNoInteractions(defaultPostProcessStrategyMock);
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesButNoneOfTheProfilesActive()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                     String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(false);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyNoInteractions(defaultPostProcessStrategyMock);
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
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
        ssmClientBuilderCaptor.getValue()
                              .overrideConfiguration()
                              .retryStrategyConfigurator()
                              .orElseThrow()
                              .accept(retryStrategyBuilderMock);
        verify(retryStrategyBuilderMock).maxAttempts(MAX_RETRIES + 1);

        verifyNoInteractions(defaultPostProcessStrategyMock);
    }

    @Test
    public void testWhenMaxRetryErrorIsSpecified()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(eq(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY),
                                                     eq(Integer.class),
                                                     any())).thenReturn(SPECIFIC_MAX_ERROR_RETRY);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                                      ssmClientBuilderCaptor.capture());
        ssmClientBuilderCaptor.getValue()
                              .overrideConfiguration()
                              .retryStrategyConfigurator()
                              .orElseThrow()
                              .accept(retryStrategyBuilderMock);
        verify(retryStrategyBuilderMock).maxAttempts(SPECIFIC_MAX_ERROR_RETRY + 1);
    }

    @Test
    public void testWhenMaxRetryErrorIsSpecifiedAndMultiRegionIsEnabled()
    {
        when(configurableEnvironmentMock.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(eq(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY),
                                                     eq(Integer.class),
                                                     any())).thenReturn(SPECIFIC_MAX_ERROR_RETRY);
        when(configurableEnvironmentMock.containsProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(multiRegionPostProcessStrategyMock,
               times(1)).configureParameterStorePropertySources(eq(configurableEnvironmentMock),
                                                                ssmClientBuilderCaptor.capture());
        ssmClientBuilderCaptor.getValue()
                              .overrideConfiguration()
                              .retryStrategyConfigurator()
                              .orElseThrow()
                              .accept(retryStrategyBuilderMock);
        verify(retryStrategyBuilderMock).maxAttempts(SPECIFIC_MAX_ERROR_RETRY + 1);

        verifyNoInteractions(defaultPostProcessStrategyMock);
    }
}
