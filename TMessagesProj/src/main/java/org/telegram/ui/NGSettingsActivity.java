package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nayagram.platform.GhostModeManager;
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
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * NGSettingsActivity - Native NayaGram Settings Fragment
 * Provides clean Telegram-style UI for Ghost Mode, NG Control, and Anti-Delete.
 */
public class NGSettingsActivity extends BaseFragment {

    public static final int TYPE_GHOST_MODE = 1;
    public static final int TYPE_STUDIO = 2;
    public static final int TYPE_ANTI_DELETE = 3;

    private final int currentType;
    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int rowCount;
    private int headerRow;
    private int masterSwitchRow;
    private int hideTypingRow;
    private int hideOnlineRow;
    private int hideReadReceiptsRow;
    private int hideForwardTagRow;
    private int infoRow;

    // NG Studio specific rows
    private int studioHeaderRow;
    private int studioAppIdRow;
    private int studioVersionRow;
    private int studioLogsRow;

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

        if (currentType == TYPE_GHOST_MODE) {
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

        if (currentType == TYPE_GHOST_MODE) {
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
            if (position == masterSwitchRow) {
                if (currentType == TYPE_GHOST_MODE) {
                    gm.setGhostModeEnabled(!gm.isGhostModeEnabled());
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(gm.isGhostModeEnabled());
                    }
                    if (listAdapter != null) listAdapter.notifyDataSetChanged();
                }
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
        });

        return fragmentView;
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
            if (currentType == TYPE_GHOST_MODE) {
                if (pos == masterSwitchRow) return true;
                GhostModeManager gm = GhostModeManager.getInstance();
                return gm.isGhostModeEnabled() && (pos == hideTypingRow || pos == hideOnlineRow || pos == hideReadReceiptsRow || pos == hideForwardTagRow);
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
            switch (holder.getItemViewType()) {
                case 0:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == headerRow) {
                        headerCell.setText(currentType == TYPE_GHOST_MODE ? "Privacy & Stealth" : "Anti-Delete Settings");
                    } else if (position == studioHeaderRow) {
                        headerCell.setText("NG Studio Diagnostics");
                    }
                    break;
                case 1:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
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
                case 3:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (currentType == TYPE_GHOST_MODE) {
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
            if (position == headerRow || position == studioHeaderRow) {
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
