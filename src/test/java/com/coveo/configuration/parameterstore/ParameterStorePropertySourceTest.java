package com.coveo.configuration.parameterstore;

import static com.google.common.truth.Truth.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ParameterStorePropertySourceTest
{
    private static final String VALID_PROPERTY_NAME = "/validproperty";
    private static final String VALID_VALUE = "myvalidvalue";

    @Mock
    private ParameterStoreSource parameterStoreSourceMock;

    private ParameterStorePropertySource parameterStorePropertySource;

    @BeforeEach
    public void setUp()
    {
        parameterStorePropertySource = new ParameterStorePropertySource("someuselessname", parameterStoreSourceMock);
    }

    @Test
    public void testGetPropertyReturnsNullWithoutPingingParameterStoreIfPrefixIsNotPresent()
    {
        Object value = parameterStorePropertySource.getProperty("somepropswithoutslashbefore");

        assertThat(value).isNull();
        verify(parameterStoreSourceMock, never()).getProperty(any());
    }

    @Test
    public void testGetProperty()
    {
        when(parameterStoreSourceMock.getProperty(VALID_PROPERTY_NAME)).thenReturn(VALID_VALUE);

        Object value = parameterStorePropertySource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value).isEqualTo(VALID_VALUE);
        verify(parameterStoreSourceMock).getProperty(VALID_PROPERTY_NAME);
    }
}
