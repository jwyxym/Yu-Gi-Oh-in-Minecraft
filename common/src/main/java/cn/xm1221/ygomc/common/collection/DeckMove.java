package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardRef;
import cn.xm1221.ygomc.common.card.DeckData;
import cn.xm1221.ygomc.common.data.CardDataDb;

import java.util.ArrayList;
import java.util.List;

/**
 * 一张卡能不能进某个区，以及进出之后的新卡组。
 *
 * <p>不碰物品栏。规则错了不会报异常，只会把融合塞进主卡组，所以判断留在这里，
 * 让离线断言能钉住。额外区只收融合 / 同步 / 超量 / 连接；衍生物哪都不收；
 * 副卡组两边都能放。上限是 ygopro 的 60 / 15 / 15，编辑时不要求满 40 张。
 */
public final class DeckMove {

    /** 只能进额外卡组（或副卡组）的怪兽类型。仪式怪兽不在里面，它进主卡组。 */
    public static final int EXTRA_ONLY = CardDataDb.CardTypes.TYPE_FUSION
            | CardDataDb.CardTypes.TYPE_SYNCHRO
            | CardDataDb.CardTypes.TYPE_XYZ
            | CardDataDb.CardTypes.TYPE_LINK;

    public enum Zone {
        MAIN, EXTRA, SIDE
    }

    private DeckMove() {
    }

    /**
     * @param type 卡的 {@code type} 位
     * @param size 该区现在的张数
     * @return 不能放时的语言键；能放返回 {@code null}
     */
    public static String reject(Zone zone, int type, int size) {
        if ((type & CardDataDb.CardTypes.TYPE_TOKEN) != 0) {
            return CollectionText.ERR_TOKEN;
        }
        boolean extra = (type & EXTRA_ONLY) != 0;
        if (zone == Zone.MAIN && extra) {
            return CollectionText.ERR_EXTRA_ONLY;
        }
        if (zone == Zone.EXTRA && !extra) {
            return CollectionText.ERR_NOT_EXTRA;
        }
        int cap = switch (zone) {
            case MAIN -> DeckData.MAIN_MAX;
            case EXTRA -> DeckData.EXTRA_MAX;
            case SIDE -> DeckData.SIDE_MAX;
        };
        if (size >= cap) {
            return CollectionText.ERR_FULL;
        }
        return null;
    }

    public static List<CardRef> zone(DeckCards deck, Zone which) {
        return switch (which) {
            case MAIN -> deck.main();
            case EXTRA -> deck.extra();
            case SIDE -> deck.side();
        };
    }

    public static DeckCards add(DeckCards deck, Zone which, CardRef ref) {
        return new DeckCards(
                which == Zone.MAIN ? plus(deck.main(), ref) : deck.main(),
                which == Zone.EXTRA ? plus(deck.extra(), ref) : deck.extra(),
                which == Zone.SIDE ? plus(deck.side(), ref) : deck.side());
    }

    /** @return 下标越界时 {@code null} */
    public static DeckCards remove(DeckCards deck, Zone which, int index) {
        List<CardRef> next = without(zone(deck, which), index);
        if (next == null) {
            return null;
        }
        return new DeckCards(
                which == Zone.MAIN ? next : deck.main(),
                which == Zone.EXTRA ? next : deck.extra(),
                which == Zone.SIDE ? next : deck.side());
    }

    private static List<CardRef> plus(List<CardRef> list, CardRef ref) {
        ArrayList<CardRef> next = new ArrayList<>(list.size() + 1);
        next.addAll(list);
        next.add(ref);
        return next;
    }

    private static List<CardRef> without(List<CardRef> list, int index) {
        if (index < 0 || index >= list.size()) {
            return null;
        }
        ArrayList<CardRef> next = new ArrayList<>(list);
        next.remove(index);
        return next;
    }
}
