package com.eriwen.gradle

import spock.lang.Specification

class DigestAlgorithmTest extends Specification {
    def "MD5 file suffix is .md5"() {
        expect:
        DigestAlgorithm.MD5.suffix() == "md5"
    }
}
