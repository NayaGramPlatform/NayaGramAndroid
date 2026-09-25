package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * NGSettingsActivity - Dedicated NayaGram Feature Store & Settings Hub
 * Inspired by Nekogram, Cherrygram, and Novagram.
 */
public class NGSettingsActivity extends BaseFragment {

    public static final int TYPE_FEATURES_HUB = 0;
    public static final int TYPE_GHOST_MODE = 1;
    public static final int TYPE_STUDIO = 2;
    public static final int TYPE_ANTI_DELETE = 3;

    private final int currentType;
    private ListAdapter listAdapter;
    private RecyclerListView listView;

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
        updateRows();
        return true;
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

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        if (currentType == TYPE_FEATURES_HUB) {
            actionBar.setTitle("NayaGram Settings");
        } else if (currentType == TYPE_GHOST_MODE) {
            actionBar.setTitle("Ghost Mode");
        } else if (currentType == TYPE_STUDIO) {
            actionBar.setTitle("NG Control");
        } else {
            actionBar.setTitle("Anti-Delete");
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            GhostModeManager gm = GhostModeManager.getInstance();
            NayaConfig cfg = NayaConfig.getInstance();

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
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        // Line 1: © {YEAR} 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦. All rights reserved.
        int start = ssb.length();
        ssb.append("© ").append(String.valueOf(currentYear)).append(" ");
        ssb.setSpan(new ForegroundColorSpan(0xFF34C759), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = ssb.length();
        ssb.append("𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 ");
        ssb.setSpan(new ForegroundColorSpan(0xFF9B51E0), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = ssb.length();
        ssb.append("𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦");
        ssb.setSpan(new ForegroundColorSpan(0xFF2F80ED), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = ssb.length();
        ssb.append(". All rights reserved.\n");
        ssb.setSpan(new ForegroundColorSpan(0xFF34C759), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Line 2: Built with ❤️ in Bangladesh 🇧🇩
        start = ssb.length();
        ssb.append("Built with ❤️ in Bangladesh 🇧🇩");
        ssb.setSpan(new ForegroundColorSpan(0xFF2F80ED), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Centered alignment
        ssb.setSpan(new android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            if (currentType == TYPE_FEATURES_HUB) {
                if (pos == hubRowAnonymousStories || pos == hubRowStorySaver || pos == hubRowGhostMaster ||
                    pos == hubRowForwardNoQuote || pos == hubRowAntiDelete || pos == hubRowConfirmActions ||
                    pos == hubRowShowIdDc || pos == hubRowStudio) {
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

            switch (holder.getItemViewType()) {
                case 0:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (currentType == TYPE_FEATURES_HUB) {
                        if (position == hubHeaderStories) {
                            headerCell.setText("Story Settings (Nekogram / Novagram)");
                        } else if (position == hubHeaderStealth) {
                            headerCell.setText("Privacy & Stealth (Ghost Mode)");
                        } else if (position == hubHeaderMessaging) {
                            headerCell.setText("Messaging & Chats");
                        } else if (position == hubHeaderProtection) {
                            headerCell.setText("Protection & Details");
                        } else if (position == hubHeaderStudio) {
                            headerCell.setText("NG Studio Management");
                        }
                    } else if (position == headerRow) {
                        headerCell.setText(currentType == TYPE_GHOST_MODE ? "Privacy & Stealth" : "Anti-Delete Settings");
                    } else if (position == studioHeaderRow) {
                        headerCell.setText("NG Studio Diagnostics");
                    }
                    break;
                case 1:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (currentType == TYPE_FEATURES_HUB) {
                        if (position == hubRowAnonymousStories) {
                            checkCell.setTextAndCheck("View Stories Anonymously", cfg.isAnonymousStories(), true);
                        } else if (position == hubRowStorySaver) {
                            checkCell.setTextAndCheck("Download & Save Stories", cfg.isStorySaverEnabled(), false);
                        } else if (position == hubRowGhostMaster) {
                            checkCell.setTextAndCheck("Enable Ghost Mode", gm.isGhostModeEnabled(), true);
                        } else if (position == hubRowHideTyping) {
                            checkCell.setTextAndCheck("Hide Typing Indicator", gm.isHideTypingStatus(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hubRowHideOnline) {
                            checkCell.setTextAndCheck("Hide Online Status", gm.isHideOnlineStatus(), true);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hubRowHideReadReceipts) {
                            checkCell.setTextAndCheck("Hide Read Receipts", gm.isHideReadReceipts(), false);
                            checkCell.setEnabled(gm.isGhostModeEnabled(), null);
                        } else if (position == hubRowForwardNoQuote) {
                            checkCell.setTextAndCheck("Forward Without Quote (Direct Share)", cfg.isForwardWithoutQuote(), true);
                        } else if (position == hubRowAntiDelete) {
                            checkCell.setTextAndCheck("Anti-Delete (Save deleted msgs)", AntiDeleteManager.getInstance().isAntiDeleteEnabled(), false);
                        } else if (position == hubRowConfirmActions) {
                            checkCell.setTextAndCheck("Confirm Calls & Voice Notes", cfg.isConfirmActions(), true);
                        } else if (position == hubRowShowIdDc) {
                            checkCell.setTextAndCheck("Show User ID & DC in Profile", cfg.isShowIdAndDc(), false);
                        }
                    } else if (currentType == TYPE_GHOST_MODE) {
                        if (position == masterSwitchRow) {
                            checkCell.setTextAndCheck("Enable Ghost Mode", gm.isGhostModeEnabled(), true);
                        } else if (position == hideTypingRow) {
                            checkCell.setTextAndCheck("Hide Typing Indicator", gm.isHideTypingStatus(), true);
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
                    if (position == studioAppIdRow) {
                        detailCell.setTextAndValue("Package", "org.nayagram.platform", true);
                    } else if (position == studioVersionRow) {
                        detailCell.setTextAndValue("Version", "1.0.0 (1)", true);
                    } else if (position == studioLogsRow) {
                        detailCell.setTextAndValue("Security Mode", "Production Hardened", false);
                    }
                    break;
                case 4:
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == hubRowStudio) {
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
            if (currentType == TYPE_FEATURES_HUB) {
                if (position == hubHeaderStories || position == hubHeaderStealth ||
                    position == hubHeaderMessaging || position == hubHeaderProtection ||
                    position == hubHeaderStudio) {
                    return 0;
                } else if (position == hubRowStudio) {
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
            }
            return 3;
        }
    }
}
