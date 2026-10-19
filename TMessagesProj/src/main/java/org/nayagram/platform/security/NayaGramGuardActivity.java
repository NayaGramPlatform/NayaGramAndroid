package org.nayagram.platform.security;

/**
 * @deprecated Renamed to {@link NGSafetyActivity}. Preserved for backward compatibility.
 */
@Deprecated
public class NayaGramGuardActivity extends NGSafetyActivity {
    public NayaGramGuardActivity() {
        super(false, false);
    }

    public NayaGramGuardActivity(boolean gateMode) {
        super(gateMode, false);
    }

    public NayaGramGuardActivity(boolean gateMode, boolean startSetup) {
        super(gateMode, startSetup);
    }
}
