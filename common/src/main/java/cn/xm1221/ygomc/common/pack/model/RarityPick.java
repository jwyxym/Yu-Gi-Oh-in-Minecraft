package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一个卡位先抽稀有度的分布。单一档位是权重为 1 的特例。
 */
public record RarityPick(Map<CardRarity, Integer> weights) {

    /** {@code "rare"} 或 {@code {"super": 3, "ultra": 1}}。 */
    public static final Codec<RarityPick> CODEC = Codec.either(
            CardRarity.CODEC,
            Codec.unboundedMap(CardRarity.CODEC, Codec.intRange(1, 1_000_000))
    ).xmap(
            either -> either.map(RarityPick::one, RarityPick::new),
            pick -> pick.weights().size() == 1 && pick.total() == 1
                    ? Either.left(pick.weights().keySet().iterator().next())
                    : Either.right(pick.weights()));

    public RarityPick {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("稀有度分布不能为空");
        }
        Map<CardRarity, Integer> copy = new LinkedHashMap<>();
        for (Map.Entry<CardRarity, Integer> entry : weights.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("稀有度权重必须为正：" + entry);
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        weights = Collections.unmodifiableMap(copy);
    }

    public static RarityPick one(CardRarity rarity) {
        return new RarityPick(Map.of(rarity, 1));
    }

    public int total() {
        int sum = 0;
        for (int weight : weights.values()) {
            sum += weight;
        }
        return sum;
    }

    /** @return 抽到 {@code rarity} 的概率，没有这一档时为 0 */
    public double chance(CardRarity rarity) {
        Integer weight = weights.get(rarity);
        return weight == null ? 0 : weight / (double) total();
    }

    public CardRarity roll(RollSource random) {
        int pick = random.nextInt(total());
        for (Map.Entry<CardRarity, Integer> entry : weights.entrySet()) {
            pick -= entry.getValue();
            if (pick < 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("稀有度前缀和没有覆盖总权重");
    }
}
