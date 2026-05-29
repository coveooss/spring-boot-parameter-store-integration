package com.coveo.configuration.parameterstore;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParameterStorePropertySourceTest
{
    private static final String VALID_PROPERTY_NAME = "/validproperty";
    private static final String VALID_VALUE = "myvalidvalue";

    @Mock
    private ParameterStoreSource parameterStoreSourceMock;

    private ParameterStorePropertySource parameterStorePropertySource;

    @BeforeEach
    void setUp()
    {
        parameterStorePropertySource = new ParameterStorePropertySource("someuselessname", parameterStoreSourceMock);
    }

    @Test
    void testGetPropertyReturnsNullWithoutPingingParameterStoreIfPrefixIsNotPresent()
    {
        Object value = parameterStorePropertySource.getProperty("somepropswithoutslashbefore");

        assertThat(value).isNull();
        verify(parameterStoreSourceMock, never()).getProperty(any());
    }

    @Test
    void testGetProperty()
    {
        when(parameterStoreSourceMock.getProperty(VALID_PROPERTY_NAME)).thenReturn(VALID_VALUE);

        Object value = parameterStorePropertySource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value).isEqualTo(VALID_VALUE);
        verify(parameterStoreSourceMock).getProperty(VALID_PROPERTY_NAME);
    }
}
