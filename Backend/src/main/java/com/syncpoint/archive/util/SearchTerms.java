package com.syncpoint.archive.util;


public final class SearchTerms {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 100;

    private SearchTerms() {
    }

   
    public static String likeContains(String term) {
        StringBuilder pattern = new StringBuilder("%");
        for (char c : term.trim().toCharArray()) {
            if (c == '!' || c == '%' || c == '_') {
                pattern.append('!');
            }
            pattern.append(c);
        }
        return pattern.append('%').toString();
    }

 
    public static String validate(String term) {
        String trimmed = term == null ? "" : term.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Enter at least " + MIN_LENGTH + " characters to search.");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Search text must not exceed " + MAX_LENGTH + " characters.");
        }
        return trimmed;
    }
}
