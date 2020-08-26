package com.coveo.configuration.parameterstore.strategy;

import org.springframework.core.env.ConfigurableEnvironment;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;

public interface ParameterStorePropertySourceConfigurationStrategy
{
    void configureParameterStorePropertySources(ConfigurableEnvironment environment,
                                                AWSSimpleSystemsManagementClientBuilder ssmClientBuilder);
}
