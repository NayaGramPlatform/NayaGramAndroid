package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nayagram.platform.AntiDeleteManager;
import org.nayagram.platform.GhostModeManager;
import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * NGSettingsActivity - Dedicated NayaGram Feature Hub & Settings
 * Inspired by Nekogram, Cherrygram, and Novagram.
 */
public class NGSettingsActivity extends BaseFragment {

    public static final int TYPE_FEATURES_HUB = 0;
    public static final int TYPE_GHOST_MODE = 1;
    public static final int TYPE_STUDIO = 2;
    public static final int TYPE_ANTI_DELETE = 3;

    private static final int ITEM_SEARCH = 1;
    private static final int ITEM_MORE = 2;
    private static final int SUB_ITEM_RESET = 3;

    private final int currentType;
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private ActionBarMenuItem searchItem;

    private boolean isSearching;
    private String searchQuery = "";
    private final ArrayList<SearchItem> searchResults = new ArrayList<>();
    private final ArrayList<SearchItem> allFeatures = new ArrayList<>();

    private int rowCount;

    // Feature Hub Rows
    private int hubHeaderStories;
    private int hubRowAnonymousStories;
    private int hubRowStorySaver;
    private int hubHeaderStealth;
    private int hubRowGhostMaster;
    private int hubRowHideTyping;
    private int hubRowHideOnline;
    private int hubRowHideReadReceipts;
    private int hubHeaderMessaging;
    private int hubRowForwardNoQuote;
    private int hubRowAntiDelete;
    private int hubHeaderProtection;
    private int hubRowConfirmActions;
    private int hubRowShowIdDc;
    private int hubRowResetDefaults;
    private int hubHeaderStudio;
    private int hubRowStudio;
    private int hubFooterCopyrightRow;

    // Standalone mode rows
    private int headerRow;
    private int masterSwitchRow;
    private int hideTypingRow;
    private int hideOnlineRow;
    private int hideReadReceiptsRow;
    private int hideForwardTagRow;
    private int infoRow;

    // Studio standalone rows
    private int studioHeaderRow;
    private int studioAppIdRow;
    private int studioVersionRow;
    private int studioLogsRow;

    private static class SearchItem {
        final int id;
        final String title;
        final String subtitle;
        final boolean isCheck;

