/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class NamesTest {
    final String maxBase = "a".repeat(Names.maxBenchmarkBaseLength);

    @Test
    void validateBenchmarkNameValid() {
        for (String name : new String[] {
                "Now- now() -Static",
                "Now- now() -Inc",
                "CumCombo- 6 Ops 3 Groups 1M Unique Combos -Static",
                "CountBy- 10 Cols Wide Avro Blink -Inc",
                "Join animals and adjectives - Parquet Views",
                "Deephaven Sort 1 Col",
                "NamesTest",
                "1st Test",
                "PositionCombo-AggBy- 4 Ops 3 Groups 100K Unique Combos -Static",
                maxBase,
                maxBase + " -Static",
                maxBase + " -Inc",
                "# Services Restart 24G",
                "# Data Table Generation source",
                "# Any/internal,session's name ",
        }) {
            assertDoesNotThrow(() -> Names.validateBenchmarkName(name), "Should be valid: " + name);
        }
    }

    @Test
    void validateBenchmarkNameInvalid() {
        for (String name : new String[] {
                null,
                "",
                "   ",
                "-Leading Hyphen",
                "(Leading Paren",
                " Leading Space",
                "Trailing Space ",
                "Double  Space",
                "MathCombo-AggBy- 7 Ops 3 Groups 1M Unique Combos  -Static",
                "Path/Separator",
                "Back\\Slash",
                "Comma, Name",
                "Quote's Name",
                "Back`tick",
                "Dot.Name",
                "Under_Score",
                "Colon: Name",
                "Star* Name",
                "Tab\tName",
                "New\nLine",
                "Caf\u00e9 Name",
                maxBase + "a",
                maxBase + "a -Static",
                maxBase + " -Other",
        }) {
            assertThrows(RuntimeException.class, () -> Names.validateBenchmarkName(name), "Should be invalid: " + name);
        }
    }

}
