package com.coveo.configuration.parameterstore;

public final class ParameterStorePropertySourceConfigurationProperty
{
    private static final String PREFIX = "awsParameterStorePropertySource.";
    private static final String SSM_CLIENT_PREFIX = "awsParameterStoreSource.ssmClient.";

    public static final String ACCEPTED_PROFILE = "awsParameterStorePropertySourceEnabled";

    public static final String ENABLED = PREFIX + "enabled";
    public static final String ACCEPTED_PROFILES = PREFIX + "enabledProfiles";
    public static final String HALT_BOOT = PREFIX + "haltBoot";
    public static final String SUPPORT_MULTIPLE_APPLICATION_CONTEXTS = PREFIX + "supportMultipleApplicationContexts";
    public static final String SSM_CLIENT_ENDPOINT_CONFIG_ENDPOINT = SSM_CLIENT_PREFIX
            + "endpointConfiguration.endpoint";
    public static final String SSM_CLIENT_ENDPOINT_CONFIG_SIGNING_REGION = SSM_CLIENT_PREFIX
            + "endpointConfiguration.signingRegion";
    public static final String SSM_CLIENT_SIGNING_REGIONS = SSM_CLIENT_PREFIX + "signingRegions";
}
