package org.nayagram.platform.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.TimeProvider;

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

    @Test
    public void acceptsOneAdjacentThirtySecondWindowOnly() throws Exception {
        final long[] fixedTime = {59L};
        TimeProvider clock = new TimeProvider() {
            @Override
            public long getTime() {
                return fixedTime[0];
            }
        };
        DefaultCodeGenerator generator = new DefaultCodeGenerator();
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(generator, clock);
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(1);

        // At t=59 the current counter is 1; counter 2 is within +1, counter 3 is +2.
        assertTrue(verifier.isValidCode(RFC_SECRET_BASE32, generator.generate(RFC_SECRET_BASE32, 2L)));
        assertFalse(verifier.isValidCode(RFC_SECRET_BASE32, generator.generate(RFC_SECRET_BASE32, 3L)));
    }
}
