package com.example.ridelink.ride.client;

import com.example.ridelink.ride.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

/** Shared timeout setup and error translation for outbound calls. The original exception is never discarded. */
abstract class RemoteCallSupport {

    private static final Logger log = LoggerFactory.getLogger(RemoteCallSupport.class);

    protected static RestClient buildClient(String baseUrl, int connectMs, int readMs) {
        // JDK HttpClient (not HttpURLConnection): the old SimpleClientHttpRequestFactory cannot send PATCH.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        log.info("HTTP client configured: baseUrl={} connectTimeout={}ms readTimeout={}ms", baseUrl, connectMs, readMs);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    protected <T> T call(String serviceName, Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException ex) {
            // The other service answered, but with an error status (401 = JWT secret mismatch, 403 = role, 404 = path...).
            String body = truncate(ex.getResponseBodyAsString());
            log.error("{} answered HTTP {} {} | response body: {}", serviceName,
                    ex.getStatusCode().value(), ex.getStatusText(), body, ex);
            HttpStatus status = ex.getStatusCode().is5xxServerError()
                    ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
            throw new ApiException(status, serviceName + " answered HTTP " + ex.getStatusCode().value()
                    + (body.isBlank() ? "" : ": " + body), ex);
        } catch (ResourceAccessException ex) {
            // No usable HTTP response at all: refused, timeout, unknown host, reset, TLS...
            log.error("{} could not be reached: {}", serviceName, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, serviceName + " is unavailable", ex);
        } catch (RestClientException ex) {
            // Anything else, for example the response body could not be converted to the expected type.
            log.error("{} call failed: {}", serviceName, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, serviceName + " call failed", ex);
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
