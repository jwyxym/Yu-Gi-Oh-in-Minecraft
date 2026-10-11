package cn.xm1221.ygomc.common.pack.data;

import cn.xm1221.ygomc.common.Ygomc;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * 卡包三张动态注册表的键。真正的登记在两个平台入口里：
 * Fabric 的 {@code DynamicRegistries} 和 NeoForge 的 {@code DataPackRegistryEvent}，
 * Architectury 没有包这一层。
 *
 * <p>JSON 放在 {@code data/<命名空间>/ygomc/card_set/}（以及 {@code booster}、{@code booster_box}）。
 */
public final class PackRegistries {

    public static final ResourceKey<Registry<CardSetDef>> CARD_SET = key("card_set");
    public static final ResourceKey<Registry<BoosterDef>> BOOSTER = key("booster");
    public static final ResourceKey<Registry<BoosterBoxDef>> BOOSTER_BOX = key("booster_box");

    private PackRegistries() {
    }

    private static <T> ResourceKey<Registry<T>> key(String path) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Ygomc.MOD_ID, path));
    }
}
