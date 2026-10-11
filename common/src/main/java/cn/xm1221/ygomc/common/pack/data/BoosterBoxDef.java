package cn.xm1221.ygomc.common.pack.data;

import cn.xm1221.ygomc.common.pack.model.BoxSpec;
import cn.xm1221.ygomc.common.pack.model.CollationEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 一种盒装卡包。配比的键是抽法 id，值是这种抽法在一盒里的固定包数。
 */
public record BoosterBoxDef(
        Holder<BoosterDef> booster,
        int packs,
        Mode mode,
        Map<String, Integer> collation,
        List<PrintSpec> extras,
        PackDisplay display
) {

    public enum Mode implements StringRepresentable {
        INDEPENDENT("independent"),
        COLLATED("collated");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final Codec<BoosterBoxDef> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryFileCodec.create(PackRegistries.BOOSTER, BoosterDef.DIRECT_CODEC).fieldOf("booster")
                    .forGetter(BoosterBoxDef::booster),
            Codec.intRange(1, 1000).fieldOf("packs").forGetter(BoosterBoxDef::packs),
            Mode.CODEC.optionalFieldOf("mode", Mode.INDEPENDENT).forGetter(BoosterBoxDef::mode),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 1000)).optionalFieldOf("collation", Map.of())
                    .forGetter(BoosterBoxDef::collation),
            PrintSpec.CODEC.listOf().optionalFieldOf("extras", List.of()).forGetter(BoosterBoxDef::extras),
            PackDisplay.CODEC.fieldOf("display").forGetter(BoosterBoxDef::display)
    ).apply(instance, BoosterBoxDef::new));

    public static final Codec<Holder<BoosterBoxDef>> HOLDER_CODEC =
            RegistryFileCodec.create(PackRegistries.BOOSTER_BOX, DIRECT_CODEC);

    public BoosterBoxDef {
        collation = Map.copyOf(collation);
        extras = List.copyOf(extras);
    }

    /** 展开成开盒用的规格。键按名字排序，计划不随哈希顺序漂移。 */
    public BoxSpec spec() {
        List<CollationEntry> entries = new ArrayList<>();
        if (mode == Mode.COLLATED) {
            collation.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> entries.add(new CollationEntry(entry.getKey(), entry.getValue())));
        }
        List<cn.xm1221.ygomc.common.pack.model.PrintRef> gifts = new ArrayList<>();
        for (PrintSpec extra : extras) {
            gifts.add(extra.toRef());
        }
        return new BoxSpec(packs, entries, gifts);
    }
}
