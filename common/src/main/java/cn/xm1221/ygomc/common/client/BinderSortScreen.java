package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.collection.BinderArrange;
import cn.xm1221.ygomc.common.collection.CollectionText;
import cn.xm1221.ygomc.common.collection.CollectionView;
import cn.xm1221.ygomc.common.net.CollectionNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * 卡册这一页的排序。上面五个键重排当前页，列表里拖动是玩家自己的顺序。
 */
public final class BinderSortScreen extends Screen {

    private static final int ROW = 14;

    private CollectionView view;
    private final int page;
    private int scroll;
    private int dragFrom = -1;
    private boolean dragging;
    private double pressY;

    public BinderSortScreen(CollectionView view, int page) {
        super(CollectionText.c(CollectionText.SORT));
        this.view = view;
        this.page = page;
    }

    public int token() {
        return view.token();
    }

    public void apply(CollectionView view) {
        this.view = view;
    }

    @Override
    protected void init() {
        int x = 6;
        addRenderableWidget(sortButton(CollectionText.SORT_NAME, BinderArrange.BY_NAME, x));
        x += 36;
        addRenderableWidget(sortButton(CollectionText.SORT_ATK, BinderArrange.BY_ATTACK, x));
        x += 36;
        addRenderableWidget(sortButton(CollectionText.SORT_DEF, BinderArrange.BY_DEFENSE, x));
        x += 36;
        addRenderableWidget(sortButton(CollectionText.SORT_LEVEL, BinderArrange.BY_LEVEL, x));
        x += 36;
        addRenderableWidget(sortButton(CollectionText.SORT_CODE, BinderArrange.BY_CODE, x));
        addRenderableWidget(Button.builder(CollectionText.c(CollectionText.SORT_BACK),
                        b -> minecraft.setScreen(new BinderScreen(view, page)))
                .bounds(width - 46, 4, 40, 16).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CollectionTextures.panel(g, 0, 0, width, height);
        g.drawString(font, title, 6, 4, 0xFFF3E6C8, false);
        g.drawString(font, CollectionText.c(CollectionText.SORT_HINT), 6, 24, 0xFFC8B898, false);
        int top = 40;
        int rows = Math.max(1, (height - top - 8) / ROW);
        List<Integer> cards = pageCards();
        int total = cards.size();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - rows)));
        for (int i = 0; i < rows && scroll + i < total; i++) {
            int index = scroll + i;
            int y = top + i * ROW;
            int code = cards.get(index);
            if (dragging && index == dragFrom) {
                y = (int) pressY;
            }
            CollectionTextures.bar(g, 6, y, width - 12, ROW - 1);
            CardArt.drawIcon(g, code, 8, y, 10);
            String name = CardTips.name(code);
            g.drawString(font, name == null ? "#" + code : name, 24, y + 2, 0xFFF8F0E0, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        dragFrom = rowAt(my);
        dragging = false;
        pressY = my;
        return dragFrom >= 0;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragFrom >= 0) {
            dragging = true;
            pressY = my;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        int from = dragFrom;
        boolean moved = dragging;
        dragFrom = -1;
        dragging = false;
        if (from < 0 || !moved) {
            return super.mouseReleased(mx, my, button);
        }
        int to = rowAt(my);
        if (to >= 0 && to != from) {
            CollectionClient.act(view.token(), CollectionNet.BINDER_MOVE, from, to, page);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll + (scrollY > 0 ? -1 : 1));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new BinderScreen(view, page));
    }

    private Button sortButton(String key, int mode, int x) {
        return Button.builder(CollectionText.c(key), b ->
                        CollectionClient.act(view.token(), CollectionNet.BINDER_SORT, mode, page))
                .bounds(x, 4, 34, 16).build();
    }

    private int rowAt(double my) {
        int top = 40;
        int rows = Math.max(1, (height - top - 8) / ROW);
        int local = (int) ((my - top) / ROW);
        if (local < 0 || local >= rows) {
            return -1;
        }
        int index = scroll + local;
        return index >= 0 && index < pageCards().size() ? index : -1;
    }

    private List<Integer> pageCards() {
        if (page < 0 || page >= view.pages().size()) {
            return List.of();
        }
        return view.pages().get(page);
    }
}
