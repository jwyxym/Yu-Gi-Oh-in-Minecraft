package cn.xm1221.ygomc.common.collection;

import cn.xm1221.ygomc.common.card.CardRef;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 一本卡册。页由玩家翻出来，不是按张数自动拆开。
 *
 * <p>每一页最多 {@link #PAGE_MAX} 张。旧存档里那一整列卡按这个上限拆成页，避免多出来的卡被藏住。
 */
public record BinderContents(List<List<CardRef>> pages) {

    public static final int PAGE_MAX = 9;

    public static final BinderContents EMPTY = new BinderContents(List.of(List.of()));

    private static final Codec<List<CardRef>> CARDS = CardRef.CODEC.listOf();
    private static final Codec<List<List<CardRef>>> PAGES = CARDS.listOf();

    public static final Codec<BinderContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PAGES.optionalFieldOf("pages").forGetter(contents -> Optional.of(contents.pages)),
            CARDS.optionalFieldOf("cards").forGetter(contents -> Optional.empty())
    ).apply(instance, BinderContents::fromStored));

    private static final StreamCodec<ByteBuf, List<CardRef>> PAGE_CODEC =
            CardRef.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, BinderContents> STREAM_CODEC =
            PAGE_CODEC.apply(ByteBufCodecs.list()).map(BinderContents::new, BinderContents::pages);

    public BinderContents {
        if (pages.isEmpty()) {
            pages = List.of(List.of());
        } else {
            ArrayList<List<CardRef>> copy = new ArrayList<>(pages.size());
            for (List<CardRef> page : pages) {
                copy.add(List.copyOf(page));
            }
            pages = List.copyOf(copy);
        }
    }

    /** 全部卡，按页序排。组卡扣卡时用这个下标。 */
    public List<CardRef> cards() {
        ArrayList<CardRef> all = new ArrayList<>();
        for (List<CardRef> page : pages) {
            all.addAll(page);
        }
        return all;
    }

    public boolean blank() {
        return pages.size() <= 1 && cards().isEmpty();
    }

    public List<CardRef> page(int index) {
        if (index < 0 || index >= pages.size()) {
            return List.of();
        }
        return pages.get(index);
    }

    /** 在末尾加一页空白。最后一页已经是空的就不再加。 */
    public BinderContents addPage() {
        if (pages.get(pages.size() - 1).isEmpty()) {
            return this;
        }
        ArrayList<List<CardRef>> next = new ArrayList<>(pages);
        next.add(List.of());
        return new BinderContents(next);
    }

    /** @return 这一页已满或页码不对时 {@code null} */
    public BinderContents plus(int page, CardRef ref) {
        if (page < 0 || page >= pages.size() || pages.get(page).size() >= PAGE_MAX) {
            return null;
        }
        ArrayList<List<CardRef>> next = copyPages();
        ArrayList<CardRef> row = new ArrayList<>(next.get(page));
        row.add(ref);
        next.set(page, row);
        return new BinderContents(next);
    }

    public BinderContents without(int page, int index) {
        if (page < 0 || page >= pages.size() || index < 0 || index >= pages.get(page).size()) {
            return null;
        }
        ArrayList<List<CardRef>> next = copyPages();
        ArrayList<CardRef> row = new ArrayList<>(next.get(page));
        row.remove(index);
        next.set(page, row);
        return new BinderContents(next);
    }

    /** 组卡扣卡用的是 {@link #cards()} 的下标。 */
    public BinderContents withoutFlat(int flat) {
        int seen = 0;
        for (int page = 0; page < pages.size(); page++) {
            int size = pages.get(page).size();
            if (flat < seen + size) {
                return without(page, flat - seen);
            }
            seen += size;
        }
        return null;
    }

    public BinderContents move(int page, int from, int to) {
        List<CardRef> row = page(page);
        if (from < 0 || to < 0 || from >= row.size() || to >= row.size() || from == to) {
            return this;
        }
        ArrayList<CardRef> nextRow = new ArrayList<>(row);
        nextRow.add(to, nextRow.remove(from));
        ArrayList<List<CardRef>> next = copyPages();
        next.set(page, nextRow);
        return new BinderContents(next);
    }

    public BinderContents replace(int page, List<CardRef> row) {
        if (page < 0 || page >= pages.size()) {
            return this;
        }
        ArrayList<List<CardRef>> next = copyPages();
        next.set(page, row);
        return new BinderContents(next);
    }

    /** 旧的一整列卡，按每页 {@link #PAGE_MAX} 张切开。 */
    public static BinderContents migrated(List<CardRef> cards) {
        if (cards.isEmpty()) {
            return EMPTY;
        }
        ArrayList<List<CardRef>> pages = new ArrayList<>();
        for (int i = 0; i < cards.size(); i += PAGE_MAX) {
            pages.add(new ArrayList<>(cards.subList(i, Math.min(cards.size(), i + PAGE_MAX))));
        }
        return new BinderContents(pages);
    }

    private static BinderContents fromStored(Optional<List<List<CardRef>>> pages, Optional<List<CardRef>> cards) {
        if (pages.isPresent()) {
            return new BinderContents(pages.get());
        }
        if (cards.isPresent()) {
            return migrated(cards.get());
        }
        return EMPTY;
    }

    private ArrayList<List<CardRef>> copyPages() {
        ArrayList<List<CardRef>> next = new ArrayList<>(pages.size());
        for (List<CardRef> page : pages) {
            next.add(new ArrayList<>(page));
        }
        return next;
    }
}
