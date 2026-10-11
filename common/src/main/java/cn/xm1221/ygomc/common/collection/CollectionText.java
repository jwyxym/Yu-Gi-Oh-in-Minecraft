package cn.xm1221.ygomc.common.collection;

import net.minecraft.network.chat.Component;

/**
 * 组卡 / 卡册界面自己说的话。
 *
 * <p>句子只放语言文件。这个类不引用任何客户端类：提示要在服务端发给玩家，
 * 专用服务器加载得到它。
 */
public final class CollectionText {

    private CollectionText() {
    }

    public static final String DECK_TITLE = "ygomc.collection.deck";
    public static final String ENDER_DECK_TITLE = "ygomc.collection.ender_deck";
    public static final String BINDER_TITLE = "ygomc.collection.binder";
    public static final String CREATIVE_TITLE = "ygomc.collection.creative";
    public static final String ENDER_BINDER_TITLE = "ygomc.collection.ender_binder";

    public static final String MAIN = "ygomc.collection.main";
    public static final String EXTRA = "ygomc.collection.extra";
    public static final String SIDE = "ygomc.collection.side";
    public static final String SUPPLY = "ygomc.collection.supply";
    public static final String OWNED = "ygomc.collection.owned";

    public static final String HINT_DECK = "ygomc.collection.hint.deck";
    public static final String SEARCH = "ygomc.collection.search";
    public static final String SORT = "ygomc.collection.sort";
    public static final String SORT_NAME = "ygomc.collection.sort.name";
    public static final String SORT_ATK = "ygomc.collection.sort.atk";
    public static final String SORT_DEF = "ygomc.collection.sort.def";
    public static final String SORT_LEVEL = "ygomc.collection.sort.level";
    public static final String SORT_CODE = "ygomc.collection.sort.code";
    public static final String SORT_BACK = "ygomc.collection.sort.back";
    public static final String SORT_HINT = "ygomc.collection.sort.hint";
    public static final String PAGE_N = "ygomc.collection.page_n";
    public static final String SEARCH_PROMPT = "ygomc.collection.search_prompt";
    public static final String INTRO_EMPTY = "ygomc.collection.intro_empty";
    public static final String EXPORT = "ygomc.collection.export";
    public static final String IMPORT = "ygomc.collection.import";
    public static final String EXPORT_DONE = "ygomc.collection.export_done";
    public static final String IMPORT_RESULT = "ygomc.collection.import_result";
    public static final String IMPORT_BAD = "ygomc.collection.import_bad";
    public static final String HINT_BINDER = "ygomc.collection.hint.binder";
    public static final String HINT_CREATIVE = "ygomc.collection.hint.creative";
    public static final String EMPTY_SUPPLY = "ygomc.collection.empty_supply";

    public static final String FOLLOWS_PLAYER = "ygomc.collection.follows_player";
    public static final String ALL_CARDS = "ygomc.collection.all_cards";
    public static final String BINDER_COUNT = "ygomc.collection.binder_count";
    public static final String DECK_COUNT = "ygomc.collection.deck_count";
    public static final String ZONE_COUNT = "ygomc.collection.zone_count";
    public static final String PAGE = "ygomc.collection.page";

    public static final String ERR_TOKEN = "ygomc.collection.err.token";
    public static final String ERR_EXTRA_ONLY = "ygomc.collection.err.extra_only";
    public static final String ERR_NOT_EXTRA = "ygomc.collection.err.not_extra";
    public static final String ERR_FULL = "ygomc.collection.err.full";
    public static final String PAGE_FULL = "ygomc.collection.err.page_full";
    public static final String ERR_NO_CARD = "ygomc.collection.err.no_card";
    public static final String ERR_INVENTORY = "ygomc.collection.err.inventory";
    public static final String ERR_UNKNOWN = "ygomc.collection.err.unknown";
    public static final String ERR_MOVED = "ygomc.collection.err.moved";
    public static final String ERR_CREATIVE_PUT = "ygomc.collection.err.creative_put";

    public static Component c(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
