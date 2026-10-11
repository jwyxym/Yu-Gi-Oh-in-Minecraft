package cn.xm1221.ygomc.common.pack.model;

import cn.xm1221.ygomc.common.card.CardRarity;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一种卡包商品：若干抽法、同包去重策略、空池时的回退顺序。
 */
public record Booster(List<Pull> pulls, Duplicates duplicates, List<CardRarity> fallback) {

    public enum Duplicates implements StringRepresentable {
        /** 同一包里同一张印制尽量不重复。池被抽空才放回。 */
        AVOID_IN_PACK("avoid_in_pack"),
        /** 每一抽独立放回。 */
        ALLOW("allow");

        public static final Codec<Duplicates> CODEC = StringRepresentable.fromEnum(Duplicates::values);

        private final String name;

        Duplicates(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public Booster {
        if (pulls == null || pulls.isEmpty()) {
            throw new IllegalArgumentException("卡包至少要有一种抽法");
        }
        pulls = List.copyOf(pulls);
        if (duplicates == null) {
            throw new IllegalArgumentException("去重策略不能为空");
        }
        if (fallback == null || fallback.isEmpty()) {
            throw new IllegalArgumentException("回退顺序不能为空");
        }
        fallback = List.copyOf(fallback);
    }

    public int totalWeight() {
        int sum = 0;
        for (Pull pull : pulls) {
            sum += pull.weight();
        }
        return sum;
    }

    public Pull byId(String id) {
        for (Pull pull : pulls) {
            if (pull.id().equals(id)) {
                return pull;
            }
        }
        return null;
    }

    public Pull roll(RollSource random) {
        int pick = random.nextInt(totalWeight());
        for (Pull pull : pulls) {
            pick -= pull.weight();
            if (pick < 0) {
                return pull;
            }
        }
        throw new IllegalStateException("抽法前缀和没有覆盖总权重");
    }

    /**
     * 保底用：在「卡位分布里含有这一档」的抽法中按权重再抽一次。
     *
     * @return 没有这种抽法时返回 {@code null}
     */
    public Pull rollContaining(CardRarity rarity, RollSource random) {
        int total = 0;
        for (Pull pull : pulls) {
            if (contains(pull, rarity)) {
                total += pull.weight();
            }
        }
        if (total == 0) {
            return null;
        }
        int pick = random.nextInt(total);
        for (Pull pull : pulls) {
            if (!contains(pull, rarity)) {
                continue;
            }
            pick -= pull.weight();
            if (pick < 0) {
                return pull;
            }
        }
        throw new IllegalStateException("含 " + rarity + " 的抽法前缀和没有覆盖");
    }

    private static boolean contains(Pull pull, CardRarity rarity) {
        for (Slot slot : pull.slots()) {
            if (slot.pick().chance(rarity) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 加载时校验。返回的句子以 {@code ERROR} 或 {@code WARN} 开头，空列表表示通过。
     * 未知卡号不在这里查，那要等卡片数据库。
     */
    public List<String> validate(CardSetData set) {
        List<String> issues = new ArrayList<>();
        for (Pull pull : pulls) {
            for (Slot slot : pull.slots()) {
                boolean any = false;
                for (CardRarity rarity : slot.pick().weights().keySet()) {
                    if (reachable(set, rarity)) {
                        any = true;
                        break;
                    }
                }
                if (!any) {
                    issues.add("ERROR 抽法 " + pull.id() + " 的卡位抽到的稀有度池全空，回退也救不回");
                }
                if (duplicates == Duplicates.AVOID_IN_PACK) {
                    for (CardRarity rarity : slot.pick().weights().keySet()) {
                        int size = set.pool(rarity).size();
                        if (slot.pick().weights().size() == 1 && size > 0 && slot.count() > size) {
                            issues.add("WARN 抽法 " + pull.id() + " 的 " + rarity.getSerializedName()
                                    + " 卡位要 " + slot.count() + " 张，池里只有 " + size + " 张，必然重复");
                        }
                    }
                }
            }
        }
        for (CardRarity rarity : CardRarity.values()) {
            if (set.isEmpty(rarity)) {
                continue;
            }
            boolean reachable = false;
            for (Pull pull : pulls) {
                if (contains(pull, rarity)) {
                    reachable = true;
                    break;
                }
            }
            if (!reachable) {
                issues.add("WARN 系列里有 " + rarity.getSerializedName() + "，但没有任何卡位抽得到");
            }
        }
        return issues;
    }

    private boolean reachable(CardSetData set, CardRarity rarity) {
        if (!set.isEmpty(rarity)) {
            return true;
        }
        for (CardRarity next : fallback) {
            if (!set.isEmpty(next)) {
                return true;
            }
        }
        return false;
    }
}
