package com.coveo.configuration.parameterstore.strategy;

import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public interface ParameterStorePropertySourceConfigurationStrategy
{
    void configureParameterStorePropertySources(ConfigurableEnvironment environment, SsmClientBuilder ssmClientBuilder);
}
