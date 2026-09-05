package uz.unnarsx.cherrygram.preferences.tabs;

import android.content.Context;
import android.view.View;
import org.telegram.messenger.R;
import java.util.ArrayList;
import java.util.List;
import uz.unnarsx.cherrygram.preferences.NayaGramGhostPreferences;
import uz.unnarsx.cherrygram.preferences.base.BasePreferencesEntry;
import uz.unnarsx.cherrygram.preferences.items.UItem;

public class NayaGramGhostPreferencesEntry extends BasePreferencesEntry {

    private final int hideOnlineRow = 0;
    private final int hideTypingRow = 1;
    private final int hideReadRow = 2;

    @Override
    public String getTitle() {
        return "Ghost Mode \uD83D\uDC7B";
    }

    @Override
    protected void fillItems(List<UItem> items) {
        items.add(UItem.asHeader("Privacy Ghost"));
        items.add(UItem.asCheckBox(hideOnlineRow, "Hide Online Status", "Show offline even when online", NayaGramGhostPreferences.isHideOnlineEnabled()).setIcon(R.drawable.msg_online));
        items.add(UItem.asCheckBox(hideTypingRow, "Hide Typing...", "Don't show typing status", NayaGramGhostPreferences.isHideTypingEnabled()).setIcon(R.drawable.msg_typing));
        items.add(UItem.asCheckBox(hideReadRow, "Hide Read Receipt", "Don't send blue ticks", NayaGramGhostPreferences.isHideReadEnabled()).setIcon(R.drawable.msg_read));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == hideOnlineRow) {
            NayaGramGhostPreferences.setHideOnlineEnabled(!NayaGramGhostPreferences.isHideOnlineEnabled());
        } else if (item.id == hideTypingRow) {
            NayaGramGhostPreferences.setHideTypingEnabled(!NayaGramGhostPreferences.isHideTypingEnabled());
        } else if (item.id == hideReadRow) {
            NayaGramGhostPreferences.setHideReadEnabled(!NayaGramGhostPreferences.isHideReadEnabled());
        }
        updateRow(item.id);
    }
}
