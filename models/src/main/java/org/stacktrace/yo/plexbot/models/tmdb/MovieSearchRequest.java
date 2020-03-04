package org.stacktrace.yo.plexbot.models.tmdb;

import org.stacktrace.yo.plexbot.models.shared.Routes;
import org.stacktrace.yo.plexbot.models.shared.ToGetRequest;

public class MovieSearchRequest implements ToGetRequest {

    private final String query;
    private final String apiKey;

    public MovieSearchRequest(String query, String apiKey) {
        this.query = query;
        this.apiKey = apiKey;
    }

    //    https://api.themoviedb.org/3/search/movie?api_key=6fa90113001beddccde1f30d5133dd3f&language=en-US&query=mission%20impossible&page=1&include_adult=false
    @Override
    public String toGetPath() {
        String params = "?api_key=" + apiKey + "&query=" + Routes.encode(query);
        return Routes.TMDb.Search.Movie.create(params);
    }

    @Override
    public String toGetDetailPath() {
        return toGetPath();
    }
}
