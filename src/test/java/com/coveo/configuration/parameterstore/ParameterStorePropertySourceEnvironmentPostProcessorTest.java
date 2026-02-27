package com.coveo.configuration.parameterstore;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

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

    private Map<String, Object> propertyMap;
    private MutablePropertySources propertySources;

    @BeforeEach
    public void setUp()
    {
        when(strategyFactoryMock.getStrategy(StrategyType.DEFAULT)).thenReturn(defaultPostProcessStrategyMock);
        when(strategyFactoryMock.getStrategy(StrategyType.MULTI_REGION)).thenReturn(multiRegionPostProcessStrategyMock);
        ParameterStorePropertySourceEnvironmentPostProcessor.strategyFactory = strategyFactoryMock;

        propertyMap = new HashMap<>();
        propertySources = new MutablePropertySources();
        propertySources.addFirst(new MapPropertySource("test", propertyMap));
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(propertySources);
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
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.ENABLED, "true");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY, MAX_RETRIES);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(propertySources),
                                                                                      any(Binder.class),
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

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(propertySources),
                                                                                      any(Binder.class),
                                                                                      any(SsmClientBuilder.class));
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithCustomProfiles()
    {
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES, "open,source,this");
        when(configurableEnvironmentMock.acceptsProfiles(Profiles.of(CUSTOM_PROFILES))).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(propertySources),
                                                                                      any(Binder.class),
                                                                                      any(SsmClientBuilder.class));
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesEmpty()
    {
        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyNoInteractions(defaultPostProcessStrategyMock);
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesButNoneOfTheProfilesActive()
    {
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES, "open,source,this");
        when(configurableEnvironmentMock.acceptsProfiles(Profiles.of(CUSTOM_PROFILES))).thenReturn(false);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyNoInteractions(defaultPostProcessStrategyMock);
        verifyNoInteractions(multiRegionPostProcessStrategyMock);
    }

    @Test
    public void testWhenMultiRegionIsEnabled()
    {
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.ENABLED, "true");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                        "us-east-1,us-east-2");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY, MAX_RETRIES);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(multiRegionPostProcessStrategyMock,
               times(1)).configureParameterStorePropertySources(eq(propertySources),
                                                                any(Binder.class),
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
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.ENABLED, "true");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY, SPECIFIC_MAX_ERROR_RETRY);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(defaultPostProcessStrategyMock).configureParameterStorePropertySources(eq(propertySources),
                                                                                      any(Binder.class),
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
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.ENABLED, "true");
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY, SPECIFIC_MAX_ERROR_RETRY);
        propertyMap.put(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                        "us-east-1,us-east-2");

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(multiRegionPostProcessStrategyMock,
               times(1)).configureParameterStorePropertySources(eq(propertySources),
                                                                any(Binder.class),
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
