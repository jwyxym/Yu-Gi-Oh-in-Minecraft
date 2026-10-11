package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardComponents;
import cn.xm1221.ygomc.common.card.CardItem;
import cn.xm1221.ygomc.common.card.CardRarity;
import cn.xm1221.ygomc.common.card.CardRef;
import cn.xm1221.ygomc.common.card.DeckData;
import cn.xm1221.ygomc.common.data.CardDataDb;
import cn.xm1221.ygomc.common.data.DataPacks;
import cn.xm1221.ygomc.common.deck.DeckBoxItem;
import cn.xm1221.ygomc.common.deck.DeckIo;
import cn.xm1221.ygomc.common.net.CollectionNet;
import cn.xm1221.ygomc.common.registry.YgomcItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 组卡和卡册的权威端。客户端只发「把哪张放进哪一区」，扣卡、退卡、写回盒子都在这里。
 *
 * <p>可用的卡按物品栏槽位从左到右找：槽里的散卡，然后是这个槽上卡册里的卡。
 * 末影卡册的内容只算一次，哪怕背包里放了好几本。创造卡册在背包里时不扣卡，
 * 卡池里有的都能放进卡组。
 */
public final class CollectionServer {

    private static final Map<UUID, Session> SESSIONS = new java.util.HashMap<>();
    private static int nextToken = 1;

    private CollectionServer() {
    }

