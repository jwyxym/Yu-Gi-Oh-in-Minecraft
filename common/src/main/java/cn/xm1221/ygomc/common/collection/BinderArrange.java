package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardRef;
import cn.xm1221.ygomc.common.data.CardDataDb;
import cn.xm1221.ygomc.common.data.DataPacks;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * 卡册的自动排序。排完之后顺序写回卡册，玩家之后仍可以再拖。
 *
 * <p>0 卡名、1 攻击、2 守备、3 等级、4 卡号。数值从高到低，卡名按字典序。
 */
public final class BinderArrange {

    public static final int BY_NAME = 0;
    public static final int BY_ATTACK = 1;
    public static final int BY_DEFENSE = 2;
    public static final int BY_LEVEL = 3;
    public static final int BY_CODE = 4;

    private BinderArrange() {
    }

    /** 只排这一页，页与页之间的分界不动。 */
    public static BinderContents sortPage(BinderContents contents, int page, int mode) {
        CardDataDb db = DataPacks.get().cardData();
        ArrayList<CardRef> next = new ArrayList<>(contents.page(page));
        next.sort(comparator(db, mode));
        return contents.replace(page, next);
    }

    private static Comparator<CardRef> comparator(CardDataDb db, int mode) {
        Comparator<CardRef> byCode = Comparator.comparingInt(CardRef::cardCode);
        return switch (mode) {
            case BY_ATTACK -> Comparator.comparingInt((CardRef ref) -> attack(db, ref)).reversed().thenComparing(byCode);
            case BY_DEFENSE -> Comparator.comparingInt((CardRef ref) -> defense(db, ref)).reversed().thenComparing(byCode);
            case BY_LEVEL -> Comparator.comparingInt((CardRef ref) -> level(db, ref)).reversed().thenComparing(byCode);
            case BY_CODE -> byCode;
            default -> Comparator.comparing((CardRef ref) -> name(ref), Comparator.nullsLast(String::compareTo))
                    .thenComparing(byCode);
        };
    }

    private static CardDataDb.Stats stats(CardDataDb db, CardRef ref) {
        return db == null ? null : db.stats(ref.cardCode());
    }

    private static int attack(CardDataDb db, CardRef ref) {
        CardDataDb.Stats stats = stats(db, ref);
        return stats == null ? -1 : stats.attack();
    }

    private static int defense(CardDataDb db, CardRef ref) {
        CardDataDb.Stats stats = stats(db, ref);
        if (stats == null || !stats.hasDefense()) {
            return -1;
        }
        return stats.defense();
    }

    private static int level(CardDataDb db, CardRef ref) {
        CardDataDb.Stats stats = stats(db, ref);
        return stats == null ? 0 : stats.level();
    }

    private static String name(CardRef ref) {
        String n = DataPacks.get().nameOf(ref.cardCode());
        return n == null ? "" : n;
    }
}
