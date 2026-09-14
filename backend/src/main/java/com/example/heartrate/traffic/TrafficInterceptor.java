package com.example.heartrate.traffic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

/**
 * Copies every call to a standards server into the {@link TrafficRecorder} on its way past.
 *
 * <p>Reading the response body here would normally consume it before the caller sees it; the
 * clients are built on a {@code BufferingClientHttpRequestFactory} so the stream can be read twice.
 */
public class TrafficInterceptor implements ClientHttpRequestInterceptor {

    private final TrafficRecorder recorder;
    private final String server;

    public TrafficInterceptor(TrafficRecorder recorder, String server) {
        this.recorder = recorder;
        this.server = server;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        var uri = request.getURI();
        var method = request.getMethod().name();
        var requestBody = new String(body, StandardCharsets.UTF_8);
        long started = System.nanoTime();

        try {
            var response = execution.execute(request, body);
            recorder.record(
                    server,
                    method,
                    uri.getPath(),
                    uri.getQuery(),
                    response.getStatusCode().value(),
                    millisSince(started),
                    requestBody,
                    readBody(response),
                    null);
            return response;
        } catch (IOException | RuntimeException e) {
            recorder.record(server, method, uri.getPath(), uri.getQuery(), null, millisSince(started),
                    requestBody, null, e.getMessage());
            throw e;
        }
    }

    /** A body that cannot be read is not worth failing the call over. */
    private static String readBody(ClientHttpResponse response) {
        try {
            return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static long millisSince(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
