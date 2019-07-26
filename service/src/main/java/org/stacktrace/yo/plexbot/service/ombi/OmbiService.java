package org.stacktrace.yo.plexbot.service.ombi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiMovieRequest;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiTVRequest;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiMovieSearchResponse;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVDetailResponse;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.Routes;
import org.stacktrace.yo.plexbot.service.HttpClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class OmbiService {

    private final HttpClient myHttpClient;
    private final OmbiConfig myConfig;
    private final NameValuePair[] mySearchHeaders;
    private final NameValuePair[] myRequestHeaders;

    public OmbiService(HttpClient myhttpClient, OmbiConfig config) {
        myHttpClient = myhttpClient;
        myConfig = config;
        mySearchHeaders = searchHeaders();
        myRequestHeaders = searchHeaders();
    }

    public List<OmbiMovieSearchResponse> movieSearch(OmbiSearch searchReq) {
        return myHttpClient.get(searchUrl(searchReq), mySearchHeaders, new TypeReference<List<OmbiMovieSearchResponse>>() {
        }).orElse(Lists.newArrayList());
    }

    public List<OmbiTVSearchResponse> tvSearch(OmbiSearch searchReq) {
        return myHttpClient.get(searchUrl(searchReq), mySearchHeaders, new TypeReference<List<OmbiTVSearchResponse>>() {
        }).orElse(Lists.newArrayList());
    }

    public Optional<OmbiTVDetailResponse> tvDetail(OmbiSearch searchReq) {
        return myHttpClient.get(searchDetailUrl(searchReq), mySearchHeaders, OmbiTVDetailResponse.class);
    }

    public Optional<Map> request(OmbiTVRequest request) {
        return myHttpClient.post(requestUrl(Routes.Ombi.Request.TV.path), request, myRequestHeaders, Map.class);
    }

    public Optional<Map> request(OmbiMovieRequest request) {
        return myHttpClient.post(requestUrl(Routes.Ombi.Request.Movie.path), request, myRequestHeaders, Map.class);
    }

    private String searchUrl(OmbiSearch search) {
        return myConfig.host + search.toGetPath();
    }

    private String searchDetailUrl(OmbiSearch search) {
        return myConfig.host + search.toGetDetailPath();
    }

    private String requestUrl(String path) {
        return myConfig.host + path;
    }

    private NameValuePair[] searchHeaders() {
        NameValuePair[] headers = new NameValuePair[3];
        headers[0] = new BasicNameValuePair("ApiKey", myConfig.token);
        headers[1] = new BasicNameValuePair("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        if (myConfig.username != null) {
            headers[2] = new BasicNameValuePair("UserName", myConfig.username);
        } else {
            headers[2] = new BasicNameValuePair("ApiAlias", "ombi-bot");
        }

        return headers;
    }

    public static final class OmbiConfig {
        private final String host;
        private final String token;
        private final String username;

        public OmbiConfig(String host, String token, String username) {
            this.host = host;
            this.token = token;
            this.username = username;
        }
    }
}
