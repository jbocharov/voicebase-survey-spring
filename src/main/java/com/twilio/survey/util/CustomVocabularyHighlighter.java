package com.twilio.survey.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jbocharov on 5/24/17.
 */
public class CustomVocabularyHighlighter {
    /**
     *
     * @param original The original string without highlighting
     * @param termsList a Comma-Separated list of custom terms
     * @param before the characters of markup to add before a match
     * @param after the characters or markup to add after a match
     * @return
     */
    public static String highlight(final String original, final String termsList, final String before, final String after) {
        Preconditions.checkNotNull(original);
        Preconditions.checkNotNull(termsList);
        Preconditions.checkNotNull(before);
        Preconditions.checkNotNull(after);

        final List<String> terms = Arrays.asList(termsList.toLowerCase().split(", "));

        String currentHighlight = original;
        String currentLowercase = original.toLowerCase();
        int currentHighlightEnd = currentHighlight.length();

        for (String term: terms) {
            int startOfNextMatch = currentLowercase.indexOf(term);

            int termLength = term.length();

            if (startOfNextMatch < 0) { continue; }

            StringBuilder sb = new StringBuilder();
            sb.append(currentHighlight.substring(0, startOfNextMatch));

            do {
                int endOfNextMatch = startOfNextMatch + termLength;
                sb.append(before)
                        .append(currentHighlight.substring(startOfNextMatch, endOfNextMatch))
                        .append(after);

                startOfNextMatch = currentLowercase.indexOf(term, endOfNextMatch);
                if (startOfNextMatch < 0) {
                    startOfNextMatch = currentHighlightEnd;
                }

                sb.append(currentHighlight.substring(endOfNextMatch, startOfNextMatch));
            } while (startOfNextMatch < currentHighlightEnd);

            currentHighlight = sb.toString();
            currentLowercase = currentHighlight.toLowerCase();
            currentHighlightEnd = currentHighlight.length();
        }

        return currentHighlight;

    }
}
