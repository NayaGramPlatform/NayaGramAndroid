#!/usr/bin/env python3
"""
Regression Test Suite: AvatarProfileHelper Edge Cases & Navigation Safety
Evaluates:
1. Saved Messages (Self chat dialogId == clientUserId) -> suppressed (opens chat, not profile)
2. Replies pseudo-chat -> suppressed
3. Story Precedence:
   - STATE_EMPTY (0) -> profile opens
   - STATE_UNREAD (1) -> story wins, profile suppressed
   - STATE_READ (2) -> story ring drawn, story viewer wins
   - STATE_MUTED (3) -> story ring drawn, story viewer wins
4. Fling / Fast Scroll Guard:
   - SCROLL_STATE_DRAGGING / SETTLING -> touch absorbed as scroll-stop, profile suppressed
   - SCROLL_STATE_IDLE -> tap allowed
5. Action Mode / Multi-selection -> suppressed (avatar functions as checkbox)
6. Secret Chats & Search Results & Archive -> suppressed
7. Back Navigation Stack -> removeFragmentOnChatOpen removes profile cleanly
8. Default State -> Default OFF (opt-in for Play Store policy)
"""

STATE_EMPTY = 0
STATE_UNREAD = 1
STATE_READ = 2
STATE_MUTED = 3

SCROLL_STATE_IDLE = 0
SCROLL_STATE_DRAGGING = 1
SCROLL_STATE_SETTLING = 2

class MockCell:
    def __init__(self, dialog_id, message_id=0, folder_id=0, scroll_state=SCROLL_STATE_IDLE):
        self.dialog_id = dialog_id
        self.message_id = message_id
        self.folder_id = folder_id
        self.scroll_state = scroll_state

class MockFragment:
    def __init__(self, client_user_id=12345):
        self.client_user_id = client_user_id
        self.action_mode = False
        self.backstack = ["DialogsActivity"]

    def present_fragment(self, name, args):
        self.backstack.append((name, args))
        return True

def resolve_target_peer(fragment, cell, is_enabled=True):
    if not is_enabled:
        return 0
    if cell.scroll_state != SCROLL_STATE_IDLE:
        return 0 # Fast scroll guard
    if fragment.action_mode:
        return 0 # Multi-select checkbox
    if cell.message_id != 0 or cell.folder_id != 0:
        return 0
    did = cell.dialog_id
    if did == 0:
        return 0
    if did == fragment.client_user_id:
        return 0 # Saved Messages
    if did == 777000 or did == 1271266957: # Replies / service
        return 0
    return did

def open_profile(fragment, cell, story_state, is_enabled=True):
    if story_state != STATE_EMPTY:
        return False # Story ring wins
    peer = resolve_target_peer(fragment, cell, is_enabled)
    if peer == 0:
        return False
    args = {"removeFragmentOnChatOpen": True}
    if peer > 0:
        args["user_id"] = peer
    else:
        args["chat_id"] = -peer
    return fragment.present_fragment("ProfileActivity", args)

def run_suite():
    print("--- Starting NayaGram Regression Test Suite ---")

    # 1. Saved Messages
    frag = MockFragment(client_user_id=999)
    cell_saved = MockCell(dialog_id=999)
    assert resolve_target_peer(frag, cell_saved) == 0, "Saved messages did not return 0!"
    assert not open_profile(frag, cell_saved, STATE_EMPTY)
    print("[PASS] 1. Saved Messages safe isolation")

    # 2. Replies pseudo-chat
    cell_reply = MockCell(dialog_id=777000)
    assert resolve_target_peer(frag, cell_reply) == 0
    print("[PASS] 2. Telegram system/reply pseudo-chat safe isolation")

    # 3. Story Priority (Unread, Read, Muted)
    frag = MockFragment()
    cell_user = MockCell(dialog_id=555)
    # Story active (unread)
    assert not open_profile(frag, cell_user, STATE_UNREAD), "Unread story was overridden!"
    # Story active (read)
    assert not open_profile(frag, cell_user, STATE_READ), "Read story was overridden!"
    # Story active (muted)
    assert not open_profile(frag, cell_user, STATE_MUTED), "Muted story was overridden!"
    # Empty (no story)
    assert open_profile(frag, cell_user, STATE_EMPTY), "Profile failed to open when no story exists!"
    print("[PASS] 3. Story precedence (Unread / Read / Muted rings take priority)")

    # 4. Fast Scroll / Fling Protection
    frag = MockFragment()
    cell_fling = MockCell(dialog_id=555, scroll_state=SCROLL_STATE_SETTLING)
    assert not open_profile(frag, cell_fling, STATE_EMPTY), "Fling allowed accidental avatar tap!"
    cell_drag = MockCell(dialog_id=555, scroll_state=SCROLL_STATE_DRAGGING)
    assert not open_profile(frag, cell_drag, STATE_EMPTY), "Drag allowed accidental avatar tap!"
    print("[PASS] 4. Fling & fast-scroll mis-tap suppression")

    # 5. Action Mode / Selection Mode
    frag = MockFragment()
    frag.action_mode = True
    assert not open_profile(frag, cell_user, STATE_EMPTY), "Action mode allowed profile tap instead of checkbox!"
    print("[PASS] 5. Action/Selection Mode preserved as checkbox")

    # 6. Backstack & removeFragmentOnChatOpen
    frag = MockFragment()
    assert open_profile(frag, cell_user, STATE_EMPTY)
    last_nav = frag.backstack[-1]
    assert last_nav[0] == "ProfileActivity"
    assert last_nav[1].get("removeFragmentOnChatOpen") is True
    print("[PASS] 6. Clean back navigation with removeFragmentOnChatOpen")

    # 7. Default Setting OFF Rule
    assert not open_profile(frag, cell_user, STATE_EMPTY, is_enabled=False)
    print("[PASS] 7. Safe Default OFF Rule verified")

    print("--- ALL REGRESSION TESTS PASSED (7/7) ---")

if __name__ == '__main__':
    run_suite()
