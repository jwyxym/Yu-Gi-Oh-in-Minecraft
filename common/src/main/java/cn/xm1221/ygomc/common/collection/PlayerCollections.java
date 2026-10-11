package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardRef;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 末影卡盒与末影卡册的内容。跟玩家走，不跟物品走：手里哪一个末影卡盒打开的都是同一副卡组。
 *
 * <p>存在主世界的 {@link SavedData} 里，而不是玩家的 NBT。玩家数据要 mixin 才能追加，
 * 两个平台各写一份；世界存档这一条路两边一样。换维度也不会丢，因为只挂在主世界上。
 */
public final class PlayerCollections extends SavedData {

    private static final String ID = "ygomc_player_collections";
    private static final SavedData.Factory<PlayerCollections> FACTORY = new SavedData.Factory<>(
            PlayerCollections::new, PlayerCollections::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, DeckCards> decks = new HashMap<>();
    private final Map<UUID, BinderContents> binders = new HashMap<>();

    public static PlayerCollections get(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("没有服务器，读不到末影卡册");
        }
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public DeckCards deck(UUID player) {
        return decks.getOrDefault(player, DeckCards.EMPTY);
    }

    public void setDeck(UUID player, DeckCards deck) {
        if (deck.main().isEmpty() && deck.extra().isEmpty() && deck.side().isEmpty()) {
            decks.remove(player);
        } else {
            decks.put(player, deck);
        }
        setDirty();
    }

    public BinderContents binder(UUID player) {
        return binders.getOrDefault(player, BinderContents.EMPTY);
    }

    public void setBinder(UUID player, BinderContents contents) {
        if (contents.blank()) {
            binders.remove(player);
        } else {
            binders.put(player, contents);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag deckList = new ListTag();
        for (Map.Entry<UUID, DeckCards> entry : decks.entrySet()) {
            CompoundTag one = entry.getValue().save();
            one.putUUID("player", entry.getKey());
            deckList.add(one);
        }
        tag.put("decks", deckList);
        ListTag binderList = new ListTag();
        for (Map.Entry<UUID, BinderContents> entry : binders.entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putUUID("player", entry.getKey());
            ListTag pageList = new ListTag();
            for (java.util.List<CardRef> page : entry.getValue().pages()) {
                CompoundTag pageTag = new CompoundTag();
                writeRefs(pageTag, page);
                pageList.add(pageTag);
            }
            one.put("pages", pageList);
            binderList.add(one);
        }
        tag.put("binders", binderList);
        return tag;
    }

    private static PlayerCollections load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerCollections data = new PlayerCollections();
        for (Tag element : tag.getList("decks", Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) element;
            data.decks.put(one.getUUID("player"), DeckCards.load(one));
        }
        for (Tag element : tag.getList("binders", Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) element;
            data.binders.put(one.getUUID("player"), readBinder(one));
        }
        return data;
    }

    private static void writeRefs(CompoundTag tag, java.util.List<CardRef> refs) {
        DeckCards holder = new DeckCards(refs, java.util.List.of(), java.util.List.of());
        CompoundTag saved = holder.save();
        tag.putIntArray("main", saved.getIntArray("main"));
        tag.putByteArray("main_rarity", saved.getByteArray("main_rarity"));
        tag.putIntArray("main_variant", saved.getIntArray("main_variant"));
    }

    private static java.util.List<CardRef> readRefs(CompoundTag tag) {
        return DeckCards.load(tag).main();
    }

    private static BinderContents readBinder(CompoundTag tag) {
        if (tag.contains("pages", Tag.TAG_LIST)) {
            java.util.ArrayList<java.util.List<CardRef>> pages = new java.util.ArrayList<>();
            for (Tag element : tag.getList("pages", Tag.TAG_COMPOUND)) {
                pages.add(readRefs((CompoundTag) element));
            }
            return new BinderContents(pages);
        }
        return BinderContents.migrated(readRefs(tag));
    }
}
