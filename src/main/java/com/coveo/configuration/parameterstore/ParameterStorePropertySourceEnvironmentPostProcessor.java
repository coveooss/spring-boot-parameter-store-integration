package com.coveo.configuration.parameterstore;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

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
    static final String PARAMETER_STORE_MULTI_REGION_ENABLED_CONFIGURATION_PROPERTY = "awsParameterStoreSource.ssmClient.multiRegion.enabled";
    static final String PARAMETER_STORE_CLIENT_ENDPOINT_CONFIGURATION_PROPERTY = "awsParameterStoreSource.ssmClient.endpointConfiguration.endpoint";
    static final String PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGION_CONFIGURATION_PROPERTY = "awsParameterStoreSource.ssmClient.endpointConfiguration.signingRegion";
    static final String PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGIONS_CONFIGURATION_PROPERTY = "awsParameterStoreSource.ssmClient.endpointConfiguration.signingRegions";
    static final String PARAMETER_STORE_SUPPORT_MULTIPLE_APPLICATION_CONTEXTS_CONFIGURATION_PROPERTY = "awsParameterStorePropertySource.supportMultipleApplicationContexts";

    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "AWSParameterStorePropertySource";

    static boolean initialized;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (!initialized && isParameterStorePropertySourceEnabled(environment)) {
            boolean haltBoot = environment.getProperty(PARAMETER_STORE_HALT_BOOT_CONFIGURATION_PROPERTY,
                                                       Boolean.class,
                                                       Boolean.FALSE);
            if (isMultiRegionEnabled(environment)) {
                List<String> signingRegions = getSigningRegions(environment);
                ListIterator<String> regionsIterator = signingRegions.listIterator(signingRegions.size());

                while (regionsIterator.hasPrevious()) {
                    boolean isLast = regionsIterator.hasNext();
                    String region = regionsIterator.previous();

                    environment.getPropertySources()
                               .addFirst(buildParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME
                                       + region, buildSSMClient(region), isLast ? false : haltBoot));
                }
            } else {
                environment.getPropertySources()
                           .addFirst(buildParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME,
                                                                       buildSSMClient(environment),
                                                                       haltBoot));
            }

            if (doesNotSupportMultipleApplicationContexts(environment)) {
                initialized = true;
            }
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

    private boolean doesNotSupportMultipleApplicationContexts(ConfigurableEnvironment environment)
    {
        return !environment.getProperty(PARAMETER_STORE_SUPPORT_MULTIPLE_APPLICATION_CONTEXTS_CONFIGURATION_PROPERTY,
                                        Boolean.class,
                                        Boolean.FALSE);
    }

    private boolean isMultiRegionEnabled(ConfigurableEnvironment environment)
    {
        return environment.getProperty(PARAMETER_STORE_MULTI_REGION_ENABLED_CONFIGURATION_PROPERTY,
                                       Boolean.class,
                                       Boolean.FALSE);
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(String name,
                                                                           AWSSimpleSystemsManagement ssmClient,
                                                                           boolean haltBoot)
    {
        return new ParameterStorePropertySource(name, new ParameterStoreSource(ssmClient, haltBoot));
    }

    private AWSSimpleSystemsManagement buildSSMClient(ConfigurableEnvironment environment)
    {
        if (environment.containsProperty(PARAMETER_STORE_CLIENT_ENDPOINT_CONFIGURATION_PROPERTY)) {
            return AWSSimpleSystemsManagementClientBuilder.standard()
                                                          .withEndpointConfiguration(new EndpointConfiguration(environment.getProperty(PARAMETER_STORE_CLIENT_ENDPOINT_CONFIGURATION_PROPERTY),
                                                                                                               getSigningRegion(environment)))
                                                          .build();
        }
        return AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

    private AWSSimpleSystemsManagement buildSSMClient(String region)
    {
        return AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).build();
    }

    private List<String> getSigningRegions(ConfigurableEnvironment environment)
    {
        return Arrays.asList(environment.getProperty(PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGIONS_CONFIGURATION_PROPERTY,
                                                     String[].class,
                                                     new String[] { getSigningRegion(environment) }));
    }

    private String getSigningRegion(ConfigurableEnvironment environment)
    {
        return environment.getProperty(PARAMETER_STORE_CLIENT_ENDPOINT_SIGNING_REGION_CONFIGURATION_PROPERTY,
                                       new DefaultAwsRegionProviderChain().getRegion());
    }
}
