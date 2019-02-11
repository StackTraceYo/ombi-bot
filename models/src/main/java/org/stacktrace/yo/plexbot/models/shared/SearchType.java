package org.stacktrace.yo.plexbot.models.shared;

public enum SearchType {
    TV("Tv"),
    MOVIE("Movie"),
    NA("Unknown");

    private String reqValue;

    SearchType(String reqValue) {
        this.reqValue = reqValue;
    }

    public String getReqValue() {
        return reqValue;
    }

    public static SearchType fromReqValue(String reqValue) {
        try {
            return SearchType.valueOf(reqValue.toUpperCase());
        } catch (Exception e) {
            return NA;
        }
    }
}