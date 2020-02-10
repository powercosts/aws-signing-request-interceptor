package vc.inreach.aws.request;

import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AWSSigningRequestInterceptorTest {

    @Mock
    private AWSSigner signer;
    @Mock
    private HttpRequestWrapper request;
    @Mock
    private HttpContext context;

    private AWSSigningRequestInterceptor interceptor;

    @Before
    public void setUp() throws Exception {
        interceptor = new AWSSigningRequestInterceptor(signer);
    }

    @Test
    public void noQueryParams() throws Exception {
        final String url = "http://someurl.com";
        final Map<String, List<String>> queryParams = new HashMap<>();

        when(signer.getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class))).thenReturn(ImmutableMap.of());
        mockRequest(url);

        interceptor.process(request, context);

        verify(request).setHeaders(new Header[]{});
        verify(signer).getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class));
    }


    @Test
    public void queryParamsSupportValuesWithSpaceEncodedAsPlus() throws Exception {
        final String url = "http://someurl.com?a=b+c";
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("a", Collections.singletonList("b c"));

        when(signer.getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class))).thenReturn(ImmutableMap.of());
        mockRequest(url);

        interceptor.process(request, context);

        verify(request).setHeaders(new Header[]{});
        verify(signer).getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class));
    }

    @Test
    public void queryParamsSupportValuesWithEquals() throws Exception {
        final String key = "scroll_id";
        final String value = "c2NhbjsxOzc3Mjo5WGljUUFNeVJGcVdDSzBjaUVQcDJ3OzE7dG90YWxfaGl0czo1NTg0Ow==";
        final String url = "http://someurl.com?" + key + "=" + value;
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put(key, Collections.singletonList(value));

        when(signer.getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class))).thenReturn(ImmutableMap.of());
        mockRequest(url);

        interceptor.process(request, context);

        verify(request).setHeaders(new Header[]{});
        verify(signer).getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class));
    }

    @Test
    public void queryParamsSupportValuesWithoutEquals() throws Exception {
        final String key = "scroll_id";
        final String url = "http://someurl.com?" + key;
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put(key, Collections.singletonList(""));

        when(signer.getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class))).thenReturn(ImmutableMap.of());
        mockRequest(url);

        interceptor.process(request, context);

        verify(request).setHeaders(new Header[]{});
        verify(signer).getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class));
    }

    @Test
    public void queryParamsSupportEmptyValues() throws Exception {
        final String key = "a";
        final String url = "http://someurl.com?" + key + "=";
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put(key, Collections.singletonList(""));

        when(signer.getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class))).thenReturn(ImmutableMap.of());
        mockRequest(url);

        interceptor.process(request, context);

        verify(request).setHeaders(new Header[]{});
        verify(signer).getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class));
    }

    @Test
    public void emptyQueryParams() throws Exception {
        final String key = "a";
        final String value = "b";
        final String url = "http://someurl.com?" + key + "=" + value + "&";
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put(key, Collections.singletonList(value));

        when(signer.getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class))).thenReturn(ImmutableMap.of());
        mockRequest(url);

        interceptor.process(request, context);

        verify(request).setHeaders(new Header[]{});
        verify(signer).getSignedHeaders(anyString(), anyString(), eq(queryParams), anyMapOf(String.class, Object.class), any(java.util.Optional.class));
    }

    private void mockRequest(String url) throws Exception {
        when(request.getURI()).thenReturn(new URI(url));
        when(request.getRequestLine()).thenReturn(new BasicRequestLine("GET", url, new ProtocolVersion("HTTP", 1, 1)));
        when(request.getAllHeaders()).thenReturn(new Header[]{});
        when(request.getOriginal()).thenReturn(request);
    }
}