    public static void openDeck(ServerPlayer player, InteractionHand hand) {
        int slot = slotOf(player, hand);
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof DeckBoxItem box)) {
            return;
        }
        int mode = box.ender() ? CollectionView.MODE_ENDER : CollectionView.MODE_ITEM;
        open(player, CollectionView.SCREEN_DECK, mode, slot);
    }

    public static void openBinder(ServerPlayer player, InteractionHand hand) {
        int slot = slotOf(player, hand);
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof CardBinderItem binder)) {
            return;
        }
        int mode = switch (binder.kind()) {
            case CREATIVE -> CollectionView.MODE_CREATIVE;
            case ENDER -> CollectionView.MODE_ENDER;
            case ITEM -> CollectionView.MODE_ITEM;
        };
        open(player, CollectionView.SCREEN_BINDER, mode, slot);
    }

    public static void act(ServerPlayer player, int token, int action, int arg, int arg2, int arg3) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.token != token) {
            return;
        }
        if (action == CollectionNet.CLOSE) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        switch (session.screen) {
            case CollectionView.SCREEN_DECK -> actDeck(player, session, action, arg);
            case CollectionView.SCREEN_BINDER -> actBinder(player, session, action, arg, arg2, arg3);
            default -> {
            }
        }
    }

    /**
     * 用剪贴板里的 ydk 文本换这副卡组。盒子里原有的卡先算作「手里有」，
     * 再扣物品栏和卡册。没有的跳过，能放的都放。放不回物品栏的原卡留在卡组里，不丢。
     */
    public static void importYdk(ServerPlayer player, int token, String text) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.token != token || session.screen != CollectionView.SCREEN_DECK) {
            return;
        }
        DeckData parsed;
        try {
            parsed = DeckIo.parse(Arrays.asList(text.split("\\R")));
        } catch (IllegalArgumentException e) {
            tell(player, CollectionText.IMPORT_BAD);
            return;
        }
        DeckCards current = readDeck(player, session);
        if (current == null) {
            moved(player);
            return;
        }
        ArrayList<CardRef> held = new ArrayList<>();
        held.addAll(current.main());
        held.addAll(current.extra());
        held.addAll(current.side());
        Supply supply = scan(player);
        int[] score = new int[2];
        DeckCards built = DeckCards.EMPTY;
        built = fillZone(player, supply, held, built, parsed.main(), DeckMove.Zone.MAIN, score);
        built = fillZone(player, supply, held, built, parsed.extra(), DeckMove.Zone.EXTRA, score);
        built = fillZone(player, supply, held, built, parsed.side(), DeckMove.Zone.SIDE, score);
        int placed = score[0];
        int missed = score[1];
        for (CardRef leftover : held) {
            if (!give(player, leftover)) {
                DeckMove.Zone zone = extraOnly(leftover.cardCode()) ? DeckMove.Zone.EXTRA : DeckMove.Zone.MAIN;
                built = DeckMove.add(built, zone, leftover);
            }
        }
        writeDeck(player, session, built);
        player.displayClientMessage(CollectionText.c(CollectionText.IMPORT_RESULT, placed, missed), true);
        refresh(player, session);
    }

    private static DeckCards fillZone(ServerPlayer player, Supply supply, List<CardRef> held,
                                      DeckCards built, List<Integer> codes, DeckMove.Zone zone, int[] score) {
        for (int code : codes) {
            CardDataDb.Stats stats = stats(code);
            if (stats == null || DeckMove.reject(zone, stats.type(), DeckMove.zone(built, zone).size()) != null) {
                score[1]++;
                continue;
            }
            CardRef ref = pull(held, code);
            if (ref == null && supply.unlimited) {
                ref = CardRef.of(code, CardRarity.COMMON);
            } else if (ref == null) {
                Source source = find(supply, code);
                if (source == null || !consume(player, source)) {
                    score[1]++;
                    continue;
                }
                ref = source.ref;
                rescan(player, supply);
            }
            built = DeckMove.add(built, zone, ref);
            score[0]++;
        }
        return built;
    }

    private static void rescan(ServerPlayer player, Supply supply) {
        Supply again = scan(player);
        supply.sources.clear();
        supply.sources.addAll(again.sources);
        supply.unlimited = again.unlimited;
    }

    private static CardRef pull(List<CardRef> held, int code) {
        for (int i = 0; i < held.size(); i++) {
            if (held.get(i).cardCode() == code) {
                return held.remove(i);
            }
        }
        return null;
    }

    private static boolean extraOnly(int code) {
        CardDataDb.Stats stats = stats(code);
        return stats != null && (stats.type() & DeckMove.EXTRA_ONLY) != 0;
    }

    private static void open(ServerPlayer player, int screen, int mode, int slot) {
        Session session = new Session(nextToken++, screen, mode, slot);
        SESSIONS.put(player.getUUID(), session);
        CollectionNet.send(player, snapshot(player, session));
    }

    private static void actDeck(ServerPlayer player, Session session, int action, int arg) {
        DeckMove.Zone zone = switch (action) {
            case CollectionNet.ADD_MAIN, CollectionNet.TAKE_MAIN -> DeckMove.Zone.MAIN;
            case CollectionNet.ADD_EXTRA, CollectionNet.TAKE_EXTRA -> DeckMove.Zone.EXTRA;
            case CollectionNet.ADD_SIDE, CollectionNet.TAKE_SIDE -> DeckMove.Zone.SIDE;
            default -> null;
        };
        if (zone == null) {
            return;
        }
        if (action == CollectionNet.ADD_MAIN || action == CollectionNet.ADD_EXTRA
                || action == CollectionNet.ADD_SIDE) {
            addToDeck(player, session, zone, arg);
        } else {
            takeFromDeck(player, session, zone, arg);
        }
    }

    private static void addToDeck(ServerPlayer player, Session session, DeckMove.Zone zone, int code) {
        DeckCards deck = readDeck(player, session);
        if (deck == null) {
            moved(player);
            return;
        }
        CardDataDb.Stats stats = stats(code);
        if (stats == null) {
            tell(player, CollectionText.ERR_UNKNOWN);
            return;
        }
        String reject = DeckMove.reject(zone, stats.type(), DeckMove.zone(deck, zone).size());
        if (reject != null) {
            tell(player, reject);
            return;
        }
        Supply supply = scan(player);
        CardRef ref;
        if (supply.unlimited) {
            ref = CardRef.of(code, CardRarity.COMMON);
        } else {
            Source source = find(supply, code);
            if (source == null || !consume(player, source)) {
                tell(player, CollectionText.ERR_NO_CARD);
                return;
            }
            ref = source.ref;
        }
        writeDeck(player, session, DeckArrange.arrange(DeckMove.add(deck, zone, ref)));
        refresh(player, session);
    }

    private static void takeFromDeck(ServerPlayer player, Session session, DeckMove.Zone zone, int index) {
        DeckCards deck = readDeck(player, session);
        if (deck == null) {
            moved(player);
            return;
        }
        List<CardRef> cards = DeckMove.zone(deck, zone);
        if (index < 0 || index >= cards.size()) {
            return;
        }
        DeckCards next = DeckMove.remove(deck, zone, index);
        if (next == null || !give(player, cards.get(index))) {
            tell(player, CollectionText.ERR_INVENTORY);
            return;
        }
        writeDeck(player, session, DeckArrange.arrange(next));
        refresh(player, session);
    }

    private static void actBinder(ServerPlayer player, Session session, int action, int arg, int arg2, int arg3) {
        if (action == CollectionNet.BINDER_PUT) {
            putInBinder(player, session, arg, arg2);
        } else if (action == CollectionNet.BINDER_TAKE) {
            takeFromBinder(player, session, arg, arg2);
        } else if (action == CollectionNet.BINDER_MOVE) {
            moveBinder(player, session, arg3, arg, arg2);
        } else if (action == CollectionNet.BINDER_SORT) {
            sortBinder(player, session, arg2, arg);
        } else if (action == CollectionNet.BINDER_ADD_PAGE) {
            addBinderPage(player, session);
        }
    }

    private static void addBinderPage(ServerPlayer player, Session session) {
        if (session.mode == CollectionView.MODE_CREATIVE) {
            return;
        }
        BinderContents contents = readBinder(player, session);
        if (contents == null) {
            moved(player);
            return;
        }
        BinderContents next = contents.addPage();
        if (next != contents) {
            writeBinder(player, session, next);
        }
        refresh(player, session);
    }

    private static void moveBinder(ServerPlayer player, Session session, int page, int from, int to) {
        if (session.mode == CollectionView.MODE_CREATIVE) {
            return;
        }
        BinderContents contents = readBinder(player, session);
        if (contents == null) {
            moved(player);
            return;
        }
        BinderContents next = contents.move(page, from, to);
        if (next != contents) {
            writeBinder(player, session, next);
        }
        refresh(player, session);
    }

    private static void sortBinder(ServerPlayer player, Session session, int page, int mode) {
        if (session.mode == CollectionView.MODE_CREATIVE) {
            return;
        }
        BinderContents contents = readBinder(player, session);
        if (contents == null) {
            moved(player);
            return;
        }
        writeBinder(player, session, BinderArrange.sortPage(contents, page, mode));
        refresh(player, session);
    }

    private static void putInBinder(ServerPlayer player, Session session, int slot, int page) {
        if (session.mode == CollectionView.MODE_CREATIVE) {
            tell(player, CollectionText.ERR_CREATIVE_PUT);
            return;
        }
        if (session.mode == CollectionView.MODE_ITEM && slot == session.slot) {
            return;
        }
        Inventory inv = player.getInventory();
        ItemStack stack = inv.getItem(slot);
        CardRef ref = cardRef(stack);
        if (ref == null) {
            tell(player, CollectionText.ERR_NO_CARD);
            return;
        }
        BinderContents contents = readBinder(player, session);
        if (contents == null) {
            moved(player);
            return;
        }
        BinderContents next = contents.plus(page, ref);
        if (next == null) {
            tell(player, CollectionText.PAGE_FULL);
            return;
        }
        inv.setItem(slot, ItemStack.EMPTY);
        writeBinder(player, session, next);
        refresh(player, session);
    }

    private static void takeFromBinder(ServerPlayer player, Session session, int arg, int arg2) {
        if (session.mode == CollectionView.MODE_CREATIVE) {
            CardDataDb db = DataPacks.get().cardData();
            if (db == null || !db.contains(arg)) {
                tell(player, CollectionText.ERR_UNKNOWN);
                return;
            }
            if (!give(player, CardRef.of(arg, CardRarity.COMMON))) {
                tell(player, CollectionText.ERR_INVENTORY);
                return;
            }
            refresh(player, session);
            return;
        }
        BinderContents contents = readBinder(player, session);
        if (contents == null) {
            moved(player);
            return;
        }
        if (arg2 < 0 || arg2 >= contents.pages().size() || arg < 0 || arg >= contents.page(arg2).size()) {
            return;
        }
        CardRef ref = contents.page(arg2).get(arg);
        BinderContents next = contents.without(arg2, arg);
        if (next == null || !give(player, ref)) {
            tell(player, CollectionText.ERR_INVENTORY);
            return;
        }
        writeBinder(player, session, next);
        refresh(player, session);
    }

    private static CollectionView snapshot(ServerPlayer player, Session session) {
        Supply supply = scan(player);
        if (session.screen == CollectionView.SCREEN_DECK) {
            DeckCards deck = readDeck(player, session);
            if (deck == null) {
                return CollectionView.closed();
            }
            DeckCards arranged = DeckArrange.arrange(deck);
            if (!arranged.equals(deck)) {
                writeDeck(player, session, arranged);
                deck = arranged;
            }
            return new CollectionView(true, session.token, session.screen, session.mode, supply.unlimited,
                    codes(deck.main()), codes(deck.extra()), codes(deck.side()),
                    supply.unlimited ? List.of() : counts(supply), List.of(), List.of());
        }
        if (session.mode == CollectionView.MODE_CREATIVE) {
            return new CollectionView(true, session.token, session.screen, session.mode, true,
                    List.of(), List.of(), List.of(), List.of(), loose(player), List.of());
        }
        BinderContents contents = readBinder(player, session);
        if (contents == null) {
            return CollectionView.closed();
        }
        return new CollectionView(true, session.token, session.screen, session.mode, false,
                List.of(), List.of(), List.of(), List.of(), loose(player), pageCodes(contents));
    }

    private static DeckCards readDeck(ServerPlayer player, Session session) {
        if (session.mode == CollectionView.MODE_ENDER) {
            return PlayerCollections.get(player).deck(player.getUUID());
        }
        ItemStack stack = player.getInventory().getItem(session.slot);
        if (!(stack.getItem() instanceof DeckBoxItem box) || box.ender()) {
            return null;
        }
        return DeckBoxItem.cards(stack);
    }

    private static void writeDeck(ServerPlayer player, Session session, DeckCards deck) {
        if (session.mode == CollectionView.MODE_ENDER) {
            PlayerCollections.get(player).setDeck(player.getUUID(), deck);
            return;
        }
        Inventory inv = player.getInventory();
        ItemStack stack = inv.getItem(session.slot);
        DeckBoxItem.setCards(stack, DeckArrange.arrange(deck));
        inv.setItem(session.slot, stack);
    }

    private static BinderContents readBinder(ServerPlayer player, Session session) {
        if (session.mode == CollectionView.MODE_ENDER) {
            return PlayerCollections.get(player).binder(player.getUUID());
        }
        ItemStack stack = player.getInventory().getItem(session.slot);
        if (!(stack.getItem() instanceof CardBinderItem binder) || binder.kind() != CardBinderItem.Kind.ITEM) {
            return null;
        }
        return CardBinderItem.contents(stack);
    }

    private static void writeBinder(ServerPlayer player, Session session, BinderContents contents) {
        if (session.mode == CollectionView.MODE_ENDER) {
            PlayerCollections.get(player).setBinder(player.getUUID(), contents);
            return;
        }
        Inventory inv = player.getInventory();
        ItemStack stack = inv.getItem(session.slot);
        stack.set(CardComponents.BINDER_CONTENTS.get(), contents);
        inv.setItem(session.slot, stack);
    }

    private static Supply scan(ServerPlayer player) {
        Supply supply = new Supply();
        Inventory inv = player.getInventory();
        boolean enderSeen = false;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof CardBinderItem binder) {
                if (binder.kind() == CardBinderItem.Kind.CREATIVE) {
                    supply.unlimited = true;
                } else if (binder.kind() == CardBinderItem.Kind.ENDER) {
                    if (!enderSeen) {
                        enderSeen = true;
                        List<CardRef> cards = PlayerCollections.get(player).binder(player.getUUID()).cards();
                        for (int i = 0; i < cards.size(); i++) {
                            supply.sources.add(new Source(Pocket.ENDER, slot, i, cards.get(i)));
                        }
                    }
                } else {
                    List<CardRef> cards = CardBinderItem.contents(stack).cards();
                    for (int i = 0; i < cards.size(); i++) {
                        supply.sources.add(new Source(Pocket.BINDER, slot, i, cards.get(i)));
                    }
                }
                continue;
            }
            CardRef ref = cardRef(stack);
            if (ref != null) {
                supply.sources.add(new Source(Pocket.LOOSE, slot, 0, ref));
            }
        }
        return supply;
    }

    private static Source find(Supply supply, int code) {
        for (Source source : supply.sources) {
            if (source.ref.cardCode() == code) {
                return source;
            }
        }
        return null;
    }

    private static boolean consume(ServerPlayer player, Source source) {
        Inventory inv = player.getInventory();
        switch (source.pocket) {
            case LOOSE -> {
                CardRef ref = cardRef(inv.getItem(source.slot));
                if (ref == null || !ref.equals(source.ref)) {
                    return false;
                }
                inv.setItem(source.slot, ItemStack.EMPTY);
                return true;
            }
            case BINDER -> {
                ItemStack stack = inv.getItem(source.slot);
                if (!(stack.getItem() instanceof CardBinderItem binder) || binder.kind() != CardBinderItem.Kind.ITEM) {
                    return false;
                }
                BinderContents contents = CardBinderItem.contents(stack);
                if (source.index >= contents.cards().size()
                        || !contents.cards().get(source.index).equals(source.ref)) {
                    return false;
                }
                BinderContents next = contents.withoutFlat(source.index);
                if (next == null) {
                    return false;
                }
                stack.set(CardComponents.BINDER_CONTENTS.get(), next);
                inv.setItem(source.slot, stack);
                return true;
            }
            case ENDER -> {
                PlayerCollections stored = PlayerCollections.get(player);
                BinderContents contents = stored.binder(player.getUUID());
                if (source.index >= contents.cards().size()
                        || !contents.cards().get(source.index).equals(source.ref)) {
                    return false;
                }
                BinderContents next = contents.withoutFlat(source.index);
                if (next == null) {
                    return false;
                }
                stored.setBinder(player.getUUID(), next);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static List<CollectionView.Counted> counts(Supply supply) {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
        for (Source source : supply.sources) {
            map.merge(source.ref.cardCode(), 1, Integer::sum);
        }
        ArrayList<CollectionView.Counted> out = new ArrayList<>(map.size());
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            out.add(new CollectionView.Counted(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static List<CollectionView.Loose> loose(ServerPlayer player) {
        Inventory inv = player.getInventory();
        ArrayList<CollectionView.Loose> out = new ArrayList<>();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            CardRef ref = cardRef(inv.getItem(slot));
            if (ref != null) {
                out.add(new CollectionView.Loose(slot, ref.cardCode()));
            }
        }
        return out;
    }

    private static List<List<Integer>> pageCodes(BinderContents contents) {
        ArrayList<List<Integer>> out = new ArrayList<>();
        for (List<CardRef> page : contents.pages()) {
            out.add(codes(page));
        }
        if (out.isEmpty()) {
            out.add(List.of());
        }
        return out;
    }

    private static List<Integer> codes(List<CardRef> refs) {
        ArrayList<Integer> out = new ArrayList<>(refs.size());
        for (CardRef ref : refs) {
            out.add(ref.cardCode());
        }
        return out;
    }

    private static CardDataDb.Stats stats(int code) {
        CardDataDb db = DataPacks.get().cardData();
        return db == null ? null : db.stats(code);
    }

    private static CardRef cardRef(ItemStack stack) {
        if (!(stack.getItem() instanceof CardItem)) {
            return null;
        }
        return CardItem.ref(stack);
    }

    private static boolean give(ServerPlayer player, CardRef ref) {
        ItemStack stack = cardStack(ref);
        return player.getInventory().add(stack) && stack.isEmpty();
    }

    private static ItemStack cardStack(CardRef ref) {
        ItemStack stack = new ItemStack(YgomcItems.CARD.get());
        stack.set(CardComponents.CARD_REF.get(), ref);
        return stack;
    }

    private static void refresh(ServerPlayer player, Session session) {
        player.inventoryMenu.broadcastChanges();
        CollectionView view = snapshot(player, session);
        if (!view.open()) {
            SESSIONS.remove(player.getUUID());
        }
        CollectionNet.send(player, view);
    }

    private static void moved(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        tell(player, CollectionText.ERR_MOVED);
        CollectionNet.send(player, CollectionView.closed());
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(CollectionText.c(key), true);
    }

    private static int slotOf(ServerPlayer player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) {
            return Inventory.SLOT_OFFHAND;
        }
        return player.getInventory().selected;
    }

    private enum Pocket {
        LOOSE, BINDER, ENDER
    }

    private record Source(Pocket pocket, int slot, int index, CardRef ref) {
    }

    private static final class Supply {
        private final List<Source> sources = new ArrayList<>();
        private boolean unlimited;
    }

    private record Session(int token, int screen, int mode, int slot) {
    }
}
