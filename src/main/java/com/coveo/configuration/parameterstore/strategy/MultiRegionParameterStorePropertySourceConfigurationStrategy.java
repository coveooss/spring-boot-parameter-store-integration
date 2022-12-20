package com.coveo.configuration.parameterstore.strategy;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.util.Collections;
import java.util.List;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.CollectionUtils;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.coveo.configuration.parameterstore.ParameterStorePropertySource;
import com.coveo.configuration.parameterstore.ParameterStorePropertySourceConfigurationProperties;
import com.coveo.configuration.parameterstore.ParameterStoreSource;

public class MultiRegionParameterStorePropertySourceConfigurationStrategy
        implements ParameterStorePropertySourceConfigurationStrategy
{
    private static final String PARAMETER_STORE_PROPERTY_SOURCE_NAME = "MultiRegionAWSParameterStorePropertySource_";

    @Override
    public void configureParameterStorePropertySources(ConfigurableEnvironment environment,
                                                       AWSSimpleSystemsManagementClientBuilder ssmClientBuilder)
    {
        boolean haltBoot = environment.getProperty(ParameterStorePropertySourceConfigurationProperties.HALT_BOOT,
                                                   Boolean.class,
                                                   Boolean.FALSE);
        if (hasRoleArn(environment)) {
            AWSSecurityTokenService defaultStsClientV1 = AWSSecurityTokenServiceClientBuilder.standard()
                .build();

            AWSCredentialsProvider awsCredentialsProvider = new STSAssumeRoleSessionCredentialsProvider.Builder(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_ROLE_ARN,
                "aws-sdk-java-v1").withStsClient(defaultStsClientV1)
                .build();

            ssmClientBuilder = ssmClientBuilder.withCredentials(awsCredentialsProvider);
        }

        List<String> regions = getRegions(environment);

        // To keep the order of precedence, we have to iterate from the last region to the first one.
        // If we want the first region specified to be the first property source, we have to add it last.
        // We cannot use addLast since it adds the property source with lowest precedence and we want the
        // Parameter store property sources to have highest precedence on the other property sources
        Collections.reverse(regions);
        String lastRegion = regions.get(0);

        // We only want to halt boot (if true) for the last region
        environment.getPropertySources()
                   .addFirst(buildParameterStorePropertySource(ssmClientBuilder, lastRegion, haltBoot));

        AWSSimpleSystemsManagementClientBuilder finalSsmClientBuilder = ssmClientBuilder;
        regions.stream()
               .skip(1)
               .forEach(region -> environment.getPropertySources()
                                             .addFirst(buildParameterStorePropertySource(
                                                 finalSsmClientBuilder,
                                                                                         region,
                                                                                         false)));
    }

    private boolean hasRoleArn(ConfigurableEnvironment environment) {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_ROLE_ARN);
    }

    private ParameterStorePropertySource buildParameterStorePropertySource(AWSSimpleSystemsManagementClientBuilder ssmClientBuilder,
                                                                           String region,
                                                                           boolean haltBoot)
    {
        return new ParameterStorePropertySource(PARAMETER_STORE_PROPERTY_SOURCE_NAME
                + region, new ParameterStoreSource(buildSSMClient(ssmClientBuilder, region), haltBoot));
    }

    private AWSSimpleSystemsManagement buildSSMClient(AWSSimpleSystemsManagementClientBuilder ssmClientBuilder,
                                                      String region)
    {
        return ssmClientBuilder.withRegion(region).build();
    }

    private List<String> getRegions(ConfigurableEnvironment environment)
    {
        List<String> regions = CollectionUtils.arrayToList(environment.getProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS,
                                                                                   String[].class));

        if (CollectionUtils.isEmpty(regions)) {
            throw new IllegalArgumentException(String.format("To enable multi region support, the property '%s' must not be empty.",
                                                             ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS));
        }

        return regions;
    }
}
