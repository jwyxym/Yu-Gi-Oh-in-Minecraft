package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardRef;
import cn.xm1221.ygomc.common.data.CardDataDb;
import cn.xm1221.ygomc.common.data.DataPacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 卡组里的顺序由规则排，不由玩家拖到第几张决定。
 *
 * <p>同一区内：融合 / 同步 / 超量 / 连接 / 主卡组怪兽，然后魔法、陷阱。
 * 同组里星级高的在前，再比攻击力，最后比卡号。同名卡靠稳定排序挨在一起。
 */
public final class DeckArrange {

    private DeckArrange() {
    }

    public static DeckCards arrange(DeckCards deck) {
        CardDataDb db = DataPacks.get().cardData();
        return new DeckCards(sort(deck.main(), db), sort(deck.extra(), db), sort(deck.side(), db));
    }

    private static List<CardRef> sort(List<CardRef> cards, CardDataDb db) {
        ArrayList<CardRef> next = new ArrayList<>(cards);
        next.sort(Comparator.comparingInt((CardRef ref) -> group(stats(db, ref.cardCode())))
                .thenComparing(Comparator.comparingInt((CardRef ref) -> level(stats(db, ref.cardCode()))).reversed())
                .thenComparing(Comparator.comparingInt((CardRef ref) -> attack(stats(db, ref.cardCode()))).reversed())
                .thenComparingInt(CardRef::cardCode));
        return next;
    }

    private static CardDataDb.Stats stats(CardDataDb db, int code) {
        return db == null ? null : db.stats(code);
    }

    private static int group(CardDataDb.Stats stats) {
        if (stats == null) {
            return 9;
        }
        int type = stats.type();
        if ((type & CardDataDb.CardTypes.TYPE_MONSTER) != 0) {
            if ((type & CardDataDb.CardTypes.TYPE_FUSION) != 0) {
                return 0;
            }
            if ((type & CardDataDb.CardTypes.TYPE_SYNCHRO) != 0) {
                return 1;
            }
            if ((type & CardDataDb.CardTypes.TYPE_XYZ) != 0) {
                return 2;
            }
            if ((type & CardDataDb.CardTypes.TYPE_LINK) != 0) {
                return 3;
            }
            return 4;
        }
        if ((type & CardDataDb.CardTypes.TYPE_SPELL) != 0) {
            return 5;
        }
        return 6;
    }

    private static int level(CardDataDb.Stats stats) {
        return stats == null ? 0 : stats.level();
    }

    private static int attack(CardDataDb.Stats stats) {
        return stats == null ? -1 : stats.attack();
    }
}
