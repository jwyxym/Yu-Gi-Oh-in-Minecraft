package cn.xm1221.ygomc.common.pack.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * 一种抽法：被抽中之后，按卡位顺序出卡。
 *
 * @param id     配比表引用的名字，例如 {@code secret_hit}
 * @param weight 抽法之间的权重
 * @param slots  卡位，从先到后
 */
public record Pull(String id, int weight, List<Slot> slots) {

    public static final Codec<Pull> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Pull::id),
            Codec.intRange(1, 1_000_000).fieldOf("weight").forGetter(Pull::weight),
            Slot.CODEC.listOf().fieldOf("slots").forGetter(Pull::slots)
    ).apply(instance, Pull::new));

    public Pull {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("抽法 id 不能为空");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("抽法 " + id + " 的权重必须为正，实得 " + weight);
        }
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("抽法 " + id + " 至少要有一个卡位");
        }
        slots = List.copyOf(slots);
    }

    public int cards() {
        int count = 0;
        for (Slot slot : slots) {
            count += slot.count();
        }
        return count;
    }
}
