package uz.unnarsx.cherrygram.preferences;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * NayaGram HQ - Navigation Hub
 * CEO: CEONayaGramHQ
 * Connects NayaGramPreferencesEntry to all sub-menus.
 *
 * NOTE: the placeholder screens below (NayaGramGeneralPage, NayaGramAppearancePage, etc.)
 * are temporary "coming soon" pages. They are deliberately named differently from the real
 * data classes (e.g. NayaGramGeneralPreferences, which stores the No-Ads/Premium switches)
 * so the two are never confused with each other.
 */
public class NayaGramNavigator {

    public static void createGeneral(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramGeneralPage());
    }

    public static void createAppearance(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramAppearancePage());
    }

    public static void createChats(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramChatsPage());
    }

    public static void createCamera(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramCameraPage());
    }

    public static void createExperimental(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramExperimentalPage());
    }

    public static void createPrivacy(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramPrivacyPage());
    }

    public static void createDonate(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramDonatePage());
    }

    public static void createAbout(BaseFragment fragment) {
        fragment.presentFragment(new NayaGramAboutPage());
    }

    // ---------------------------------------------------------------
    // Placeholder screens (so the build doesn't break while the real
    // screens are built one at a time). Replace each with a real
    // screen whenever that feature is ready.
    // ---------------------------------------------------------------

    public static class NayaGramGeneralPage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "NayaGram General";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("General"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramAppearancePage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "NayaGram Appearance";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Appearance"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramChatsPage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "NayaGram Chats";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Chats"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramCameraPage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "NayaGram Camera";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Camera"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramExperimentalPage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "NayaGram Experimental";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Experimental"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramPrivacyPage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "NayaGram Privacy";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Security"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramDonatePage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "Support NayaGram";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("Support"));
            items.add(UItem.asShadow("Coming soon"));
        }
    }

    public static class NayaGramAboutPage extends UniversalFragment {
        @Override
        protected CharSequence getTitle() {
            return "About NayaGram";
        }

        @Override
        protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            items.add(UItem.asHeader("CEO"));
            items.add(UItem.asButton(1, 0, "CEONayaGramHQ - NayaGram v1.0"));
            items.add(UItem.asShadow("Alhamdulillah for NayaGram"));
        }
    }
}
