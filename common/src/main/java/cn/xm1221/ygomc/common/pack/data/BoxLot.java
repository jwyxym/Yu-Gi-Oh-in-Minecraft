package cn.xm1221.ygomc.common.pack.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 从配好的盒里拆出来的一包属于哪一批、是第几包。
 * 包上不写抽法：抽法由服务端用存档密钥和这个批次重算。
 *
 * @param lot   这一盒的批次
 * @param index 盒内序号，从 0 起
 * @param of    这一盒一共几包
 * @param box   盒装卡包的注册表 id。开封时用它找回配比，包上仍然不写抽法
 */
public record BoxLot(UUID lot, int index, int of, ResourceLocation box) {

    public static final Codec<BoxLot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("lot").forGetter(BoxLot::lot),
            Codec.INT.fieldOf("index").forGetter(BoxLot::index),
            Codec.intRange(1, 1000).fieldOf("of").forGetter(BoxLot::of),
            ResourceLocation.CODEC.fieldOf("box").forGetter(BoxLot::box)
    ).apply(instance, BoxLot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoxLot> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BoxLot::lot,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, BoxLot::index,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, BoxLot::of,
            ResourceLocation.STREAM_CODEC, BoxLot::box,
            BoxLot::new
    );

    public BoxLot {
        if (lot == null || box == null) {
            throw new IllegalArgumentException("批次和盒装 id 不能为空");
        }
        if (index < 0 || index >= of) {
            throw new IllegalArgumentException("包序号 " + index + " 不在 0.." + (of - 1));
        }
    }
}
