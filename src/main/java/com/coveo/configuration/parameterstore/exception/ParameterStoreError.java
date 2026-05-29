package com.coveo.configuration.parameterstore.exception;

import java.io.Serial;

public class ParameterStoreError extends Error
{
    @Serial
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
