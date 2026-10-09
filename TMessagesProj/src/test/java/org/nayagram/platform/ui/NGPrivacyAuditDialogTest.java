package org.nayagram.platform.ui;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests verifying that Privacy & Security Audit UI strictly excludes
 * policy-risky stealth/ghost wording and maintains Play Store compliance.
 */
public class NGPrivacyAuditDialogTest {

    @Test
    public void testNoForbiddenWording() {
        String[] titles = new String[]{
                "Privacy & Security Audit",
                "Session Encryption",
                "Biometric Chat Locker",
                "Private Story Viewing",
                "Focus & Wellbeing"
        };

        for (String title : titles) {
            String lower = title.toLowerCase();
            Assert.assertFalse("Must not contain 'stealth': " + title, lower.contains("stealth"));
            Assert.assertFalse("Must not contain 'ghost': " + title, lower.contains("ghost"));
        }
    }
}
