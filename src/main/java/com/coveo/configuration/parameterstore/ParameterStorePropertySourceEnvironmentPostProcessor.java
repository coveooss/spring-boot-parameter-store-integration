package com.coveo.configuration.parameterstore;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ObjectUtils;

import com.amazonaws.ClientConfigurationFactory;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategy;
import com.coveo.configuration.parameterstore.strategy.ParameterStorePropertySourceConfigurationStrategyFactory;
import com.coveo.configuration.parameterstore.strategy.StrategyType;

public class ParameterStorePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered
{
    static ParameterStorePropertySourceConfigurationStrategyFactory strategyFactory = new ParameterStorePropertySourceConfigurationStrategyFactory();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        if (isParameterStorePropertySourceEnabled(environment)) {
            getParameterStorePropertySourceConfigurationStrategy(environment).configureParameterStorePropertySources(environment,
                                                                                                                     preconfigureSSMClientBuilder(environment));
        }
    }

    private AWSSimpleSystemsManagementClientBuilder preconfigureSSMClientBuilder(ConfigurableEnvironment environment)
    {

        AWSSimpleSystemsManagementClientBuilder awsSimpleSystemsManagementClientBuilder = AWSSimpleSystemsManagementClientBuilder.standard();
        if (hasRoleArn(environment)) {
            AWSSecurityTokenService defaultStsClientV1 = AWSSecurityTokenServiceClientBuilder.standard()
                .build();

            AWSCredentialsProvider awsCredentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                .Builder(environment.getProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_ROLE_ARN), "aws-sdk-java-v1")
                .withStsClient(defaultStsClientV1)
                .build();
            awsSimpleSystemsManagementClientBuilder = awsSimpleSystemsManagementClientBuilder.withCredentials(awsCredentialsProvider);
        }
        awsSimpleSystemsManagementClientBuilder = awsSimpleSystemsManagementClientBuilder
            .withClientConfiguration(new ClientConfigurationFactory().getConfig()
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(environment.getProperty(ParameterStorePropertySourceConfigurationProperties.MAX_ERROR_RETRY,
                    Integer.class,
                    PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY))));
        return awsSimpleSystemsManagementClientBuilder;                                                                                                                                                                                    PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY))));
    }

    private ParameterStorePropertySourceConfigurationStrategy getParameterStorePropertySourceConfigurationStrategy(ConfigurableEnvironment environment)
    {
        StrategyType type = isMultiRegionEnabled(environment) ? StrategyType.MULTI_REGION : StrategyType.DEFAULT;
        return strategyFactory.getStrategy(type);
    }

    private boolean isParameterStorePropertySourceEnabled(ConfigurableEnvironment environment)
    {
        String[] userDefinedEnabledProfiles = environment.getProperty(ParameterStorePropertySourceConfigurationProperties.ACCEPTED_PROFILES,
                                                                      String[].class);
        return environment.getProperty(ParameterStorePropertySourceConfigurationProperties.ENABLED,
                                       Boolean.class,
                                       Boolean.FALSE)
                || environment.acceptsProfiles(ParameterStorePropertySourceConfigurationProperties.ENABLED_PROFILE)
                || (!ObjectUtils.isEmpty(userDefinedEnabledProfiles)
                        && environment.acceptsProfiles(userDefinedEnabledProfiles));
    }

    private boolean isMultiRegionEnabled(ConfigurableEnvironment environment)
    {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperties.MULTI_REGION_SSM_CLIENT_REGIONS);
    }

    private boolean hasRoleArn(ConfigurableEnvironment environment) {
        return environment.containsProperty(ParameterStorePropertySourceConfigurationProperties.SSM_CLIENT_ROLE_ARN);
    }

    @Override
    public int getOrder()
    {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
