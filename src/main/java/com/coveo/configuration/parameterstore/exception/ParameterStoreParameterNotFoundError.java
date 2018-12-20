package com.coveo.configuration.parameterstore.exception;

import com.coveo.configuration.parameterstore.ParameterStorePropertySource;

public class ParameterStoreParameterNotFoundError extends Error
{
    private static final long serialVersionUID = 1L;

    public ParameterStoreParameterNotFoundError(String propertyName, Exception e)
    {
        super(String.format("Properties prefixed with a '%s' are reserved for the AWS Parameter Store. "
                + "The property '%s' was not found in it. "
                + "If you are trying to add some properties to AWS PS : Please make sure the property is in it and that the application's IAM role can access it (KMS key and the property). "
                + "If you are trying to add a property in the application without using the parameter store : rename your property without this prefix.",
                            ParameterStorePropertySource.PARAMETER_STORE_HIERARCHY_SPLIT_CHARACTER,
                            propertyName),
              e);
    }
}
