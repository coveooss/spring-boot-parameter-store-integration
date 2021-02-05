package com.coveo.configuration.parameterstore;

//import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
//import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
//import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
//import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.coveo.configuration.parameterstore.exception.ParameterStoreError;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundError;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

public class ParameterStoreSource
{
    private SsmClient ssmClient;
    private boolean haltBoot;

    public ParameterStoreSource(SsmClient ssmClient, boolean haltBoot)
    {
        this.ssmClient = ssmClient;
        this.haltBoot = haltBoot;
    }

    public Object getProperty(String propertyName)
    {
        try {
            GetParameterResponse getParameterResult = ssmClient.getParameter(GetParameterRequest.builder()
                                                                                                .name(propertyName)
                                                                                                .withDecryption(true)
                                                                                                .build());
            validate(propertyName, getParameterResult);
            return getParameterResult.parameter().value();
        } catch (ParameterNotFoundException e) {
            if (haltBoot) {
                throw new ParameterStoreParameterNotFoundError(propertyName, e);
            }
        } catch (Exception e) {
            throw new ParameterStoreError(propertyName, e);
        }
        return null;
    }

    private void validate(String propertyName, GetParameterResponse getParameterResult)
    {
        String requestId = getParameterResult.responseMetadata().requestId();
        int statusCode = getParameterResult.sdkHttpResponse().statusCode();
        if (statusCode != 200) {
            throw new ParameterStoreError(propertyName,
                                          String.format("Invalid response code '%s' received from AWS. AWS Request ID : '%s'.",
                                                        statusCode,
                                                        requestId));
        }

        if (getParameterResult.parameter() == null) {
            throw new ParameterStoreError(propertyName,
                                          String.format("A null Parameter was received from AWS. AWS Request ID : '%s'.",
                                                        requestId));
        }

        if (getParameterResult.parameter().value() == null) {
            throw new ParameterStoreError(propertyName,
                                          String.format("A null Parameter value was received from AWS. AWS Request ID : '%s'.",
                                                        requestId));
        }
    }
}
