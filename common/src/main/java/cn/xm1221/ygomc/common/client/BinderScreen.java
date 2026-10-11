package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.collection.CollectionText;
import cn.xm1221.ygomc.common.collection.CollectionView;
import cn.xm1221.ygomc.common.data.CardDataDb;
import cn.xm1221.ygomc.common.data.DataPacks;
import cn.xm1221.ygomc.common.net.CollectionNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 卡册。页是存着的：翻过最后一页就多出一页空白，这一页最多九张。
 * 搜索只影响这一页怎么显示，不改存着的顺序。
 *
 * <p>一页一张时画大图和卡文；两到四张用四格；五到九张用九宫格。
 * 物品栏里的散卡留在最下面，点一下放进册里。
 */
public final class BinderScreen extends Screen {

    private CollectionView view;
    private int[] pool = new int[0];
    private EditBox search;
    private Button sortButton;
    private String lastQuery = "";
    private int page;
    private boolean wantNewPage;
    private int looseScroll;
    private final CardHover hover = new CardHover();

    public BinderScreen(CollectionView view) {
        this(view, 0);
    }

    public BinderScreen(CollectionView view, int page) {
        super(titleOf(view));
        this.page = page;
        apply(view);
    }

    public int token() {
        return view.token();
    }

    public void apply(CollectionView view) {
        int before = this.view == null ? 0 : this.view.pages().size();
        this.view = view;
        refreshPool();
        if (wantNewPage && view.pages().size() > before) {
            page = view.pages().size() - 1;
        }
        wantNewPage = false;
        if (view.mode() != CollectionView.MODE_CREATIVE) {
            page = Math.max(0, Math.min(page, Math.max(1, view.pages().size()) - 1));
        }
    }

    private void refreshPool() {
        if (view.mode() != CollectionView.MODE_CREATIVE) {
            pool = new int[0];
            return;
        }
        CardDataDb db = DataPacks.get().cardData();
        pool = db == null ? new int[0] : db.codes();
    }

