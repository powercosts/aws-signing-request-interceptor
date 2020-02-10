package vc.inreach.aws.request;

import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

public class AWSSigningRequestInterceptor implements HttpRequestInterceptor {

    private static final Splitter SPLITTER = Splitter.on('&').trimResults().omitEmptyStrings();

    private final AWSSigner signer;

    public AWSSigningRequestInterceptor(AWSSigner signer) {
        this.signer = signer;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        request.setHeaders(headers(signer.getSignedHeaders(
                        path(request),
                        request.getRequestLine().getMethod(),
                        params(request),
                        headers(request),
                        body(request))
        ));
    }

    private Map<String, List<String>> params(HttpRequest request) throws IOException {
        final String rawQuery = ((HttpRequestWrapper) request).getURI().getRawQuery();
        if (Strings.isNullOrEmpty(rawQuery))
            return new HashMap<>();

        return params(URLDecoder.decode(rawQuery, StandardCharsets.UTF_8.name()));
    }

    private Map<String, List<String>> params(String query) {
        final Map<String, List<String>> queryParams = new HashMap<>();

        if (! Strings.isNullOrEmpty(query)) {
            for (String pair : SPLITTER.split(query)) {
                final int index = pair.indexOf('=');
                if (index > 0) {
                    final String key = pair.substring(0, index);
                    final String value = pair.substring(index + 1);
                    List<String> existingValues = queryParams.get(key);
                    if (existingValues == null) {
                        existingValues = new ArrayList<>();
                    }
                    existingValues.add(value);
                    queryParams.put(key, existingValues);
                } else {
                    List<String> existingValues = queryParams.get(pair);
                    if (existingValues == null) {
                        existingValues = new ArrayList<>();
                    }
                    existingValues.add("");
                    queryParams.put(pair, existingValues);
                }
            }
        }

        return queryParams;
    }

    private String path(HttpRequest request) {
        return ((HttpRequestWrapper) request).getURI().getRawPath();
    }

    private Map<String, Object> headers(HttpRequest request) {
        final ImmutableMap.Builder<String, Object> headers = ImmutableMap.builder();

        for (Header header : request.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }

        return headers.build();
    }

    private java.util.Optional<byte[]> body(HttpRequest request) throws IOException {
        final HttpRequest original = ((HttpRequestWrapper) request).getOriginal();
        if (! HttpEntityEnclosingRequest.class.isAssignableFrom(original.getClass())) {
            return java.util.Optional.empty();
        }
        java.util.Optional<HttpEntity> entity = java.util.Optional.of(((HttpEntityEnclosingRequest) original).getEntity());

        return entity.map(TO_BYTE_ARRAY::apply);
    }

    private Header[] headers(Map<String, Object> from) {
        return from.entrySet().stream()
                .map(entry -> new BasicHeader(entry.getKey(), entry.getValue().toString()))
                .collect(Collectors.toList())
                .toArray(new Header[from.size()]);
    }

    private static final Function<HttpEntity, byte[]> TO_BYTE_ARRAY = entity -> {
        try {
            return ByteStreams.toByteArray(entity.getContent());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    };
}
