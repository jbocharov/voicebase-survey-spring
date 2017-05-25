package com.twilio.survey.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by jbocharov on 5/24/17.
 */
public class CustomVocabularyHighligherTest {
    @Test
    public void testHtmlHighlight() {
        // intentionally mangled case
        final String original = "I hung out with zorro, voltron man, and Captain Jack sparrow today.";
        final String expected = "I hung out with <b>zorro</b>, <b>voltron</b> man, and Captain <b>Jack sparrow</b> today.";
        final String termsList = "Zorro, Voltron, Jack Sparrow";
        final String before = "<b>";
        final String after = "</b>";

        final String actual = CustomVocabularyHighlighter.highlight(original, termsList, before, after);

        assertEquals(expected, actual);
    }

}
