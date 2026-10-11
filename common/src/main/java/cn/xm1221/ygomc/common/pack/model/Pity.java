package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;

import java.util.List;

/**
 * 按「连续没出」计数的保底。默认关闭，计数放在调用方，不放在物品上。
 * 第 {@code cap} 包若还没出过目标档，强制走含有该档的抽法。
 */
public final class Pity {

    private final CardRarity rarity;
    private final int cap;
    private int dry;

    public Pity(CardRarity rarity, int cap) {
        if (rarity == null || cap <= 0) {
            throw new IllegalArgumentException("保底档位和上限必须有效");
        }
        this.rarity = rarity;
        this.cap = cap;
    }

    public List<PrintRef> open(Booster booster, CardSetData set, RollSource random) {
        Pull pull;
        if (dry + 1 >= cap) {
            pull = booster.rollContaining(rarity, random);
            if (pull == null) {
                throw new PackRollException("没有抽法含有 " + rarity.getSerializedName() + "，保底无法生效");
            }
        } else {
            pull = booster.roll(random);
        }
        List<PrintRef> cards = PackRoller.open(booster, set, pull, random);
        boolean hit = false;
        for (PrintRef card : cards) {
            if (card.rarity() == rarity) {
                hit = true;
                break;
            }
        }
        dry = hit ? 0 : dry + 1;
        return cards;
    }

    /** 从零开始，开到第一次出目标档为止，返回用了几包。 */
    public static int packsUntilHit(Booster booster, CardSetData set, CardRarity rarity, int cap, RollSource random) {
        Pity pity = new Pity(rarity, cap);
        for (int packs = 1; packs <= cap; packs++) {
            List<PrintRef> cards = pity.open(booster, set, random);
            for (PrintRef card : cards) {
                if (card.rarity() == rarity) {
                    return packs;
                }
            }
        }
        throw new IllegalStateException("保底上限 " + cap + " 包后仍未出 " + rarity.getSerializedName());
    }
}
