package com.coveo.configuration.parameterstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundError;
import com.coveo.configuration.parameterstore.exception.ParameterStoreError;

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
            return ssmClient.getParameter(new GetParameterRequest().withName(propertyName).withWithDecryption(true))
                            .getParameter()
                            .getValue();
        } catch (ParameterNotFoundException e) {
            if (haltBoot) {
                throw new ParameterStoreParameterNotFoundError(propertyName, e);
            }
        } catch (Exception e) {
            throw new ParameterStoreError(propertyName, e);
        }
        return null;
    }
}
