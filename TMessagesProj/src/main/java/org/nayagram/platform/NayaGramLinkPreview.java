package org.nayagram.platform;

import org.telegram.tgnet.TLRPC;

import java.util.Locale;

/**
 * Telegram's servers still label t.me link previews as "Telegram"
 * (call links, group/channel invites, users, bots, stickers, gifts).
 * Rewrite only that platform branding so NayaGram shows its own name.
 * User-written names and bios are left untouched.
 */
public final class NayaGramLinkPreview {

    public static final String BRAND = "NayaGram";

    private NayaGramLinkPreview() {
    }

    public static void apply(TLRPC.WebPage page) {
        if (page == null || !isOfficialLink(page)) {
            return;
        }
        if (page.site_name == null || page.site_name.trim().isEmpty() || isBrand(page.site_name)) {
            if (isBrand(page.site_name) || (page.type != null && page.type.startsWith("telegram_"))) {
                page.site_name = BRAND;
            }
        }
        page.title = rewriteIfBoilerplate(page.title);
        page.description = rewriteIfBoilerplate(page.description);
        if (isBrand(page.author)) {
            page.author = BRAND;
        }
    }

    static boolean isOfficialLink(TLRPC.WebPage page) {
        if (page.type != null && (page.type.startsWith("telegram_") || "telegram".equalsIgnoreCase(page.type))) {
            return true;
        }
        return isTelegramUrl(page.url) || isTelegramUrl(page.display_url) || isTelegramUrl(page.embed_url);
    }

    static boolean isTelegramUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        String u = url.toLowerCase(Locale.US);
        return u.startsWith("tg://")
                || u.startsWith("t.me/")
                || u.startsWith("telegram.me/")
                || u.startsWith("telegram.dog/")
                || u.contains("://t.me/")
                || u.contains("://www.t.me/")
                || u.contains("://telegram.me/")
                || u.contains("://www.telegram.me/")
                || u.contains("://telegram.dog/")
                || u.contains("://www.telegram.dog/");
    }

    static boolean isBrand(String value) {
        if (value == null) {
            return false;
        }
        String t = value.trim();
        return t.equalsIgnoreCase("Telegram")
                || t.equals("টেলিগ্রাম")
                || t.equalsIgnoreCase("Telegram Messenger");
    }

    static String rewriteIfBoilerplate(String value) {
        if (value == null || value.isEmpty() || !isBoilerplate(value)) {
            return value;
        }
        String out = value;
        if (out.trim().regionMatches(true, 0, "Telegram:", 0, "Telegram:".length())) {
            int colon = out.indexOf(':');
            out = BRAND + out.substring(colon);
        }
        out = out.replace("Telegram Call", BRAND + " Call");
        out = out.replace("Telegram call", BRAND + " call");
        out = out.replace("TELEGRAM CALL", "NAYAGRAM CALL");
        out = out.replace("on Telegram", "on " + BRAND);
        out = out.replace("On Telegram", "On " + BRAND);
        out = out.replace("in Telegram", "in " + BRAND);
        out = out.replace("In Telegram", "In " + BRAND);
        out = out.replace("via Telegram", "via " + BRAND);
        out = out.replace("Via Telegram", "Via " + BRAND);
        out = out.replace("a Telegram user", "a " + BRAND + " user");
        out = out.replace("A Telegram user", "A " + BRAND + " user");
        out = out.replace("টেলিগ্রাম কল", BRAND + " কল");
        out = out.replace("টেলিগ্রামে", BRAND + "-এ");
        out = out.replace("টেলিগ্রাম", BRAND);
        if (isBrand(out)) {
            return BRAND;
        }
        return out;
    }

    static boolean isBoilerplate(String value) {
        String trimmed = value.trim();
        if (isBrand(trimmed)) {
            return true;
        }
        String lower = trimmed.toLowerCase(Locale.US);
        if (lower.startsWith("telegram:")) {
            return true;
        }
        if (lower.contains("telegram call")) {
            return true;
        }
        if (lower.startsWith("join ") && lower.contains("telegram")) {
            return true;
        }
        if (lower.startsWith("add sticker") && lower.contains("telegram")) {
            return true;
        }
        if (lower.contains("telegram user has created")) {
            return true;
        }
        if (lower.contains("invited") && lower.contains("telegram")) {
            return true;
        }
        if (lower.contains("telegram") && lower.contains("click") && (lower.contains("join") || lower.contains("invit"))) {
            return true;
        }
        if (trimmed.contains("টেলিগ্রাম") && (trimmed.contains("আমন্ত্রণ") || trimmed.contains("যোগ") || trimmed.contains("কল"))) {
            return true;
        }
        return false;
    }
}
