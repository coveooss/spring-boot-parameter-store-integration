package com.coveo.configuration.parameterstore.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.CollectionUtils;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import com.coveo.configuration.parameterstore.ParameterStoreSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public class MultiRegionParameterStorePropertySourceConfigurationStrategy
        implements ParameterStorePropertySourceConfigurationStrategy
{
    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "MultiRegionAWSParameterStorePropertySource_";

    @Override
    public void configureParameterStorePropertySources(MutablePropertySources propertySources,
                                                       Binder binder,
                                                       SsmClientBuilder ssmClientBuilder)
    {
        boolean haltBoot = binder.bind(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT, Boolean.class)
                                 .orElse(Boolean.FALSE);

        List<String> regions = getRegions(binder);

        // To keep the order of precedence, we have to iterate from the last region to the first one.
        // If we want the first region specified to be the first property source, we have to add it last.
        // We cannot use addLast since it adds the property source with lowest precedence and we want the
        // Parameter store property sources to have highest precedence on the other property sources
        Collections.reverse(regions);
        String lastRegion = regions.get(0);

        // We only want to halt boot (if true) for the last region
        propertySources.addFirst(buildParameterStorePropertySource(ssmClientBuilder, lastRegion, haltBoot));

        regions.stream()
               .skip(1)
               .forEach(region -> propertySources.addFirst(buildParameterStorePropertySource(ssmClientBuilder,
                                                                                             region,
                                                                                             false)));
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(SsmClientBuilder ssmClientBuilder,
                                                                           String region,
                                                                           boolean haltBoot)
    {
        return new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME
                + region, new ParameterStoreSource(buildSSMClient(ssmClientBuilder, region), haltBoot));
    }

    private SsmClient buildSSMClient(SsmClientBuilder ssmClientBuilder, String region)
    {
        return ssmClientBuilder.region(Region.of(region)).build();
    }

    private List<String> getRegions(Binder binder)
    {
        List<String> regions = Arrays.asList(binder.bind(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                         String[].class)
                                                   .orElseGet(() -> new String[0]));

        if (CollectionUtils.isEmpty(regions)) {
            throw new IllegalArgumentException(String.format("To enable multi region support, the property '%s' must not be empty.",
                                                             ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS));
        }

        return regions;
    }
}
