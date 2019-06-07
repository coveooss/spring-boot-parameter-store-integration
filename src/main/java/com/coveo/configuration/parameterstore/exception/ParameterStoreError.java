package com.coveo.configuration.parameterstore.exception;

public class ParameterStoreError extends Error
{
    private static final long serialVersionUID = 1L;

    public ParameterStoreError(String propertyName, String reason)
    {
        super(String.format("Accessing Parameter Store for parameter '%s' failed. %s", propertyName, reason));
    }

    public ParameterStoreError(String propertyName, Exception e)
    {
        super(String.format("Accessing Parameter Store for parameter '%s' failed.", propertyName), e);
    }
}
