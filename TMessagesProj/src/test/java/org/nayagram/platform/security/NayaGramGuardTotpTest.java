package org.nayagram.platform.security;

import static org.junit.Assert.assertEquals;

import dev.samstevens.totp.code.DefaultCodeGenerator;

import org.junit.Test;

/** RFC 6238 Appendix B SHA-1 vectors, evaluated by the actual java-totp dependency. */
public class NayaGramGuardTotpTest {
    private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    public void matchesRfc6238Sha1SixDigitVectors() throws Exception {
        DefaultCodeGenerator generator = new DefaultCodeGenerator();
        assertEquals("287082", generator.generate(RFC_SECRET_BASE32, 59 / 30));
        assertEquals("081804", generator.generate(RFC_SECRET_BASE32, 1111111109L / 30));
        assertEquals("050471", generator.generate(RFC_SECRET_BASE32, 1111111111L / 30));
        assertEquals("005924", generator.generate(RFC_SECRET_BASE32, 1234567890L / 30));
        assertEquals("279037", generator.generate(RFC_SECRET_BASE32, 2000000000L / 30));
        assertEquals("353130", generator.generate(RFC_SECRET_BASE32, 20000000000L / 30));
    }
}
