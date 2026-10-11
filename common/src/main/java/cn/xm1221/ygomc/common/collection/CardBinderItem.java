package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardComponents;
import net.minecraft.network.chat.Component;
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
 * 卡册：用来存卡，不设张数上限。
 *
 * <ul>
 *   <li>{@link Kind#ITEM}：卡写在这本物品上，跟着物品走；</li>
 *   <li>{@link Kind#ENDER}：物品只是钥匙，内容跟玩家走，跟末影箱一样；</li>
 *   <li>{@link Kind#CREATIVE}：打开就是整份卡池，取走不减少。不接受放入，免得把卡吞掉。</li>
 * </ul>
 *
 * 组卡时，物品栏里的卡册（含末影、创造）会作为可消耗的来源。创造卡册在背包里时不扣卡。
 */
public class CardBinderItem extends Item {

    public enum Kind {
        ITEM, CREATIVE, ENDER
    }

    private final Kind kind;

    public CardBinderItem(Properties properties) {
        this(properties, Kind.ITEM);
    }

    public CardBinderItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static BinderContents contents(ItemStack stack) {
        BinderContents stored = stack.get(CardComponents.BINDER_CONTENTS.get());
        return stored == null ? BinderContents.EMPTY : stored;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CollectionServer.openBinder(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        switch (kind) {
            case CREATIVE -> tooltip.add(CollectionText.c(CollectionText.ALL_CARDS));
            case ENDER -> tooltip.add(CollectionText.c(CollectionText.FOLLOWS_PLAYER));
            case ITEM -> tooltip.add(CollectionText.c(CollectionText.BINDER_COUNT, contents(stack).cards().size()));
        }
    }
}
