package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一个系列的印制池，按稀有度分桶。同一张卡可以同时出现在多个桶里。
 */
public final class CardSetData {

    private final Map<CardRarity, List<PrintRef>> byRarity = new EnumMap<>(CardRarity.class);

    public CardSetData(List<PrintRef> prints) {
        if (prints == null || prints.isEmpty()) {
            throw new IllegalArgumentException("系列至少要有一张印制");
        }
        for (PrintRef print : prints) {
            byRarity.computeIfAbsent(print.rarity(), key -> new ArrayList<>()).add(print);
        }
    }

    public List<PrintRef> pool(CardRarity rarity) {
        return byRarity.getOrDefault(rarity, List.of());
    }

    public boolean isEmpty(CardRarity rarity) {
        return pool(rarity).isEmpty();
    }
}
