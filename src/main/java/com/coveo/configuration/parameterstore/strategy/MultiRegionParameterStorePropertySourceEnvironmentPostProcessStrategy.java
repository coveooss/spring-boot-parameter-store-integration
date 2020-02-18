package com.coveo.configuration.parameterstore.strategy;

import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty.HALT_BOOT;
import static com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperty.SSM_CLIENT_SIGNING_REGIONS;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStoreSource;

public class MultiRegionParameterStorePropertySourceEnvironmentPostProcessStrategy
        implements ParameterStorePropertySourceEnvironmentPostProcessStrategy
{
    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "MultiRegionAWSParameterStorePropertySource_";

    @Override
    public void postProcess(ConfigurableEnvironment environment)
    {
        boolean haltBoot = environment.getProperty(HALT_BOOT, Boolean.class, Boolean.FALSE);

        LinkedList<String> signingRegions = getSigningRegions(environment);
        ListIterator<String> regionsIterator = signingRegions.listIterator(signingRegions.size());

        // To keep the order of precedence, we iterate from the last one to the first one because of the method 'addFirst'.
        // If we want the first region specified to be the first property source, we have to add it last.
        while (regionsIterator.hasPrevious()) {
            // We only want to halt boot (if true) for the last property source so the first one added.
            boolean shouldHaltBoot = haltBoot && !regionsIterator.hasNext();
            String region = regionsIterator.previous();

            environment.getPropertySources()
                       .addFirst(buildParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME + region,
                                                                   region,
                                                                   shouldHaltBoot));
        }
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(String name, String region, boolean haltBoot)
    {
        return new ParameterStorePropertySource(name, new ParameterStoreSource(buildSSMClient(region), haltBoot));
    }

    private AWSSimpleSystemsManagement buildSSMClient(String region)
    {
        return AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).build();
    }

    private LinkedList<String> getSigningRegions(ConfigurableEnvironment environment)
    {
        String[] signingRegions = environment.getProperty(SSM_CLIENT_SIGNING_REGIONS, String[].class);
        return ObjectUtils.isEmpty(signingRegions) ? new LinkedList<>()
                                                   : new LinkedList<>(Arrays.asList(signingRegions));
    }
}
