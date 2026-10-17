package com.github.minecraftschurlimods.helperplugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class McVersionTest {
    @ParameterizedTest
    @CsvSource(
        "1.20.1, false",
        "1.21, false",
        "1.21.3, false",
        "1.21.4, true",
        "1.21.11, true",
        "1.21.4-pre1, true",
        "26.1, true",
        "26.1.2, true",
        "1.22, true",
        "garbage, false",
    )
    fun splitDataRunsForVersion(ver: String, expected: Boolean) {
        assertEquals(expected, usesSplitDataRuns(ver))
    }

    @ParameterizedTest
    @CsvSource(
        "1.20, true",
        "1.20.4, true",
        "1.20.5, true",
        "1.20.6, false",
        "1.21, false",
        "1.21.4, false",
        "26.1, false",
        "1.19.2, true",
        "garbage, false",
    )
    fun legacyModsTomlForVersion(ver: String, expected: Boolean) {
        assertEquals(expected, usesLegacyModsToml(ver))
    }
}
