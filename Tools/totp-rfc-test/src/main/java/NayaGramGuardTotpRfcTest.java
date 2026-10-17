import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.TimeProvider;

/** Standalone known-answer tests for the exact TOTP library used by NayaGram Guard. */
public final class NayaGramGuardTotpRfcTest {
    private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private NayaGramGuardTotpRfcTest() {}

    public static void main(String[] args) throws Exception {
        DefaultCodeGenerator generator = new DefaultCodeGenerator();
        long[] times = {59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L};
        String[] expected = {"287082", "081804", "050471", "005924", "279037", "353130"};
        for (int i = 0; i < times.length; i++) {
            String actual = generator.generate(RFC_SECRET_BASE32, times[i] / 30L);
            if (!expected[i].equals(actual)) {
                throw new AssertionError("RFC 6238 vector failed at " + times[i]
                        + ": expected " + expected[i] + ", got " + actual);
            }
        }

        final long[] fixedTime = {59L};
        TimeProvider clock = new TimeProvider() {
            @Override
            public long getTime() {
                return fixedTime[0];
            }
        };
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(generator, clock);
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(1);
        if (!verifier.isValidCode(RFC_SECRET_BASE32, generator.generate(RFC_SECRET_BASE32, 2L))) {
            throw new AssertionError("Verifier rejected the adjacent +1 time-step code");
        }
        if (verifier.isValidCode(RFC_SECRET_BASE32, generator.generate(RFC_SECRET_BASE32, 3L))) {
            throw new AssertionError("Verifier accepted a code outside the ±1 time-step window");
        }
        System.out.println("PASS: 6 RFC 6238 TOTP vectors and verifier ±1-step window");
    }
}
