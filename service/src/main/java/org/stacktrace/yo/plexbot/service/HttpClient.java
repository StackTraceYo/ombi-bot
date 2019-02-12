package org.stacktrace.yo.plexbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class HttpClient {

    private final ObjectMapper myMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    public HttpClient(ObjectMapper oMapper) {
        myMapper = oMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    public <T> Optional<T> post(String pRoute, Object request, Class<T> responseType) {

        try {
            String rq = myMapper.writeValueAsString(request);

            URI route = URI.create(pRoute);
            HttpPost httpPost = new HttpPost(route);
            httpPost.setEntity(
                    EntityBuilder
                            .create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setText(rq)
                            .build()
            );
            T result = executePost(httpPost, new StringResponseHandler(), responseType);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", pRoute, e);
            return Optional.empty();
        }
    }

    public <T> Optional<T> post(String pRoute, Object request, NameValuePair[] headers, Class<T> responseType) {

        try {
            String rq = myMapper.writeValueAsString(request);

            URI route = URI.create(pRoute);
            HttpPost httpPost = new HttpPost(route);
            httpPost.setEntity(
                    EntityBuilder
                            .create()
                            .setContentType(ContentType.APPLICATION_JSON)
                            .setText(rq)
                            .build()
            );
            for (NameValuePair pair : headers) {
                httpPost.addHeader(pair.getName(), pair.getValue());
            }
            T result = executePost(httpPost, new StringResponseHandler(), responseType);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", pRoute, e);
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String pRoute, NameValuePair[] headers, Class<T> responseType) {
        try {
            URI route = URI.create(pRoute);
            HttpGet get = new HttpGet(route);
            for (NameValuePair pair : headers) {
                get.addHeader(pair.getName(), pair.getValue());
            }
            T result = executeGet(get, new StringResponseHandler(), responseType);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", pRoute, e);
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String pRoute, NameValuePair[] headers, TypeReference<T> responseType) {
        try {
            URI route = URI.create(pRoute);
            HttpGet get = new HttpGet(route);
            for (NameValuePair pair : headers) {
                get.addHeader(pair.getName(), pair.getValue());
            }
            T result = executeGet(get, new StringResponseHandler(), responseType);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", pRoute, e);
            return Optional.empty();
        }
    }

    private <T> T executeGet(HttpGet get, ResponseHandler<String> handler, TypeReference<T> responseType) {
        try (CloseableHttpClient httpClient = httpClient()) {
            LOGGER.debug("{} {} {}", get.getMethod(), get.getURI());
            String responseBody = httpClient.execute(get, handler);
            LOGGER.debug("Response Body: {}", responseBody);
            return myMapper.readValue(responseBody, responseType);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", get.getURI(), e);
            return null;
        }
    }

    private <T> T executeGet(HttpGet get, ResponseHandler<String> handler, Class<T> responseType) {
        try (CloseableHttpClient httpClient = httpClient()) {
            LOGGER.debug("{} {} {}", get.getMethod(), get.getURI());
            String responseBody = httpClient.execute(get, handler);
            LOGGER.debug("Response Body: {}", responseBody);
            return myMapper.readValue(responseBody, responseType);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", get.getURI(), e);
            return null;
        }
    }

    private <T> T executePost(HttpPost post, ResponseHandler<String> handler, Class<T> responseType) {
        try (CloseableHttpClient httpClient = httpClient()) {
            LOGGER.debug("{} {}", post.getMethod(), post.getURI());
            String responseBody = httpClient.execute(post, handler);
            LOGGER.debug("Response Body: {}", responseBody);
            return myMapper.readValue(responseBody, responseType);
        } catch (Exception e) {
            LOGGER.debug("Exception calling {}", post.getURI(), e);
            return null;
        }
    }

    public static class StringResponseHandler implements ResponseHandler<String> {


        @Override
        public String handleResponse(HttpResponse httpResponse) throws IOException {
            int status = httpResponse.getStatusLine().getStatusCode();
            LOGGER.debug("HTTP status code from ToGetRequest = {}", status);
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
                HttpEntity entity = httpResponse.getEntity();
                return entity == null ? null : EntityUtils.toString(entity);
            } else {
                throw new RuntimeException(
                        "Unexpected status from response - " + status
                );
            }
        }
    }

    private CloseableHttpClient httpClient() {
        return HttpClients.createDefault();
    }
}