        SearchItem(int id, String title, String subtitle, boolean isCheck) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.isCheck = isCheck;
        }
    }

    public NGSettingsActivity() {
        this(TYPE_FEATURES_HUB);
    }

    public NGSettingsActivity(int type) {
        super();
        this.currentType = type;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        initSearchItems();
        updateRows();
        return true;
    }

    private void initSearchItems() {
        allFeatures.clear();
        allFeatures.add(new SearchItem(1, "Ghost Mode", "Main stealth switch for all privacy hooks", true));
        allFeatures.add(new SearchItem(2, "Hide Typing Status", "Contacts will not see when you are typing", true));
        allFeatures.add(new SearchItem(3, "Hide Online Status", "Keep last seen timestamp undisturbed", true));
        allFeatures.add(new SearchItem(4, "Hide Read Receipts", "Read messages without marking as seen", true));
        allFeatures.add(new SearchItem(5, "Anonymous Stories", "View user stories without leaving a view trace", true));
        allFeatures.add(new SearchItem(6, "Story Saver", "Download and save photo/video stories locally", true));
        allFeatures.add(new SearchItem(7, "Forward Without Quote", "Direct forward without original sender name", true));
        allFeatures.add(new SearchItem(8, "Anti-Delete Messages", "Save incoming messages locally before deletion", true));
        allFeatures.add(new SearchItem(9, "Confirm Actions", "Confirmation before placing calls or sending notes", true));
        allFeatures.add(new SearchItem(10, "Show ID & Datacenter", "Display numeric Telegram ID & DC in user profile", true));
        allFeatures.add(new SearchItem(11, "Reset All Features", "Restore all NG Features to default values", false));
        
        long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (BuildVars.isNgStudioAllowed(clientUserId)) {
            allFeatures.add(new SearchItem(12, "NG Control Dashboard", "Authorized developer telemetry & diagnostics", false));
            allFeatures.add(new SearchItem(6, "Story Saver (Admin Only)", "Download stories locally (NG Control)", true));
            allFeatures.add(new SearchItem(8, "Anti-Delete Messages (Admin Only)", "Preserve deleted messages (NG Control)", true));
        }
    }

    private void updateRows() {
        rowCount = 0;

        hubHeaderStories = -1;
        hubRowAnonymousStories = -1;
        hubRowStorySaver = -1;
        hubHeaderStealth = -1;
        hubRowGhostMaster = -1;
        hubRowHideTyping = -1;
        hubRowHideOnline = -1;
        hubRowHideReadReceipts = -1;
        hubHeaderMessaging = -1;
        hubRowForwardNoQuote = -1;
        hubRowAntiDelete = -1;
        hubHeaderProtection = -1;
        hubRowConfirmActions = -1;
        hubRowShowIdDc = -1;
        hubRowResetDefaults = -1;
        hubHeaderStudio = -1;
        hubRowStudio = -1;
        hubFooterCopyrightRow = -1;

        headerRow = -1;
        masterSwitchRow = -1;
        hideTypingRow = -1;
        hideOnlineRow = -1;
        hideReadReceiptsRow = -1;
        hideForwardTagRow = -1;
        infoRow = -1;

        studioHeaderRow = -1;
        studioAppIdRow = -1;
        studioVersionRow = -1;
        studioLogsRow = -1;

        if (currentType == TYPE_FEATURES_HUB) {
            hubHeaderStories = rowCount++;
            hubRowAnonymousStories = rowCount++;
            hubRowStorySaver = rowCount++;

            hubHeaderStealth = rowCount++;
            hubRowGhostMaster = rowCount++;
            hubRowHideTyping = rowCount++;
            hubRowHideOnline = rowCount++;
            hubRowHideReadReceipts = rowCount++;

            hubHeaderMessaging = rowCount++;
            hubRowForwardNoQuote = rowCount++;
            hubRowAntiDelete = rowCount++;

            hubHeaderProtection = rowCount++;
            hubRowConfirmActions = rowCount++;
            hubRowShowIdDc = rowCount++;
            hubRowResetDefaults = rowCount++;

            long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
            if (BuildVars.isNgStudioAllowed(clientUserId)) {
                hubHeaderStudio = rowCount++;
                hubRowStudio = rowCount++;
            }

            hubFooterCopyrightRow = rowCount++;
        } else if (currentType == TYPE_GHOST_MODE) {
            headerRow = rowCount++;
            masterSwitchRow = rowCount++;
            hideTypingRow = rowCount++;
            hideOnlineRow = rowCount++;
            hideReadReceiptsRow = rowCount++;
            hideForwardTagRow = rowCount++;
            infoRow = rowCount++;
        } else if (currentType == TYPE_STUDIO) {
            studioHeaderRow = rowCount++;
            studioAppIdRow = rowCount++;
            studioVersionRow = rowCount++;
            studioLogsRow = rowCount++;
            infoRow = rowCount++;
        } else if (currentType == TYPE_ANTI_DELETE) {
            headerRow = rowCount++;
            masterSwitchRow = rowCount++;
            infoRow = rowCount++;
        }
    }

    private void filterFeatures(String query) {
        searchResults.clear();
        if (query == null || query.trim().isEmpty()) {
            isSearching = false;
        } else {
            isSearching = true;
            String lower = query.trim().toLowerCase(Locale.US);
            for (SearchItem item : allFeatures) {
                if (item.title.toLowerCase(Locale.US).contains(lower) || item.subtitle.toLowerCase(Locale.US).contains(lower)) {
                    searchResults.add(item);
                }
            }
        }
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void showResetConfirmationDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Reset NG Features");
        builder.setMessage("Are you sure you want to restore all Naya Features (Ghost Mode, Anti-Delete, Stories, and Privacy) back to their defaults?");
        builder.setPositiveButton("Reset", (dialog, which) -> {
            NayaConfig.getInstance().resetToDefaults();
            GhostModeManager.getInstance().resetAllSettings();
            AntiDeleteManager.getInstance().setAntiDeleteEnabled(false);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (getParentActivity() != null) {
                BulletinFactory.of(NGSettingsActivity.this).createSimpleBulletin(R.raw.done, "All features reset to default").show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        if (currentType == TYPE_FEATURES_HUB) {
            actionBar.setTitle("𝐍𝐆 𝐅𝐞𝐚𝐭𝐮𝐫𝐞");
        } else if (currentType == TYPE_GHOST_MODE) {
            actionBar.setTitle("Ghost Mode");
        } else if (currentType == TYPE_STUDIO) {
            actionBar.setTitle("NG Control");
        } else {
            actionBar.setTitle("Anti-Delete");
        }

        ActionBarMenu menu = actionBar.createMenu();

        if (currentType == TYPE_FEATURES_HUB) {
            searchItem = menu.addItem(ITEM_SEARCH, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                @Override
                public void onSearchExpand() {
                    isSearching = true;
                }

                @Override
                public void onSearchCollapse() {
                    isSearching = false;
                    searchQuery = "";
                    filterFeatures("");
                }

                @Override
                public void onTextChanged(EditText editText) {
                    searchQuery = editText.getText().toString();
                    filterFeatures(searchQuery);
                }
            });
            searchItem.setSearchFieldHint(LocaleController.getString("Search", R.string.Search));

            ActionBarMenuItem otherItem = menu.addItem(ITEM_MORE, R.drawable.ic_ab_other);
            otherItem.addSubItem(SUB_ITEM_RESET, R.drawable.msg_reset, "Reset to defaults");
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == SUB_ITEM_RESET) {
                    showResetConfirmationDialog();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(80));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            GhostModeManager gm = GhostModeManager.getInstance();
            NayaConfig cfg = NayaConfig.getInstance();

            if (isSearching) {
                if (position >= 0 && position < searchResults.size()) {
                    SearchItem item = searchResults.get(position);
                    switch (item.id) {
                        case 1:
                            gm.setGhostModeEnabled(!gm.isGhostModeEnabled());
                            break;
                        case 2:
                            gm.setHideTypingStatus(!gm.isHideTypingStatus());
                            break;
                        case 3:
                            gm.setHideOnlineStatus(!gm.isHideOnlineStatus());
                            break;
                        case 4:
                            gm.setHideReadReceipts(!gm.isHideReadReceipts());
                            break;
                        case 5:
                            cfg.setAnonymousStories(!cfg.isAnonymousStories());
                            break;
                        case 6:
                            cfg.setStorySaverEnabled(!cfg.isStorySaverEnabled());
                            break;
                        case 7:
                            cfg.setForwardWithoutQuote(!cfg.isForwardWithoutQuote());
                            break;
                        case 8:
                            AntiDeleteManager.getInstance().setAntiDeleteEnabled(!AntiDeleteManager.getInstance().isAntiDeleteEnabled());
                            break;
                        case 9:
                            cfg.setConfirmActions(!cfg.isConfirmActions());
                            break;
                        case 10:
                            cfg.setShowIdAndDc(!cfg.isShowIdAndDc());
                            break;
                        case 11:
                            showResetConfirmationDialog();
                            return;
                        case 12:
                            presentFragment(new NGSettingsActivity(TYPE_STUDIO));
                            return;
                    }
                    if (listAdapter != null) listAdapter.notifyDataSetChanged();
                }
                return;
            }

            if (currentType == TYPE_FEATURES_HUB) {
                if (position == hubRowAnonymousStories) {
                    cfg.setAnonymousStories(!cfg.isAnonymousStories());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(cfg.isAnonymousStories());
                    }
                } else if (position == hubRowStorySaver) {
                    cfg.setStorySaverEnabled(!cfg.isStorySaverEnabled());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(cfg.isStorySaverEnabled());
                    }
                } else if (position == hubRowGhostMaster) {
                    gm.setGhostModeEnabled(!gm.isGhostModeEnabled());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isGhostModeEnabled());
                    }
                    if (listAdapter != null) listAdapter.notifyDataSetChanged();
                } else if (position == hubRowHideTyping) {
                    gm.setHideTypingStatus(!gm.isHideTypingStatus());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideTypingStatus());
                    }
                } else if (position == hubRowHideOnline) {
                    gm.setHideOnlineStatus(!gm.isHideOnlineStatus());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideOnlineStatus());
                    }
                } else if (position == hubRowHideReadReceipts) {
                    gm.setHideReadReceipts(!gm.isHideReadReceipts());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideReadReceipts());
                    }
                } else if (position == hubRowForwardNoQuote) {
                    cfg.setForwardWithoutQuote(!cfg.isForwardWithoutQuote());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(cfg.isForwardWithoutQuote());
                    }
                } else if (position == hubRowAntiDelete) {
                    AntiDeleteManager adm = AntiDeleteManager.getInstance();
                    adm.setAntiDeleteEnabled(!adm.isAntiDeleteEnabled());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(adm.isAntiDeleteEnabled());
                    }
                } else if (position == hubRowConfirmActions) {
                    cfg.setConfirmActions(!cfg.isConfirmActions());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(cfg.isConfirmActions());
                    }
                } else if (position == hubRowShowIdDc) {
                    cfg.setShowIdAndDc(!cfg.isShowIdAndDc());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(cfg.isShowIdAndDc());
                    }
                } else if (position == hubRowResetDefaults) {
                    showResetConfirmationDialog();
                } else if (position == hubRowStudio) {
                    presentFragment(new NGSettingsActivity(TYPE_STUDIO));
                }
            } else if (currentType == TYPE_GHOST_MODE) {
                if (position == masterSwitchRow) {
                    gm.setGhostModeEnabled(!gm.isGhostModeEnabled());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isGhostModeEnabled());
                    }
                    if (listAdapter != null) listAdapter.notifyDataSetChanged();
                } else if (position == hideTypingRow) {
                    gm.setHideTypingStatus(!gm.isHideTypingStatus());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideTypingStatus());
                    }
                } else if (position == hideOnlineRow) {
                    gm.setHideOnlineStatus(!gm.isHideOnlineStatus());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideOnlineStatus());
                    }
                } else if (position == hideReadReceiptsRow) {
                    gm.setHideReadReceipts(!gm.isHideReadReceipts());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideReadReceipts());
                    }
                } else if (position == hideForwardTagRow) {
                    gm.setHideForwardTag(!gm.isHideForwardTag());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isHideForwardTag());
                    }
                }
            } else if (currentType == TYPE_ANTI_DELETE) {
                if (position == masterSwitchRow) {
                    AntiDeleteManager adm = AntiDeleteManager.getInstance();
                    adm.setAntiDeleteEnabled(!adm.isAntiDeleteEnabled());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(adm.isAntiDeleteEnabled());
                    }
                }
            }
        });

        return fragmentView;
    }

    private CharSequence createNayaCopyrightSpan() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        boolean isDark = Theme.isCurrentThemeDark();
        int greenColor = isDark ? 0xFF4ADE80 : 0xFF16A34A;
        int purpleColor = isDark ? 0xFFC084FC : 0xFF7C3AED;
        int blueColor = isDark ? 0xFF60A5FA : 0xFF2563EB;

        // Line 1: © {YEAR} 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦. All rights reserved.
        int start = ssb.length();
        ssb.append("© ").append(String.valueOf(currentYear)).append(" ");
        ssb.setSpan(new ForegroundColorSpan(greenColor), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = ssb.length();
        ssb.append("𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 ");
        ssb.setSpan(new ForegroundColorSpan(purpleColor), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = ssb.length();
        ssb.append("𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦");
        ssb.setSpan(new ForegroundColorSpan(blueColor), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = ssb.length();
        ssb.append(". All rights reserved.\n");
        ssb.setSpan(new ForegroundColorSpan(greenColor), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Line 2: Built with ❤️ in Bangladesh 🇧🇩 - [CurrentYear]
        start = ssb.length();
        ssb.append("Built with ❤️ in Bangladesh 🇧🇩 - ").append(String.valueOf(currentYear));
        ssb.setSpan(new ForegroundColorSpan(blueColor), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Centered alignment
        ssb.setSpan(new AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            if (isSearching) {
                return searchResults.size();
            }
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            if (isSearching) {
                return true;
            }
            int pos = holder.getAdapterPosition();
            if (currentType == TYPE_FEATURES_HUB) {
                if (pos == hubRowAnonymousStories || pos == hubRowStorySaver || pos == hubRowGhostMaster ||
                    pos == hubRowForwardNoQuote || pos == hubRowAntiDelete || pos == hubRowConfirmActions ||
                    pos == hubRowShowIdDc || pos == hubRowResetDefaults || pos == hubRowStudio) {
                    return true;
                }
                GhostModeManager gm = GhostModeManager.getInstance();
                return gm.isGhostModeEnabled() && (pos == hubRowHideTyping || pos == hubRowHideOnline || pos == hubRowHideReadReceipts);
            } else if (currentType == TYPE_GHOST_MODE) {
                if (pos == masterSwitchRow) return true;
                GhostModeManager gm = GhostModeManager.getInstance();
                return gm.isGhostModeEnabled() && (pos == hideTypingRow || pos == hideOnlineRow || pos == hideReadReceiptsRow || pos == hideForwardTagRow);
            } else if (currentType == TYPE_ANTI_DELETE) {
                return pos == masterSwitchRow;
            }
            return false;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                default:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            GhostModeManager gm = GhostModeManager.getInstance();
            NayaConfig cfg = NayaConfig.getInstance();

            if (isSearching) {
                if (position >= 0 && position < searchResults.size()) {
                    SearchItem item = searchResults.get(position);
                    if (item.isCheck) {
                        TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                        boolean checked = false;
                        switch (item.id) {
                            case 1: checked = gm.isGhostModeEnabled(); break;
                            case 2: checked = gm.isHideTypingStatus(); break;
                            case 3: checked = gm.isHideOnlineStatus(); break;
                            case 4: checked = gm.isHideReadReceipts(); break;
                            case 5: checked = cfg.isAnonymousStories(); break;
                            case 6: checked = cfg.isStorySaverEnabled(); break;
                            case 7: checked = cfg.isForwardWithoutQuote(); break;
                            case 8: checked = AntiDeleteManager.getInstance().isAntiDeleteEnabled(); break;
                            case 9: checked = cfg.isConfirmActions(); break;
                            case 10: checked = cfg.isShowIdAndDc(); break;
                        }
                        checkCell.setTextAndCheck(item.title, checked, position != searchResults.size() - 1);
                    } else {
                        TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                        textCell.setTextAndValue(item.title, item.subtitle, position != searchResults.size() - 1);
                    }
                }
                return;
            }

            switch (holder.getItemViewType()) {
                case 0:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (currentType == TYPE_FEATURES_HUB) {
                        if (position == hubHeaderStories) {
                            headerCell.setText("Story Features");
                        } else if (position == hubHeaderStealth) {
                            headerCell.setText("Ghost & Stealth Settings");
                        } else if (position == hubHeaderMessaging) {
                            headerCell.setText("Chat & Message Tools");
                        } else if (position == hubHeaderProtection) {
                            headerCell.setText("Protection & Confirmation");
                        } else if (position == hubHeaderStudio) {
                            headerCell.setText("NG Studio / Developer");
                        }
                    } else if (currentType == TYPE_GHOST_MODE) {
                        headerCell.setText("Stealth Privacy Options");
                    } else if (currentType == TYPE_STUDIO) {
                        headerCell.setText("Diagnostic Information");
                    } else {
                        headerCell.setText("Message Protection");
                    }
                    break;
                case 1:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (currentType == TYPE_FEATURES_HUB) {
                        if (position == hubRowAnonymousStories) {
                            checkCell.setTextAndCheck("Anonymous Story Viewing", cfg.isAnonymousStories(), true);
                        } else if (position == hubRowStorySaver) {
                            checkCell.setTextAndCheck("Enable Story Saver", cfg.isStorySaverEnabled(), false);
                        } else if (position == hubRowGhostMaster) {
                            checkCell.setTextAndCheck("Ghost Mode Master Switch", gm.isGhostModeEnabled(), true);
                        } else if (position == hubRowHideTyping) {
                            checkCell.setTextAndCheck("Hide Typing Status", gm.isHideTypingStatus(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hubRowHideOnline) {
                            checkCell.setTextAndCheck("Hide Online Status", gm.isHideOnlineStatus(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hubRowHideReadReceipts) {
                            checkCell.setTextAndCheck("Hide Read Receipts", gm.isHideReadReceipts(), false);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hubRowForwardNoQuote) {
                            checkCell.setTextAndCheck("Forward Without Quote", cfg.isForwardWithoutQuote(), true);
                        } else if (position == hubRowAntiDelete) {
                            checkCell.setTextAndCheck("Enable Anti-Delete", AntiDeleteManager.getInstance().isAntiDeleteEnabled(), false);
                        } else if (position == hubRowConfirmActions) {
                            checkCell.setTextAndCheck("Confirm Calls & Audio Notes", cfg.isConfirmActions(), true);
                        } else if (position == hubRowShowIdDc) {
                            checkCell.setTextAndCheck("Show User ID & Datacenter", cfg.isShowIdAndDc(), true);
                        }
                    } else if (currentType == TYPE_GHOST_MODE) {
                        if (position == masterSwitchRow) {
                            checkCell.setTextAndCheck("Enable Ghost Mode", gm.isGhostModeEnabled(), true);
                        } else if (position == hideTypingRow) {
                            checkCell.setTextAndCheck("Hide Typing Status", gm.isHideTypingStatus(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hideOnlineRow) {
                            checkCell.setTextAndCheck("Hide Online Status", gm.isHideOnlineStatus(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hideReadReceiptsRow) {
                            checkCell.setTextAndCheck("Hide Read Receipts", gm.isHideReadReceipts(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hideForwardTagRow) {
                            checkCell.setTextAndCheck("Hide Forward Tag", gm.isHideForwardTag(), false);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        }
                    } else if (currentType == TYPE_ANTI_DELETE) {
                        if (position == masterSwitchRow) {
                            checkCell.setTextAndCheck("Enable Anti-Delete", AntiDeleteManager.getInstance().isAntiDeleteEnabled(), false);
                        }
                    }
                    break;
                case 2:
                    TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                    if (currentType == TYPE_STUDIO) {
                        if (position == studioAppIdRow) {
                            detailCell.setTextAndValue("Telemetry Access", "Developer authorization verified", true);
                        } else if (position == studioVersionRow) {
                            detailCell.setTextAndValue("Client Build", "NayaGram for Android v1.0.0 (1)", true);
                        } else if (position == studioLogsRow) {
                            detailCell.setTextAndValue("Logging Level", "Standard / Release", true);
                        }
                    }
                    break;
                case 4:
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == hubRowResetDefaults) {
                        textCell.setTextAndValue("Reset All Features", "Restore defaults", position != hubFooterCopyrightRow - 1);
                    } else if (position == hubRowStudio) {
                        textCell.setTextAndValue("NG Control Dashboard", "Authorized access only", true);
                    }
                    break;
                case 3:
                default:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (currentType == TYPE_FEATURES_HUB) {
                        if (position == hubFooterCopyrightRow) {
                            infoCell.setText(createNayaCopyrightSpan());
                        }
                    } else if (currentType == TYPE_GHOST_MODE) {
                        infoCell.setText("When Ghost Mode is enabled, contacts will not see you online or typing, and message reads will remain undisclosed.");
                    } else if (currentType == TYPE_STUDIO) {
                        infoCell.setText("NG Studio is restricted to authorized developer accounts for internal diagnostics and telemetry.");
                    } else {
                        infoCell.setText("Stores incoming messages locally to keep conversation continuity even if messages are removed by sender.");
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (isSearching) {
                if (position >= 0 && position < searchResults.size()) {
                    return searchResults.get(position).isCheck ? 1 : 4;
                }
                return 4;
            }
            if (currentType == TYPE_FEATURES_HUB) {
                if (position == hubHeaderStories || position == hubHeaderStealth ||
                    position == hubHeaderMessaging || position == hubHeaderProtection ||
                    position == hubHeaderStudio) {
                    return 0;
                } else if (position == hubRowResetDefaults || position == hubRowStudio) {
                    return 4;
                } else if (position == hubFooterCopyrightRow) {
                    return 3;
                }
                return 1;
            } else if (position == headerRow || position == studioHeaderRow) {
                return 0;
            } else if (position == masterSwitchRow || position == hideTypingRow || position == hideOnlineRow || position == hideReadReceiptsRow || position == hideForwardTagRow) {
                return 1;
            } else if (position == studioAppIdRow || position == studioVersionRow || position == studioLogsRow) {
                return 2;
            } else if (position == hubRowAntiDelete || position == hubRowStorySaver) {
                return 1;
            }
            return 3;
        }
    }
}
