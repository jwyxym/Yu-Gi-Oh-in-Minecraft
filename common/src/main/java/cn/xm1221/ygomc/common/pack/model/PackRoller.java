package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 开一包。随机数全部来自参数，不读世界。
 */
public final class PackRoller {

    private PackRoller() {
    }

    /**
     * 按卡包自己的权重抽一种抽法，再开包。
     *
     * @throws PackRollException 回退之后仍然没有可抽的池
     */
    public static List<PrintRef> open(Booster booster, CardSetData set, RollSource random) {
        return open(booster, set, booster.roll(random), random);
    }

    /**
     * 用指定抽法开一包。配好的盒在开封时走这一条。
     */
    public static List<PrintRef> open(Booster booster, CardSetData set, Pull pull, RollSource random) {
        Set<String> seen = new HashSet<>();
        List<PrintRef> cards = new ArrayList<>();
        for (Slot slot : pull.slots()) {
            for (int i = 0; i < slot.count(); i++) {
                CardRarity rarity = resolve(booster, set, slot.pick().roll(random));
                cards.add(draw(booster, set, rarity, seen, random));
            }
        }
        return List.copyOf(cards);
    }

    private static CardRarity resolve(Booster booster, CardSetData set, CardRarity rolled) {
        if (!set.isEmpty(rolled)) {
            return rolled;
        }
        for (CardRarity rarity : booster.fallback()) {
            if (!set.isEmpty(rarity)) {
                return rarity;
            }
        }
        throw new PackRollException("稀有度 " + rolled.getSerializedName() + " 的池是空的，回退顺序里也没有可抽的档");
    }

    private static PrintRef draw(Booster booster, CardSetData set, CardRarity rarity,
                                 Set<String> seen, RollSource random) {
        List<PrintRef> pool = set.pool(rarity);
        if (booster.duplicates() == Booster.Duplicates.AVOID_IN_PACK) {
            List<PrintRef> fresh = new ArrayList<>();
            for (PrintRef print : pool) {
                if (!seen.contains(key(print))) {
                    fresh.add(print);
                }
            }
            if (!fresh.isEmpty()) {
                PrintRef drawn = weighted(fresh, random);
                seen.add(key(drawn));
                return drawn;
            }
        }
        return weighted(pool, random);
    }

    private static PrintRef weighted(List<PrintRef> pool, RollSource random) {
        int total = 0;
        for (PrintRef print : pool) {
            total += print.weight();
        }
        int pick = random.nextInt(total);
        for (PrintRef print : pool) {
            pick -= print.weight();
            if (pick < 0) {
                return print;
            }
        }
        throw new IllegalStateException("印制前缀和没有覆盖总权重");
    }

    private static String key(PrintRef print) {
        return print.code() + ":" + print.rarity().getSerializedName() + ":" + print.variant();
    }
}
