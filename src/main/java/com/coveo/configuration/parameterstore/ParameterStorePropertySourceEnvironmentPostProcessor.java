package com.coveo.configuration.parameterstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor
{
    static final String PARAMETER_STORE_ACCEPTED_PROFILE = "awsParameterStorePropertySourceEnabled";

    static final String PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.enabledProfiles";
    static final String PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.enabled";
    static final String PARAMETER_STORE_HALT_BOOT_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.haltBoot";
    static final String PARAMETER_STORE_CLIENT_ENDPOINT_CONFIGURATION_PROPERTY = "awsParameterStoreSource.ssmClient.endpointConfiguration.endpoint";
    static final String PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGION_CONFIGURATION_PROPERTY = "awsParameterStoreSource.ssmClient.endpointConfiguration.signingRegion";

    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "AWSParameterStorePropertySource";

    static boolean initialized;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {
            environment.getPropertySources()
                       .addFirst(new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME,
                                                                  new ParameterStoreSource(buildAWSSimpleSystemsManagementClient(environment),
                                                                                           environment.getProperty(PARAMETER_STORE_HALT_BOOT_CONFIGURATION_PROPERTY,
                                                                                                                   Boolean.class,
                                                                                                                   Boolean.FALSE))));
            initialized = true;
        }
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment)
    {
        String[] userDefinedEnabledProfiles = environment.getProperty(PARAMETER_STORE_ACCEPTED_PROFILES_CONFIGURATION_PROPERTY,
                                                                      String[].class);
        return environment.getProperty(PARAMETER_STORE_ENABLED_CONFIGURATION_PROPERTY, Boolean.class, Boolean.FALSE)
                || environment.acceptsProfiles(PARAMETER_STORE_ACCEPTED_PROFILE)
                || (!ObjectUtils.isEmpty(userDefinedEnabledProfiles)
                        && environment.acceptsProfiles(userDefinedEnabledProfiles));
    }

    private AWSSimpleSystemsManagement buildAWSSimpleSystemsManagementClient(ConfigurableEnvironment environment)
    {
        if (environment.containsProperty(PARAMETER_STORE_CLIENT_ENDPOINT_CONFIGURATION_PROPERTY)) {
            return AWSSimpleSystemsManagementClientBuilder.standard()
                                                          .withEndpointConfiguration(new EndpointConfiguration(environment.getProperty(PARAMETER_STORE_CLIENT_ENDPOINT_CONFIGURATION_PROPERTY),
                                                                                                               environment.getProperty(PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGION_CONFIGURATION_PROPERTY,
                                                                                                                                       new DefaultAwsRegionProviderChain().getRegion())))
                                                          .build();
        }
        return AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }
}
