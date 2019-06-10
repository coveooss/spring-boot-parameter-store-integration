package com.coveo.configuration.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.coveo.configuration.parameterstore.exception.ParameterStoreError;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundError;

public class ParameterStoreSource
{
    private AWSSimpleSystemsManagement ssmClient;
    private boolean haltBoot;

    public ParameterStoreSource(AWSSimpleSystemsManagement ssmClient, boolean haltBoot)
    {
        this.ssmClient = ssmClient;
        this.haltBoot = haltBoot;
    }

    public Object getProperty(String propertyName)
    {
        try {
            GetParameterResult getParameterResult = ssmClient.getParameter(new GetParameterRequest().withName(propertyName)
                                                                                                    .withWithDecryption(true));
            validate(propertyName, getParameterResult);
            return getParameterResult.getParameter().getValue();
        } catch (ParameterNotFoundException e) {
            if (haltBoot) {
                throw new ParameterStoreParameterNotFoundError(propertyName, e);
            }
        } catch (Exception e) {
            throw new ParameterStoreError(propertyName, e);
        }
        return null;
    }

    private void validate(String propertyName, GetParameterResult getParameterResult)
    {
        String requestId = getParameterResult.getSdkResponseMetadata().getRequestId();
        int statusCode = getParameterResult.getSdkHttpMetadata().getHttpStatusCode();
        if (statusCode != 200) {
            throw new ParameterStoreError(propertyName,
                                          String.format("Invalid response code '%s' received from AWS. AWS Request ID : '%s'.",
                                                        statusCode,
                                                        requestId));
        }

        if (getParameterResult.getParameter() == null) {
            throw new ParameterStoreError(propertyName,
                                          String.format("A null Parameter was received from AWS. AWS Request ID : '%s'.",
                                                        requestId));
        }

        if (getParameterResult.getParameter().getValue() == null) {
            throw new ParameterStoreError(propertyName,
                                          String.format("A null Parameter value was received from AWS. AWS Request ID : '%s'.",
                                                        requestId));
        }
    }
}
