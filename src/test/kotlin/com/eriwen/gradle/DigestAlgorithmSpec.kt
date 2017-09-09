package com.eriwen.gradle

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals

class DigestAlgorithmSpec : Spek({
    describe("DigestAlgorithm") {
        it("calculates file suffix") {
            assertEquals("md5", DigestAlgorithm.MD5.suffix())
        }
    }
})