    @Override
    protected void init() {
        search = new EditBox(font, 6, 18, 110, 14, CollectionText.c(CollectionText.SEARCH));
        search.setHint(CollectionText.c(CollectionText.SEARCH));
        search.setResponder(text -> {
            if (!text.equals(lastQuery)) {
                lastQuery = text;
                if (view.mode() == CollectionView.MODE_CREATIVE) {
                    page = 0;
                }
            }
        });
        addRenderableWidget(search);
        sortButton = Button.builder(CollectionText.c(CollectionText.SORT), b ->
                        minecraft.setScreen(new BinderSortScreen(view, page)))
                .bounds(122, 16, 40, 16).build();
        addRenderableWidget(sortButton);
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                .bounds(width - 70, 16, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                .bounds(width - 28, 16, 20, 16).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CollectionTextures.panel(g, 0, 0, width, height);
        boolean creative = view.mode() == CollectionView.MODE_CREATIVE;
        if (sortButton != null) {
            sortButton.visible = !creative;
        }
        g.drawString(font, title, 6, 4, 0xFFF3E6C8, false);
        PageSlice slice = slice(creative);
        List<Entry> shown = slice.shown;
        int pages = slice.pages;
        int start = slice.start;
        int n = slice.n;
        g.drawString(font, CollectionText.c(CollectionText.PAGE_N, page + 1, pages, slice.cards),
                width - 150, 4, 0xFFC8B898, false);
        int hoverCode = drawPage(g, mouseX, mouseY, shown, start, n);
        drawLoose(g, mouseX, mouseY);
        if (hoverCode >= 0 && n != 1) {
            hover.at(hoverCode);
            int ready = hover.ready();
            if (ready >= 0) {
                CardTips.draw(g, font, ready, mouseX, mouseY, width, height, mouseX > width / 2);
            }
        } else {
            hover.clear();
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        boolean creative = view.mode() == CollectionView.MODE_CREATIVE;
        PageSlice slice = slice(creative);
        int index = hitPage(mx, my, slice.n);
        if (index >= 0 && slice.start + index < slice.shown.size()) {
            Entry entry = slice.shown.get(slice.start + index);
            CollectionClient.act(view.token(), CollectionNet.BINDER_TAKE,
                    creative ? entry.code : entry.index, page);
            return true;
        }
        int loose = hitLoose(mx, my);
        if (loose >= 0) {
            CollectionClient.act(view.token(), CollectionNet.BINDER_PUT, view.loose().get(loose).slot(), page);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (my > height - 40) {
            looseScroll = Math.max(0, looseScroll + (scrollY > 0 ? -1 : 1));
            return true;
        }
        turn(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void turn(int delta) {
        if (delta == 0) {
            return;
        }
        if (view.mode() == CollectionView.MODE_CREATIVE) {
            int pages = Math.max(1, filtered(true).size());
            page = Math.max(0, Math.min(pages - 1, page + delta));
            return;
        }
        int count = Math.max(1, view.pages().size());
        if (delta > 0 && page >= count - 1) {
            List<Integer> current = page < view.pages().size() ? view.pages().get(page) : List.of();
            if (!current.isEmpty() && !wantNewPage) {
                wantNewPage = true;
                CollectionClient.act(view.token(), CollectionNet.BINDER_ADD_PAGE, 0);
            }
            return;
        }
        page = Math.max(0, Math.min(count - 1, page + delta));
    }

    @Override
    public void onClose() {
        CollectionClient.act(view.token(), CollectionNet.CLOSE, 0);
        super.onClose();
    }

    private int drawPage(GuiGraphics g, int mouseX, int mouseY, List<Entry> shown, int start, int n) {
        int top = 40;
        int bottom = height - 44;
        if (n <= 0) {
            return -1;
        }
        if (n == 1) {
            Entry entry = shown.get(start);
            int artH = Math.max(40, bottom - top);
            int artW = artH * 200 / 290;
            if (artW > width / 2) {
                artW = width / 2;
                artH = artW * 290 / 200;
            }
            CardArt.draw(g, entry.code, 8, top, artW, artH, null);
            int tx = 16 + artW;
            int ty = top;
            String name = CardTips.name(entry.code);
            g.drawString(font, name == null ? "#" + entry.code : name, tx, ty, 0xFFFFE080, false);
            ty += 12;
            String stats = CardTips.statsLine(entry.code);
            if (stats != null) {
                g.drawString(font, stats, tx, ty, 0xFFCCCCCC, false);
                ty += 12;
            }
            String desc = CardTips.desc(entry.code);
            if (desc != null) {
                for (String line : CardTips.wrap(font, desc, width - tx - 8)) {
                    if (ty > top + artH - 10) {
                        break;
                    }
                    g.drawString(font, line, tx, ty, 0xFFB0B0B0, false);
                    ty += 10;
                }
            }
            return entry.code;
        }
        int cols = n <= 4 ? 2 : 3;
        int rows = n <= 4 ? 2 : 3;
        int cellW = Math.min(72, (width - 16) / cols);
        int cellH = cellW * 290 / 200;
        int gridW = cellW * cols;
        int gridH = cellH * rows;
        int ox = (width - gridW) / 2;
        int oy = top + Math.max(0, (bottom - top - gridH) / 2);
        int hover = -1;
        for (int i = 0; i < n; i++) {
            int x = ox + (i % cols) * cellW;
            int y = oy + (i / cols) * cellH;
            Entry entry = shown.get(start + i);
            CardArt.draw(g, entry.code, x + 2, y + 2, cellW - 4, cellH - 4, null);
            if (mouseX >= x && mouseY >= y && mouseX < x + cellW && mouseY < y + cellH) {
                hover = entry.code;
            }
        }
        return hover;
    }

    private int hitPage(double mx, double my, int n) {
        if (n <= 1) {
            return n == 1 && my >= 40 && my < height - 44 ? 0 : -1;
        }
        int top = 40;
        int bottom = height - 44;
        int cols = n <= 4 ? 2 : 3;
        int cellW = Math.min(72, (width - 16) / cols);
        int cellH = cellW * 290 / 200;
        int gridW = cellW * cols;
        int gridH = cellH * (n <= 4 ? 2 : 3);
        int ox = (width - gridW) / 2;
        int oy = top + Math.max(0, (bottom - top - gridH) / 2);
        if (mx < ox || my < oy) {
            return -1;
        }
        int col = (int) ((mx - ox) / cellW);
        int row = (int) ((my - oy) / cellH);
        if (col < 0 || row < 0 || col >= cols) {
            return -1;
        }
        int index = row * cols + col;
        return index >= 0 && index < n ? index : -1;
    }

    private void drawLoose(GuiGraphics g, int mouseX, int mouseY) {
        int y = height - 36;
        g.drawString(font, CollectionText.c(CollectionText.OWNED), 6, y - 10, 0xFFC8B898, false);
        int x = 6 - looseScroll * 20;
        for (CollectionView.Loose card : view.loose()) {
            if (x > -20 && x < width) {
                CardArt.drawIcon(g, card.code(), x, y, 16);
            }
            x += 20;
        }
    }

    private int hitLoose(double mx, double my) {
        int y = height - 36;
        if (my < y || my >= y + 24) {
            return -1;
        }
        int index = (int) ((mx - 6) / 20) + looseScroll;
        if (index < 0 || index >= view.loose().size()) {
            return -1;
        }
        int x = 6 + (index - looseScroll) * 20;
        return mx >= x && mx < x + 16 ? index : -1;
    }

    private List<Entry> filtered(boolean creative) {
        String q = query();
        ArrayList<Entry> out = new ArrayList<>();
        if (creative) {
            refreshPool();
            // 卡池本来的顺序，不另外排序。一页一张，从第一张翻起。
            for (int code : pool) {
                if (q.isEmpty() || matches(code, q)) {
                    out.add(new Entry(code, code));
                }
            }
            return out;
        }
        if (page < 0 || page >= view.pages().size()) {
            return out;
        }
        List<Integer> codes = view.pages().get(page);
        for (int i = 0; i < codes.size(); i++) {
            if (q.isEmpty() || matches(codes.get(i), q)) {
                out.add(new Entry(codes.get(i), i));
            }
        }
        return out;
    }

    /** 创造卡册按卡池一页一张。普通卡册的页是存着的，这一页上有几张就画几张。 */
    private PageSlice slice(boolean creative) {
        List<Entry> shown = filtered(creative);
        if (creative) {
            int pages = Math.max(1, shown.size());
            if (page >= pages) {
                page = pages - 1;
            }
            int n = page < shown.size() ? 1 : 0;
            return new PageSlice(shown, pages, page, n, shown.size());
        }
        int pages = Math.max(1, view.pages().size());
        if (page >= pages) {
            page = pages - 1;
        }
        int cards = 0;
        for (List<Integer> stored : view.pages()) {
            cards += stored.size();
        }
        return new PageSlice(shown, pages, 0, shown.size(), cards);
    }

    private String query() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(int code, String query) {
        String name = DataPacks.get().nameOf(code);
        return name != null && name.toLowerCase(Locale.ROOT).contains(query);
    }

    private static Component titleOf(CollectionView view) {
        String key = switch (view.mode()) {
            case CollectionView.MODE_CREATIVE -> CollectionText.CREATIVE_TITLE;
            case CollectionView.MODE_ENDER -> CollectionText.ENDER_BINDER_TITLE;
            default -> CollectionText.BINDER_TITLE;
        };
        return CollectionText.c(key);
    }

    private record Entry(int code, int index) {
    }

    private record PageSlice(List<Entry> shown, int pages, int start, int n, int cards) {
    }
}
