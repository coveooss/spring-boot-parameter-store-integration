package com.coveo.configuration.parameterstore.strategy;

import java.util.Collections;
import java.util.List;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty;
import com.coveo.configuration.parameterstore.ParameterStoreSource;

public class MultiRegionParameterStorePropertySourceEnvironmentPostProcessStrategy
        implements ParameterStorePropertySourceEnvironmentPostProcessStrategy
{
    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "MultiRegionAWSParameterStorePropertySource_";

    @Override
    public void postProcess(ConfigurableEnvironment environment)
    {
        boolean haltBoot = environment.getProperty(ParameterStorePropertySourceConfigurationProperty.HALT_BOOT,
                                                   Boolean.class,
                                                   Boolean.FALSE);

        List<String> regions = getRegions(environment);
        Collections.reverse(regions);

        String lastRegion = regions.get(0);
        environment.getPropertySources().addFirst(buildParameterStorePropertySource(lastRegion, haltBoot));

        regions.stream()
               .skip(1)
               .forEach(region -> environment.getPropertySources()
                                             .addFirst(buildParameterStorePropertySource(region, false)));
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(String region, boolean haltBoot)
    {
        return new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME + region,
                                                new ParameterStoreSource(buildSSMClient(region), haltBoot));
    }

    private AWSSimpleSystemsManagement buildSSMClient(String region)
    {
        return AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).build();
    }

    private List<String> getRegions(ConfigurableEnvironment environment)
    {
        List<String> regions = CollectionUtils.arrayToList(environment.getProperty(ParameterStorePropertySourceConfigurationProperty.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                                                   String[].class));

        if (CollectionUtils.isEmpty(regions)) {
            throw new IllegalArgumentException(String.format("To enable multi region support, the property '%s' must not be empty.",
                                                             ParameterStorePropertySourceConfigurationProperty.MULTI_REGION_SSM_CLIENT_REGIONS));
        }

        return regions;
    }
}
