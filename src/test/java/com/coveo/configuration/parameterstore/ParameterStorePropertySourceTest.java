package com.coveo.configuration.parameterstore;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ParameterStorePropertySourceTest
{
    private static final String VALID_PROPERTY_NAME = "/validproperty";
    private static final String VALID_VALUE = "myvalidvalue";

    @Mock
    private ParameterStoreSource parameterStoreSourceMock;

    private ParameterStorePropertySource parameterStorePropertySource;

    @Before
    public void setUp()
    {
        parameterStorePropertySource = new ParameterStorePropertySource("someuselessname", parameterStoreSourceMock);
        when(parameterStoreSourceMock.getProperty(VALID_PROPERTY_NAME)).thenReturn(VALID_VALUE);
    }

    @Test
    public void testGetPropertyReturnsNullWithoutPingingParameterStoreIfPrefixIsNotPresent()
    {
        Object value = parameterStorePropertySource.getProperty("somepropswithoutslashbefore");

        assertThat(value, is(nullValue()));
        verify(parameterStoreSourceMock, never()).getProperty(any());
    }

    @Test
    public void testGetProperty()
    {
        Object value = parameterStorePropertySource.getProperty(VALID_PROPERTY_NAME);

        assertThat(value, is(VALID_VALUE));
        verify(parameterStoreSourceMock).getProperty(VALID_PROPERTY_NAME);
    }
}
