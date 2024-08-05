package com.coveo.configuration.parameterstore;

import com.coveo.configuration.parameterstore.exception.ParameterStoreError;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundError;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.ParameterVersionNotFoundException;

import java.util.Objects;

public class ParameterStoreSource
{
    private final SsmClient ssmClient;
    private final boolean haltBoot;

    public ParameterStoreSource(SsmClient ssmClient, boolean haltBoot)
    {
        this.ssmClient = Objects.requireNonNull(ssmClient);
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
        } catch (ParameterNotFoundException | ParameterVersionNotFoundException e) {
            if (haltBoot) {
                throw new ParameterStoreParameterNotFoundError(propertyName, e);
            }
        } catch (Exception e) {
            throw new ParameterStoreError(propertyName, e);
        }
        return null;
    }

    private void validate(String propertyName, GetParameterResponse getParameterResponse)
    {
        String requestId = getParameterResponse.responseMetadata().requestId();
        int statusCode = getParameterResponse.sdkHttpResponse().statusCode();
        if (statusCode != 200) {
            throw new ParameterStoreError(propertyName,
                                          String.format("Invalid response code '%s' received from AWS. AWS Request ID : '%s'.",
                                                        statusCode,
                                                        requestId));
        }

        if (getParameterResponse.parameter() == null) {
            throw new ParameterStoreError(propertyName,
                                          String.format("A null Parameter was received from AWS. AWS Request ID : '%s'.",
                                                        requestId));
        }

        if (getParameterResponse.parameter().value() == null) {
            throw new ParameterStoreError(propertyName,
                                          String.format("A null Parameter value was received from AWS. AWS Request ID : '%s'.",
                                                        requestId));
        }
    }
}
