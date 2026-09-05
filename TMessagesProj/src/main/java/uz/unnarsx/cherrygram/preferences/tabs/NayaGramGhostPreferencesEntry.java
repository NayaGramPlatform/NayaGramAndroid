package uz.unnarsx.cherrygram.preferences.tabs;

import android.view.View;

import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

import uz.unnarsx.cherrygram.preferences.NayaGramGhostPreferences;

public class NayaGramGhostPreferencesEntry extends UniversalFragment {

    private final int hideOnlineRow = 1;
    private final int hideTypingRow = 2;
    private final int hideReadRow = 3;

    @Override
    protected CharSequence getTitle() {
        return "Ghost Mode \uD83D\uDC7B";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Privacy Ghost"));
        items.add(UItem.asCheckBox(hideOnlineRow, "Hide Online Status", "Show offline even when online", NayaGramGhostPreferences.isHideOnline()));
        items.add(UItem.asCheckBox(hideTypingRow, "Hide Typing...", "Don't show typing status", NayaGramGhostPreferences.isHideTyping()));
        items.add(UItem.asCheckBox(hideReadRow, "Hide Read Receipt", "Don't send blue ticks", NayaGramGhostPreferences.isHideRead()));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == hideOnlineRow) {
            NayaGramGhostPreferences.setHideOnline(!NayaGramGhostPreferences.isHideOnline());
        } else if (item.id == hideTypingRow) {
            NayaGramGhostPreferences.setHideTyping(!NayaGramGhostPreferences.isHideTyping());
        } else if (item.id == hideReadRow) {
            NayaGramGhostPreferences.setHideRead(!NayaGramGhostPreferences.isHideRead());
        }
    }
}
