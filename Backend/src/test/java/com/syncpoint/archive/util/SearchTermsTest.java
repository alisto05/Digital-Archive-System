package com.syncpoint.archive.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchTermsTest {

    @Test
    void wrapsTheTermForAContainsSearch() {
        assertEquals("%smith%", SearchTerms.likeContains("  smith "));
    }

    @Test
    void escapesWildcardsSoTheyCannotMatchEverything() {
        assertEquals("%50!%%", SearchTerms.likeContains("50%"));
        assertEquals("%a!_b%", SearchTerms.likeContains("a_b"));
        assertEquals("%hi!!%", SearchTerms.likeContains("hi!"));
    }

    @Test
    void requiresBetweenTwoAndOneHundredCharacters() {
        assertEquals("ab", SearchTerms.validate(" ab "));
        assertThrows(IllegalArgumentException.class, () -> SearchTerms.validate("a"));
        assertThrows(IllegalArgumentException.class, () -> SearchTerms.validate("  "));
        assertThrows(IllegalArgumentException.class, () -> SearchTerms.validate(null));
        assertThrows(IllegalArgumentException.class, () -> SearchTerms.validate("x".repeat(101)));
    }
}
