package com.coveo.configuration.parameterstore;

public final class ParameterStorePropertySourceConfigurationProperties
{
    private static final String PROPERTY_SOURCE_PREFIX = "awsParameterStorePropertySource";
    private static final String SOURCE_PREFIX = "awsParameterStoreSource";
    private static final String SSM_CLIENT_ENDPOINT_CONFIG_PREFIX = joinWithDot(SOURCE_PREFIX,
                                                                                "ssmClient",
                                                                                "endpointConfiguration");

    public static final String ENABLED_PROFILE = "awsParameterStorePropertySourceEnabled";

    public static final String ENABLED = joinWithDot(PROPERTY_SOURCE_PREFIX, "enabled");
    public static final String ACCEPTED_PROFILES = joinWithDot(PROPERTY_SOURCE_PREFIX, "enabledProfiles");
    public static final String HALT_BOOT = joinWithDot(PROPERTY_SOURCE_PREFIX, "haltBoot");

    public static final String SSM_CLIENT_CUSTOM_ENDPOINT = joinWithDot(SSM_CLIENT_ENDPOINT_CONFIG_PREFIX, "endpoint");
    public static final String SSM_CLIENT_SIGNING_REGION = joinWithDot(SSM_CLIENT_ENDPOINT_CONFIG_PREFIX,
                                                                       "signingRegion");
    public static final String MULTI_REGION_SSM_CLIENT_REGIONS = joinWithDot(SOURCE_PREFIX,
                                                                             "multiRegion",
                                                                             "ssmClient",
                                                                             "regions");

    private static String joinWithDot(String... elements)
    {
        return String.join(".", elements);
    }
}
