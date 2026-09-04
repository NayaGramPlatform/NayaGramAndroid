package uz.unnarsx.cherrygram.preferences;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.messenger.R;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.view.View;
import java.util.ArrayList;

/**
 * NayaGram HQ - Navigation Hub
 * CEO: CEONayaGramHQ
 * This connects your NayaGramPreferencesEntry to all sub-menus
 */
public class NayaGramNavigator {

    public static void createGeneral(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramGeneralPreferences());
    }
    public static void createAppearance(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramAppearancePreferences());
    }
    public static void createChats(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramChatsPreferences());
    }
    public static void createPrivacy(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramPrivacyPreferences());
    }
    public static void createAbout(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramAboutPreferences());
    }

    // --- Dummy Preference Screens for now (so build doesn't break) ---
    public static class NayaGramGeneralPreferences extends UniversalFragment {
        @Override protected CharSequence getTitle() { return "NayaGram General"; }
        @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Ghost Mode"));
            items.add(UItem.asCheckBox(1, "Hide Read Status", "", true));
            items.add(UItem.asCheckBox(2, "Hide Typing", "", true));
        }
    }
    public static class NayaGramAppearancePreferences extends UniversalFragment {
        @Override protected CharSequence getTitle() { return "NayaGram Appearance"; }
        @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("NayaGram Theme"));
            items.add(UItem.asButton(1, 0, "App Icon = NayaGram"));
        }
    }
    public static class NayaGramChatsPreferences extends UniversalFragment {
        @Override protected CharSequence getTitle() { return "NayaGram Chats"; }
        @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Ads Control"));
            items.add(UItem.asCheckBox(1, "Hide Sponsored Messages", "", true));
        }
    }
    public static class NayaGramPrivacyPreferences extends UniversalFragment {
        @Override protected CharSequence getTitle() { return "NayaGram Privacy"; }
        @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Security"));
            items.add(UItem.asCheckBox(1, "Anti-Delete Messages", "", false));
        }
    }
    public static class NayaGramAboutPreferences extends UniversalFragment {
        @Override protected CharSequence getTitle() { return "About NayaGram"; }
        @Override protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("CEO"));
            items.add(UItem.asButton(1, 0, "CEONayaGramHQ - NayaGram v1.0"));
            items.add(UItem.asShadow("Alhamdulillah for NayaGram"));
        }
    }
}
