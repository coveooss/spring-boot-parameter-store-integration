package com.coveo.configuration.parameterstore;

public final class ParameterStorePropertySourceConfigurationProperty
{
    private static final String PROPERTY_SOURCE_PREFIX = "awsParameterStorePropertySource.";
    private static final String SOURCE_PREFIX = "awsParameterStoreSource.";
    private static final String SSM_CLIENT_ENDPOINT_CONFIG_PREFIX = SOURCE_PREFIX + "ssmClient.endpointConfiguration.";

    public static final String ENABLED_PROFILE = "awsParameterStorePropertySourceEnabled";

    public static final String ENABLED = PROPERTY_SOURCE_PREFIX + "enabled";
    public static final String ACCEPTED_PROFILES = PROPERTY_SOURCE_PREFIX + "enabledProfiles";
    public static final String HALT_BOOT = PROPERTY_SOURCE_PREFIX + "haltBoot";
    public static final String SUPPORT_MULTIPLE_APPLICATION_CONTEXTS = PROPERTY_SOURCE_PREFIX
            + "supportMultipleApplicationContexts";

    public static final String SSM_CLIENT_CUSTOM_ENDPOINT = SSM_CLIENT_ENDPOINT_CONFIG_PREFIX + "endpoint";
    public static final String SSM_CLIENT_SIGNING_REGION = SSM_CLIENT_ENDPOINT_CONFIG_PREFIX + "signingRegion";
    public static final String MULTI_REGION_SSM_CLIENT_REGIONS = SOURCE_PREFIX + "multiRegion.ssmClient.regions";
}
