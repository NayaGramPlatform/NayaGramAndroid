package org.nayagram.platform.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.nayagram.platform.AntiDeleteManager;
import org.nayagram.platform.FileLog;
import org.nayagram.platform.MessageDatabase;
import org.nayagram.platform.R;

/**
 * AntiDeletePreferenceFragment - Settings UI for Anti-Delete feature
 * Allows users to enable/disable anti-delete and manage saved messages
 */
public class AntiDeletePreferenceFragment extends PreferenceFragmentCompat 
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private AntiDeleteManager antiDeleteManager;
    private MessageDatabase messageDatabase;
    private SwitchPreference antiDeleteSwitch;
    private Preference clearMessagesPreference;
    private Preference viewDeletedPreference;
    private Preference autoCleanupPreference;
    private Preference saveMediaPreference;
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_anti_delete, rootKey);
        
        initializeManagers();
        setupPreferences();
    }
    
    /**
     * Initialize managers
     */
    private void initializeManagers() {
        antiDeleteManager = AntiDeleteManager.getInstance(getContext());
        messageDatabase = MessageDatabase.getInstance();
    }
    
    /**
     * Setup all preference items
     */
    private void setupPreferences() {
        // Anti-Delete Enable/Disable Switch
        antiDeleteSwitch = findPreference("anti_delete_enabled");
        if (antiDeleteSwitch != null) {
            antiDeleteSwitch.setTitle(R.string.EnableAntiDelete);
            antiDeleteSwitch.setSummary(R.string.AntiDeleteDescription);
            antiDeleteSwitch.setChecked(antiDeleteManager.isAntiDeleteEnabled());
            antiDeleteSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (boolean) newValue;
                antiDeleteManager.setAntiDeleteEnabled(enabled);
                updateSummary(enabled);
                return true;
            });
        }
        
        // View Deleted Messages Preference
        viewDeletedPreference = findPreference("view_deleted_messages");
        if (viewDeletedPreference != null) {
            viewDeletedPreference.setTitle(R.string.ViewDeletedMessages);
            viewDeletedPreference.setOnPreferenceClickListener(preference -> {
                openDeletedMessagesViewer();
                return true;
            });
            updateDeletedMessageCount();
        }
        
        // Clear Deleted Messages Preference
        clearMessagesPreference = findPreference("clear_deleted_messages");
        if (clearMessagesPreference != null) {
            clearMessagesPreference.setTitle(R.string.ClearDeletedMessages);
            clearMessagesPreference.setOnPreferenceClickListener(preference -> {
                showClearConfirmationDialog();
                return true;
            });
        }
        
        // Auto Cleanup Preference
        autoCleanupPreference = findPreference("auto_cleanup_days");
        if (autoCleanupPreference != null) {
            autoCleanupPreference.setTitle(R.string.AutoClearOldMessages);
            autoCleanupPreference.setSummary(R.string.AutoClearOldMessagesInfo);
            autoCleanupPreference.setOnPreferenceClickListener(preference -> {
                showAutoCleanupDialog();
                return true;
            });
        }
        
        // Save Media Preference
        saveMediaPreference = findPreference("save_media_from_deleted");
        if (saveMediaPreference != null) {
            saveMediaPreference.setTitle(R.string.SaveMedia);
            saveMediaPreference.setSummary(R.string.SaveMediaInfo);
            if (saveMediaPreference instanceof SwitchPreference) {
                ((SwitchPreference) saveMediaPreference).setOnPreferenceChangeListener((preference, newValue) -> {
                    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                    prefs.edit().putBoolean("save_deleted_media", (boolean) newValue).apply();
                    return true;
                });
            }
        }
    }
    
    /**
     * Update the summary text when anti-delete is toggled
     */
    private void updateSummary(boolean enabled) {
        if (antiDeleteSwitch != null) {
            if (enabled) {
                antiDeleteSwitch.setSummary(R.string.AntiDeleteOn);
            } else {
                antiDeleteSwitch.setSummary(R.string.AntiDeleteOff);
            }
        }
        updateDeletedMessageCount();
    }
    
    /**
     * Update the count of deleted messages in summary
     */
    private void updateDeletedMessageCount() {
        if (viewDeletedPreference != null) {
            int count = messageDatabase.getDeletedMessageCount();
            String summary = String.format(
                    getString(R.string.DeletedMessagesSaved),
                    count
            );
            viewDeletedPreference.setSummary(summary);
        }
    }
    
    /**
     * Open viewer for deleted messages
     */
    private void openDeletedMessagesViewer() {
        int count = messageDatabase.getDeletedMessageCount();
        if (count == 0) {
            Toast.makeText(getContext(), R.string.NoDeletedMessages, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Navigate to DeletedMessagesActivity
        Toast.makeText(getContext(), 
                String.format(getString(R.string.DeletedMessagesSaved), count),
                Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show confirmation dialog for clearing deleted messages
     */
    private void showClearConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle(R.string.ClearDeletedMessages)
                .setMessage(R.string.ClearDeletedMessagesConfirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    antiDeleteManager.clearAllDeletedMessages();
                    Toast.makeText(getContext(), 
                            R.string.ClearDeletedMessagesSuccess,
                            Toast.LENGTH_SHORT).show();
                    updateDeletedMessageCount();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    /**
     * Show dialog for auto-cleanup settings
     */
    private void showAutoCleanupDialog() {
        final String[] options = {
                getString(R.string.KeepMessagesFor7Days),
                getString(R.string.KeepMessagesFor30Days),
                getString(R.string.KeepMessagesFor90Days),
                getString(R.string.KeepMessagesForever)
        };
        
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle(R.string.AutoClearOldMessages)
                .setItems(options, (dialog, which) -> {
                    int days = 0;
                    switch (which) {
                        case 0:
                            days = 7;
                            break;
                        case 1:
                            days = 30;
                            break;
                        case 2:
                            days = 90;
                            break;
                        case 3:
                            days = 0; // 0 means keep forever
                            break;
                    }
                    
                    SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                    prefs.edit().putInt("auto_cleanup_days", days).apply();
                    
                    if (days > 0) {
                        messageDatabase.clearOldDeletedMessages(days);
                    }
                    
                    updateAutoCleanupSummary(which);
                })
                .show();
    }
    
    /**
     * Update auto-cleanup summary
     */
    private void updateAutoCleanupSummary(int selectedIndex) {
        if (autoCleanupPreference != null) {
            String[] options = {
                    getString(R.string.KeepMessagesFor7Days),
                    getString(R.string.KeepMessagesFor30Days),
                    getString(R.string.KeepMessagesFor90Days),
                    getString(R.string.KeepMessagesForever)
            };
            autoCleanupPreference.setSummary(options[selectedIndex]);
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("anti_delete_enabled".equals(key)) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            updateSummary(enabled);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        updateDeletedMessageCount();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
