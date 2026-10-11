package cn.xm1221.ygomc.common.pack;

import cn.xm1221.ygomc.common.card.CardComponents;
import cn.xm1221.ygomc.common.client.PackTips;
import cn.xm1221.ygomc.common.pack.data.BoosterDef;
import cn.xm1221.ygomc.common.pack.data.PackRegistries;
import cn.xm1221.ygomc.common.pack.model.Pull;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 卡包：右键开封，按权重抽若干张卡。
 *
 * <h2>为什么卡包只存「是哪一种包」，不存里面有什么</h2>
 * 抽卡结果必须由<b>开启的那一刻</b>在服务端决定，否则客户端可以提前读到未来
 * （每个客户端都拿到了完整的卡包内容，改包就能偷看）。所以物品上只留
 * {@code pack_id}——它是「哪一版卡包」的键，真正的概率表、卡池、
 * 每种稀有度的抽中权重全部在服务端按这个键查表（PLAN §5.3 的
 * {@code Distribution}/{@code Pull}/{@code PullEntry} 模型）。
 *
 * <p>这条设计还有一个副作用是好的：调整某版卡包的概率表不需要动已发出的物品，
 * 也不需要发模组更新。
 */
public class CardPackItem extends Item {

    public CardPackItem(Properties properties) {
        super(properties);
    }

    /**
     * 读取卡包种类。
     *
     * @return 卡包种类键；该 stack 不带 {@code pack_id} 时返回 {@code null}
     */
    /**
     * 这一包怎么抽。优先读 {@code ygomc:booster}；只有旧的 {@code pack_id} 时按同名 id 查注册表。
     *
     * @return 找不到定义时返回 {@code null}，调用方不应当消耗这包
     */
    public static Holder<BoosterDef> resolve(ItemStack stack, RegistryAccess access) {
        DataComponentType<Holder<BoosterDef>> type = CardComponents.BOOSTER.get();
        Holder<BoosterDef> direct = stack.get(type);
        if (direct != null) {
            return direct;
        }
        ResourceLocation id = packId(stack);
        if (id == null) {
            return null;
        }
        return access.registry(PackRegistries.BOOSTER)
                .flatMap(registry -> registry.getHolder(id))
                .orElse(null);
    }

    public static ResourceLocation packId(ItemStack stack) {
        DataComponentType<ResourceLocation> type = CardComponents.PACK_ID.get();
        return stack.get(type);
    }

    /**
     * 右键开一包。潜行把这一叠都开掉。
     * 客户端只确认动作，扣包和发牌都在服务端。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        int times = player.isShiftKeyDown() ? stack.getCount() : 1;
        int opened = 0;
        for (int i = 0; i < times; i++) {
            if (stack.isEmpty() || !PackOpening.openPack(serverPlayer, stack, times == 1)) {
                break;
            }
            opened++;
        }
        if (times > 1 && opened > 0) {
            serverPlayer.displayClientMessage(Component.translatable("ygomc.pack.opened_many", opened), true);
        }
        return opened > 0 ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Holder<BoosterDef> holder = stack.get(CardComponents.BOOSTER.get());
        if ((holder == null || !holder.isBound()) && context.registries() instanceof RegistryAccess access) {
            holder = resolve(stack, access);
        }
        if (holder == null || !holder.isBound()) {
            return;
        }
        BoosterDef def = holder.value();
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (Pull pull : def.pulls()) {
            min = Math.min(min, pull.cards());
            max = Math.max(max, pull.cards());
        }
        if (min == max) {
            tooltip.add(Component.translatable("ygomc.pack.cards", min));
        } else {
            tooltip.add(Component.translatable("ygomc.pack.cards_range", min, max));
        }
        if (def.set().isBound()) {
            tooltip.add(def.set().value().name());
            def.set().value().release().ifPresent(date ->
                    tooltip.add(Component.translatable("ygomc.pack.release", date)));
        }
        if (PackTips.shiftDown.getAsBoolean()) {
            tooltip.addAll(PackOpening.oddsLines(def.mechanics()));
        } else {
            tooltip.add(Component.translatable("ygomc.pack.shift"));
        }
    }
}
