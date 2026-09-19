package org.nayagram.platform.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.nayagram.platform.GhostModeManager;
import org.nayagram.platform.R;

/**
 * GhostModePreferenceFragment - Settings UI for Ghost Mode feature
 * Allows users to hide typing status, online status, forward tags, and read receipts
 */
public class GhostModePreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private GhostModeManager ghostModeManager;
    private SwitchPreference ghostModeSwitch;
    private SwitchPreference hideTypingSwitch;
    private SwitchPreference hideOnlineSwitch;
    private SwitchPreference hideForwardSwitch;
    private SwitchPreference hideReadReceiptsSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_ghost_mode, rootKey);
        
        ghostModeManager = GhostModeManager.getInstance(getContext());
        setupPreferences();
    }

    private void setupPreferences() {
        // Master Ghost Mode Toggle
        ghostModeSwitch = findPreference("ghost_mode_enabled");
        if (ghostModeSwitch != null) {
            ghostModeSwitch.setTitle(R.string.GhostModeEnabled);
            ghostModeSwitch.setSummary(R.string.GhostModeDescription);
            ghostModeSwitch.setChecked(ghostModeManager.isGhostModeEnabled());
            ghostModeSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (boolean) newValue;
                ghostModeManager.setGhostModeEnabled(enabled);
                updateAllSwitches();
                Toast.makeText(getContext(),
                        enabled ? R.string.GhostModeOn : R.string.GhostModeOff,
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // Hide Typing Status
        hideTypingSwitch = findPreference("hide_typing_status");
        if (hideTypingSwitch != null) {
            hideTypingSwitch.setTitle(R.string.HideTypingStatus);
            hideTypingSwitch.setSummary(R.string.HideTypingStatusInfo);
            hideTypingSwitch.setChecked(ghostModeManager.isHideTypingStatus());
            hideTypingSwitch.setEnabled(ghostModeManager.isGhostModeEnabled());
            hideTypingSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                ghostModeManager.setHideTypingStatus((boolean) newValue);
                return true;
            });
        }

        // Hide Online Status
        hideOnlineSwitch = findPreference("hide_online_status");
        if (hideOnlineSwitch != null) {
            hideOnlineSwitch.setTitle(R.string.HideOnlineStatus);
            hideOnlineSwitch.setSummary(R.string.HideOnlineStatusInfo);
            hideOnlineSwitch.setChecked(ghostModeManager.isHideOnlineStatus());
            hideOnlineSwitch.setEnabled(ghostModeManager.isGhostModeEnabled());
            hideOnlineSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                ghostModeManager.setHideOnlineStatus((boolean) newValue);
                return true;
            });
        }

        // Hide Forward Tag
        hideForwardSwitch = findPreference("hide_forward_tag");
        if (hideForwardSwitch != null) {
            hideForwardSwitch.setTitle(R.string.HideForwardTag);
            hideForwardSwitch.setSummary(R.string.HideForwardTagInfo);
            hideForwardSwitch.setChecked(ghostModeManager.isHideForwardTag());
            hideForwardSwitch.setEnabled(ghostModeManager.isGhostModeEnabled());
            hideForwardSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                ghostModeManager.setHideForwardTag((boolean) newValue);
                return true;
            });
        }

        // Hide Read Receipts
        hideReadReceiptsSwitch = findPreference("hide_read_receipts");
        if (hideReadReceiptsSwitch != null) {
            hideReadReceiptsSwitch.setTitle(R.string.HideReadReceipts);
            hideReadReceiptsSwitch.setSummary(R.string.HideReadReceiptsInfo);
            hideReadReceiptsSwitch.setChecked(ghostModeManager.isHideReadReceipts());
            hideReadReceiptsSwitch.setEnabled(ghostModeManager.isGhostModeEnabled());
            hideReadReceiptsSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                ghostModeManager.setHideReadReceipts((boolean) newValue);
                return true;
            });
        }
    }

    private void updateAllSwitches() {
        boolean enabled = ghostModeManager.isGhostModeEnabled();
        if (hideTypingSwitch != null) hideTypingSwitch.setEnabled(enabled);
        if (hideOnlineSwitch != null) hideOnlineSwitch.setEnabled(enabled);
        if (hideForwardSwitch != null) hideForwardSwitch.setEnabled(enabled);
        if (hideReadReceiptsSwitch != null) hideReadReceiptsSwitch.setEnabled(enabled);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Preferences will be updated automatically by SwitchPreference
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
