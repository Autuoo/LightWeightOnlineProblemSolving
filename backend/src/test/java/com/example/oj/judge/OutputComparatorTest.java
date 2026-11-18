package com.example.oj.judge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputComparatorTest {

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void ignoresCrlfAndTrailingSpaces() {
        assertTrue(comparator.matches("3\r\n", "3   \n"));
    }

    @Test
    void ignoresTrailingBlankLines() {
        assertTrue(comparator.matches("1\n2\n\n", "1\n2"));
    }

    @Test
    void detectsContentDifference() {
        assertFalse(comparator.matches("3", "4"));
    }

    @Test
    void detectsMissingLine() {
        assertFalse(comparator.matches("1\n2", "1"));
    }

    @Test
    void normalizesEmptyOutput() {
        assertTrue(comparator.matches("", "\n\n  "));
    }
}