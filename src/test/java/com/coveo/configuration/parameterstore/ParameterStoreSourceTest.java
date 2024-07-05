package com.coveo.configuration.parameterstore;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coveo.configuration.parameterstore.exception.ParameterStoreError;
import com.coveo.configuration.parameterstore.exception.ParameterStoreParameterNotFoundError;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

@ExtendWith(MockitoExtension.class)
public class ParameterStoreSourceTest
{
    private static final String VALID_PROPERTY_NAME = "awesomeproperty";
    private static final String VALID_PROPERTY_VALUE = "awesomepropertyVALUE";

    private static final String INVALID_PROPERTY_NAME = "notawesomeproperty";

    @Mock
    private SsmClient ssmClientMock;
    @Mock
    private SdkHttpResponse sdkHttpMetadataMock;
    @Spy
    private AwsResponseMetadata responseMetadataSpy = SsmResponseMetadata.create(null);

    private ParameterStoreSource parameterStoreSource;

    @BeforeEach
    public void setUp()
    {
        parameterStoreSource = new ParameterStoreSource(ssmClientMock, false);
    }

    @Test
    public void testGetProperty()
    {
        when(sdkHttpMetadataMock.statusCode()).thenReturn(200);
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResponse().toBuilder()
                                                                                                                       .parameter(Parameter.builder()
                                                                                                                                           .value(VALID_PROPERTY_VALUE)
                                                                                                                                           .build())
                                                                                                                       .build());

        Object value = parameterStoreSource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value).isEqualTo(VALID_PROPERTY_VALUE);
    }

    @Test
    public void testGetPropertyWhenNotFoundReturnsNull()
    {
        when(ssmClientMock.getParameter(getParameterRequest(INVALID_PROPERTY_NAME))).thenThrow(ParameterNotFoundException.builder()
                                                                                                                         .build());

        Object value = parameterStoreSource.getProperty(INVALID_PROPERTY_NAME);

        assertThat(value).isNull();
    }

    @Test
    public void shouldThrowOnUnexpectedExceptionAccessingParameterStore()
    {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenThrow(new RuntimeException());

        assertThrows(ParameterStoreError.class, () -> parameterStoreSource.getProperty(VALID_PROPERTY_NAME));
    }

    @Test
    public void shouldThrowOnGetPropertyWhenNotFoundAndHaltBootIsTrue()
    {
        when(ssmClientMock.getParameter(getParameterRequest(INVALID_PROPERTY_NAME))).thenThrow(ParameterNotFoundException.builder()
                                                                                                                         .build());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        assertThrows(ParameterStoreParameterNotFoundError.class,
                     () -> parameterStoreSourceHaltingBoot.getProperty(INVALID_PROPERTY_NAME));
    }

    @Test
    public void shouldThrowWhenStatusCodeIsNot200()
    {
        when(sdkHttpMetadataMock.statusCode()).thenReturn(503);
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResponse());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        assertThrows(ParameterStoreError.class, () -> parameterStoreSourceHaltingBoot.getProperty(VALID_PROPERTY_NAME));
    }

    @Test
    public void shouldThrowWhenParameterIsNull()
    {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResponse());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);

        assertThrows(ParameterStoreError.class, () -> parameterStoreSourceHaltingBoot.getProperty(VALID_PROPERTY_NAME));
    }

    @Test
    public void shouldThrowWhenParameterValueIsNull()
    {
        when(ssmClientMock.getParameter(getParameterRequest(VALID_PROPERTY_NAME))).thenReturn(getGetParameterResponse().toBuilder()
                                                                                                                       .parameter((Parameter.builder()
                                                                                                                                            .build()))
                                                                                                                       .build());
        ParameterStoreSource parameterStoreSourceHaltingBoot = new ParameterStoreSource(ssmClientMock, true);
        assertThrows(ParameterStoreError.class, () -> parameterStoreSourceHaltingBoot.getProperty(VALID_PROPERTY_NAME));
    }

    private GetParameterResponse getGetParameterResponse()
    {
        return (GetParameterResponse) GetParameterResponse.builder()
                                                          .responseMetadata(responseMetadataSpy)
                                                          .sdkHttpResponse(sdkHttpMetadataMock)
                                                          .build();
    }

    private GetParameterRequest getParameterRequest(String parameterName)
    {
        return GetParameterRequest.builder().name(parameterName).withDecryption(true).build();
    }
}
