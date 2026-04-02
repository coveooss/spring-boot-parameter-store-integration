package com.coveo.configuration.parameterstore.strategy;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MutablePropertySources;

import software.amazon.awssdk.services.ssm.SsmClientBuilder;

public interface ParameterStorePropertySourceConfigurationStrategy
{
    void configureParameterStorePropertySources(MutablePropertySources propertySources,
                                                Binder binder,
                                                SsmClientBuilder ssmClientBuilder);
}
