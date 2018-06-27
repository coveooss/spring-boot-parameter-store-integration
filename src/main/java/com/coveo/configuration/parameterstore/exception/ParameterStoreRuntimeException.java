package com.coveo.configuration.parameterstore.exception;

public class ParameterStoreRuntimeException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public ParameterStoreRuntimeException(String propertyName, Exception e)
    {
        super(String.format("Accessing Parameter Store for parameter '%s' failed.", propertyName), e);
    }
}
