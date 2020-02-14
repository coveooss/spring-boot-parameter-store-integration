package com.coveo.configuration.parameterstore;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_ACCEPTED_PROFILE;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGIONS_CONFIGURATION_PROPERTY;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_HALT_BOOT_CONFIGURATION_PROPERTY;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_MULTI_REGION_ENABLED_CONFIGURATION_PROPERTY;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceEnvironmentPostProcessor.PARAMETER_STORE_SUPPORT_MULTIPLE_APPLICATION_CONTEXTS_CONFIGURATION_PROPERTY;
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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStorePropertySourceEnvironmentPostProcessorTest
{
    private static final String[] EMPTY_CUSTOM_PROFILES = new String[] {};
    private static final String[] CUSTOM_PROFILES = new String[] { "open", "source", "this" };

    @Mock
    private ConfigurableEnvironment configurableEnvironmentMock;
    @Mock
    private MutablePropertySources mutablePropertySourcesMock;
    @Mock
    private SpringApplication applicationMock;

    private ParameterStorePropertySourceEnvironmentPostProcessor parameterStorePropertySourceEnvironmentPostProcessor = new ParameterStorePropertySourceEnvironmentPostProcessor();

    @Before
    public void setUp()
    {
        ParameterStorePropertySourceEnvironmentPostProcessor.initialized = false;

        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_HALT_BOOT_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_SUPPORT_MULTIPLE_APPLICATION_CONTEXTS_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_MULTI_REGION_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.FALSE);
        when(configurableEnvironmentMock.getPropertySources()).thenReturn(mutablePropertySourcesMock);

        System.setProperty(ACCESS_KEY_ENV_VAR, "id");
        System.setProperty(SECRET_KEY_ENV_VAR, "secret");
        System.setProperty(AWS_REGION_SYSTEM_PROPERTY, "region");
    }

    @Test
    public void testParameterStoreIsDisabledByDefault()
    {
        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStoreIsEnabledWithPropertySetToTrue()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsEnabledWithProfile()
    {
        when(configurableEnvironmentMock.acceptsProfiles(PARAMETER_STORE_ACCEPTED_PROFILE)).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsEnabledWithCustomProfiles()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY,
                                                     String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(true);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(mutablePropertySourcesMock).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesEmpty()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY,
                                                     String[].class)).thenReturn(EMPTY_CUSTOM_PROFILES);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(configurableEnvironmentMock, never()).acceptsProfiles(EMPTY_CUSTOM_PROFILES);
        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStoreIsNotEnabledWithCustomProfilesButNoneOfTheProfilesActive()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY,
                                                     String[].class)).thenReturn(CUSTOM_PROFILES);
        when(configurableEnvironmentMock.acceptsProfiles(CUSTOM_PROFILES)).thenReturn(false);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verifyZeroInteractions(mutablePropertySourcesMock);
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCantBeCalledTwice()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(mutablePropertySourcesMock, times(1)).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testParameterStorePropertySourceEnvironmentPostProcessorCanBeCalledTwiceWhenDisablingMultipleContextSupport()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_SUPPORT_MULTIPLE_APPLICATION_CONTEXTS_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(mutablePropertySourcesMock, times(2)).addFirst(any(ParameterStorePropertySource.class));
    }

    @Test
    public void testWhenMultiRegionIsEnabled()
    {
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(PARAMETER_STORE_MULTI_REGION_ENABLED_CONFIGURATION_PROPERTY,
                                                     Boolean.class,
                                                     Boolean.FALSE)).thenReturn(Boolean.TRUE);
        when(configurableEnvironmentMock.getProperty(eq(PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGIONS_CONFIGURATION_PROPERTY),
                                                     any(),
                                                     any())).thenReturn(new String[] { "region-1", "region-2" });

        parameterStorePropertySourceEnvironmentPostProcessor.postProcessEnvironment(configurableEnvironmentMock,
                                                                                    applicationMock);

        verify(mutablePropertySourcesMock, times(2)).addFirst(any(ParameterStorePropertySource.class));
    }
}
