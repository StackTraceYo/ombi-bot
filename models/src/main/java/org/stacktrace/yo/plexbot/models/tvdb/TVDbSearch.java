package org.stacktrace.yo.plexbot.models.tvdb;


public class TVDbSearch {

    /**
     * Allows the user to search for a series based on the following parameters.
     *
     * @param name Name of the series to search for.
     */
    @GET("search/series")
    Call<SeriesResultsResponse> series(
            @Query("name") String name,
            @Query("imdbId") String imdbId,
            @Query("zap2itId") String zap2itId,
            @Query("slug") String slug,
            @Header(TheTvdb.HEADER_ACCEPT_LANGUAGE) String languages
    );

    @GET("search/series/params")
    Call<SearchParamsResponse> params();

}
