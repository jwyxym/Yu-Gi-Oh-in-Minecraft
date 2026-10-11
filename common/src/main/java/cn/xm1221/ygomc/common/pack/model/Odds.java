package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;

/**
 * §4.3 的闭式概率。前提是每个被抽到的稀有度池都非空，回退不会发生。
 * 同包去重对单卡概率的影响在卡位张数远小于池大小时忽略。
 */
public final class Odds {

    private Odds() {
    }

    /** 每包期望张数 {@code E[N_r]}。 */
    public static double expectedCount(Booster booster, CardRarity rarity) {
        double total = booster.totalWeight();
        double expected = 0;
        for (Pull pull : booster.pulls()) {
            double cards = 0;
            for (Slot slot : pull.slots()) {
                cards += slot.count() * slot.pick().chance(rarity);
            }
            expected += pull.weight() / total * cards;
        }
        return expected;
    }

    /** 一包里至少一张 {@code rarity} 的概率。 */
    public static double atLeastOne(Booster booster, CardRarity rarity) {
        double total = booster.totalWeight();
        double probability = 0;
        for (Pull pull : booster.pulls()) {
            double none = 1;
            for (Slot slot : pull.slots()) {
                none *= Math.pow(1 - slot.pick().chance(rarity), slot.count());
            }
            probability += pull.weight() / total * (1 - none);
        }
        return probability;
    }

    /**
     * 某一印制在一包里出现的概率，按放回近似。
     * 印制不在池里时返回 0。
     */
    public static double printChance(Booster booster, CardSetData set, PrintRef print) {
        double share = share(set, print);
        if (share == 0) {
            return 0;
        }
        CardRarity rarity = print.rarity();
        double total = booster.totalWeight();
        double probability = 0;
        for (Pull pull : booster.pulls()) {
            double none = 1;
            for (Slot slot : pull.slots()) {
                none *= Math.pow(1 - slot.pick().chance(rarity) * share, slot.count());
            }
            probability += pull.weight() / total * (1 - none);
        }
        return probability;
    }

    /** {@code n} 包独立开、至少一张的概率。 */
    public static double atLeastOneIn(Booster booster, CardRarity rarity, int packs) {
        double once = atLeastOne(booster, rarity);
        return 1 - Math.pow(1 - once, packs);
    }

    /**
     * 保底开启时期望包数：连续 {@code k - 1} 包没有，第 {@code k} 包强制。
     * {@code p} 是单包至少一张的概率。
     */
    public static double pityExpectedPacks(double probability, int cap) {
        if (probability <= 0 || probability > 1 || cap <= 0) {
            throw new IllegalArgumentException("保底参数不合法：p=" + probability + " k=" + cap);
        }
        return (1 - Math.pow(1 - probability, cap)) / probability;
    }

    private static double share(CardSetData set, PrintRef print) {
        int total = 0;
        int own = 0;
        for (PrintRef other : set.pool(print.rarity())) {
            total += other.weight();
            if (other.code() == print.code() && other.variant() == print.variant()) {
                own += other.weight();
            }
        }
        return total == 0 ? 0 : own / (double) total;
    }
}
