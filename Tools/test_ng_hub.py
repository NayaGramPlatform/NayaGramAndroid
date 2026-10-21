#!/usr/bin/env python3
"""Regression guards for NG Hub's real dialog-backed UI."""
from pathlib import Path

source = Path("TMessagesProj/src/main/java/org/telegram/ui/NGHubActivity.java").read_text()
checks = {
    "four sections including Telegram Communities": all(x in source for x in ("SECTION_CHANNELS", "SECTION_GROUPS", "SECTION_BOTS", "SECTION_COMMUNITIES")),
    "loads archived/all-folder dialogs even when main folder is non-empty": "ArrayList<TLRPC.Dialog> allDialogs = controller.getAllDialogs();" in source and "ArrayList<TLRPC.Dialog> mainDialogs = controller.getDialogs(0);" in source,
    "avoids duplicate dialogs while merging folders": "alreadyPresent" in source and "existing.getClass() == mainDialog.getClass()" in source,
    "community dialog explicitly classified": "dialog instanceof TLRPC.TL_dialogCommunity" in source and "((TLRPC.TL_dialogCommunity) dialog).community_id" in source,
    "community tab distinct from linked chats": '"Communities"' in source and "linked chats remain in their own sections" in source,
    "bot accounts filtered from real dialogs": "user.bot" in source and "DialogObject.isUserDialog(dialog.id)" in source,
    "My Bot uses explicit local bot selection": "isMarkedMyBot(user.id)" in source and "Added to My Bot" in source and "type == SECTION_BOTS" in source,
    "real last-message preview": "controller.dialogMessage.get(dialog.id)" in source and "lastMessage.messageText" in source,
    "real avatars": "BackupImageView" in source and "avatar.setForUserOrChat" in source,
    "search includes preview": "preview.toLowerCase(Locale.ROOT).contains(query)" in source,
    "list and card layouts": "horizontalCards" in source and "Layout: Cards" in source,
    "existing create flows": "new GroupCreateActivity(new Bundle())" in source and "ACTION_TYPE_CHANNEL_CREATE" in source,
    "open chats using actual peer ids": 'args.putLong("chat_id", chat.id)' in source and 'args.putLong("user_id", user.id)' in source,
    "swipe navigation can be disabled": "swipeNavigationEnabled" in source and "showSection(Math.max" in source,
    "owned spaces are creator-verified": "boolean owned = chat != null && chat.creator;" in source and "if (mineOnly && type != SECTION_BOTS && !owned) continue;" in source,
    "all joined includes owned spaces": "if (!mineOnly && owned) continue;" not in source and '"All Joined"' in source,
    "audience labels are unambiguous": '"My Spaces"' in source and '"All Joined"' in source,
    "official botfather": "https://t.me/BotFather" in source and "Open official BotFather" in source,
    "bot selection persisted locally": '"ng_hub_my_bots"' in source and 'putBoolean("bot_" + user.id, marked)' in source,
    "feature date": "16 October 2026" in source,
    "green gradient plus button": "GradientDrawable.Orientation.TL_BR" in source and "0xFF22C55E, 0xFF15803D" in source,
    "plus button animation": ".scaleX(plusOpen ? 1.08f : 1f)" in source and ".scaleY(plusOpen ? 1.08f : 1f)" in source,
    "personal buttons": all(x in source for x in ("My Channel", "My Group", "My Bot", "My Profile", "My Contact List")),
}
failed = [name for name, passed in checks.items() if not passed]
for name, passed in checks.items():
    print(f'{"PASS" if passed else "FAIL"}: {name}')
if failed:
    raise SystemExit("NG Hub regression checks failed: " + ", ".join(failed))
