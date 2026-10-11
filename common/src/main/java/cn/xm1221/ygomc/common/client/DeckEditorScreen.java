package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.card.DeckData;
import cn.xm1221.ygomc.common.collection.CollectionText;
import cn.xm1221.ygomc.common.collection.CollectionView;
import cn.xm1221.ygomc.common.collection.DeckMove;
import cn.xm1221.ygomc.common.collection.OverlapRow;
import cn.xm1221.ygomc.common.data.CardDataDb;
import cn.xm1221.ygomc.common.data.DataPacks;
import cn.xm1221.ygomc.common.deck.DeckIo;
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
 * 组卡：左边是卡组，卡重叠着排，不滚动。右边每种可用的卡只出现一次，旁边是张数。
 *
 * <p>拖进某个区就会放进去，放进去之后由服务端排序，落点不决定第几张。
 * 光标在一张卡上停一会儿，才弹出大图和卡文。
 */
public final class DeckEditorScreen extends Screen {

    private static final int CARD_W = 16;
    private static final int CARD_H = 24;
    private static final int PICK_W = 22;
    private static final int PICK_H = 32;

    private CollectionView view;
    private int[] pool = new int[0];
    private EditBox search;
    private int pickScroll;
    /** 介绍区一直留着上一张指到的卡，鼠标离开后不收回。 */
    private int shownCode = -1;
    private List<CollectionView.Counted> pickCache = List.of();
    private String pickKey = "";
    private Drag drag;
    private boolean dragging;
    private double pressX;
    private double pressY;

    public DeckEditorScreen(CollectionView view) {
        super(CollectionText.c(view.mode() == CollectionView.MODE_ENDER
                ? CollectionText.ENDER_DECK_TITLE : CollectionText.DECK_TITLE));
        apply(view);
    }

    public int token() {
        return view.token();
    }

    public void apply(CollectionView view) {
        this.view = view;
        if (view.unlimited()) {
            CardDataDb db = DataPacks.get().cardData();
            pool = db == null ? new int[0] : db.codes();
        } else {
            pool = new int[0];
        }
        pickKey = "";
    }

    @Override
    protected void init() {
        search = new EditBox(font, width - 120, 4, 112, 12, CollectionText.c(CollectionText.SEARCH));
        search.setHint(CollectionText.c(CollectionText.SEARCH));
        search.setResponder(text -> pickScroll = 0);
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(CollectionText.c(CollectionText.EXPORT), b -> exportDeck())
                .bounds(70, 2, 72, 14).build());
        addRenderableWidget(Button.builder(CollectionText.c(CollectionText.IMPORT), b -> importClipboard())
                .bounds(146, 2, 40, 14).build());
    }

    private void exportDeck() {
        DeckData data = new DeckData(view.main(), view.extra(), view.side());
        minecraft.keyboardHandler.setClipboard(String.join("\n", DeckIo.write(data)));
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(CollectionText.c(CollectionText.EXPORT_DONE), true);
        }
    }

    private void importClipboard() {
        if (minecraft.level == null) {
            return;
        }
        String text = minecraft.keyboardHandler.getClipboard();
        CollectionNet.sendImport(view.token(), text == null ? "" : text, minecraft.level.registryAccess());
        minecraft.setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CollectionTextures.panel(g, 0, 0, width, height);
        g.drawString(font, title, 6, 6, 0xFFF3E6C8, false);
        Layout layout = layout();
        int under = -1;
        under = drawRows(g, mouseX, mouseY, layout.main, view.main(), layout.mainRows,
                CollectionText.MAIN, 60);
        int extra = drawBand(g, mouseX, mouseY, layout.extra, view.extra(), CollectionText.EXTRA, 15);
        int side = drawBand(g, mouseX, mouseY, layout.side, view.side(), CollectionText.SIDE, 15);
        if (extra >= 0) {
            under = extra;
        }
        if (side >= 0) {
            under = side;
        }
        List<CollectionView.Counted> picks = picks();
        pickScroll = Math.max(0, Math.min(pickScroll, Math.max(0, picks.size() - layout.pickRows)));
        int pickHit = drawPicks(g, mouseX, mouseY, layout, picks);
        if (pickHit >= 0) {
            under = picks.get(pickHit).code();
        }
        if (under >= 0) {
            shownCode = under;
        }
        if (search != null) {
            search.setX(layout.pickX);
            search.setWidth(Math.max(PICK_W, layout.infoX - layout.pickX - 4));
        }
        drawScroll(g, layout, picks.size());
        drawIntro(g, layout, shownCode);
        if (dragging && drag != null) {
            CardArt.drawIcon(g, drag.code, mouseX - PICK_W / 2, mouseY - PICK_H / 2, PICK_W);
        }
        if (picks.isEmpty()) {
            g.drawString(font, CollectionText.c(CollectionText.EMPTY_SUPPLY),
                    layout.pickX, layout.pickY, 0xFFB0A090, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        pressX = mx;
        pressY = my;
        dragging = false;
        drag = dragAt(mx, my);
        return drag != null;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag != null && (mx - pressX) * (mx - pressX) + (my - pressY) * (my - pressY) > 16) {
            dragging = true;
        }
        return drag != null || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        Drag held = drag;
        boolean moved = dragging;
        drag = null;
        dragging = false;
        if (held == null) {
            return super.mouseReleased(mx, my, button);
        }
        if (!moved) {
            if (held.fromZone < 0) {
                add(held.code, extraOnly(held.code) ? DeckMove.Zone.EXTRA : DeckMove.Zone.MAIN);
            } else {
                take(held.fromZone, held.fromIndex);
            }
            return true;
        }
        Layout layout = layout();
        int zone = zoneAt(layout, mx, my);
        if (zone >= 0) {
            if (held.fromZone < 0) {
                add(held.code, zoneOf(zone));
            } else if (held.fromZone != zone) {
                take(held.fromZone, held.fromIndex);
                add(held.code, zoneOf(zone));
            }
            return true;
        }
        if (held.fromZone >= 0 && mx >= layout.pickX && mx < layout.pickX + PICK_W + 16) {
            take(held.fromZone, held.fromIndex);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        Layout layout = layout();
        if (mx >= layout.pickX && mx < layout.infoX) {
            pickScroll = Math.max(0, pickScroll + (scrollY > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        CollectionClient.act(view.token(), CollectionNet.CLOSE, 0);
        super.onClose();
    }

    private int drawRows(GuiGraphics g, int mouseX, int mouseY, Band band, List<Integer> cards,
                         int maxRows, String label, int cap) {
        g.drawString(font, CollectionText.c(CollectionText.ZONE_COUNT, CollectionText.c(label),
                cards.size(), cap), band.x, band.y, 0xFFF8F0E0, false);
        int hit = -1;
        int per = perRow(band.w, cards.size(), maxRows);
        int rows = cards.isEmpty() ? 1 : (cards.size() + per - 1) / per;
        for (int r = 0; r < rows; r++) {
            int start = r * per;
            int n = Math.min(per, Math.max(0, cards.size() - start));
            int y = band.y + 11 + r * (CARD_H + 2);
            int[] xs = OverlapRow.xs(band.x, band.w, CARD_W, n);
            for (int i = 0; i < n; i++) {
                CollectionTextures.slot(g, xs[i], y, CARD_W, CARD_H);
                CardArt.drawIcon(g, cards.get(start + i), xs[i], y, CARD_W);
            }
            int index = OverlapRow.hit(xs, CARD_W, CARD_H, y, mouseX, mouseY);
            if (index >= 0) {
                hit = cards.get(start + index);
            }
        }
        return hit;
    }

    private int drawBand(GuiGraphics g, int mouseX, int mouseY, Band band, List<Integer> cards,
                         String label, int cap) {
        g.drawString(font, CollectionText.c(CollectionText.ZONE_COUNT, CollectionText.c(label),
                cards.size(), cap), band.x, band.y, 0xFFF8F0E0, false);
        int[] xs = OverlapRow.xs(band.x, band.w, CARD_W, cards.size());
        int cardY = band.y + 11;
        for (int i = 0; i < cards.size(); i++) {
            CollectionTextures.slot(g, xs[i], cardY, CARD_W, CARD_H);
            CardArt.drawIcon(g, cards.get(i), xs[i], cardY, CARD_W);
        }
        int index = OverlapRow.hit(xs, CARD_W, CARD_H, cardY, mouseX, mouseY);
        return index >= 0 ? cards.get(index) : -1;
    }

    private int drawPicks(GuiGraphics g, int mouseX, int mouseY, Layout layout, List<CollectionView.Counted> picks) {
        int hit = -1;
        int shown = Math.min(layout.pickRows, Math.max(0, picks.size() - pickScroll));
        for (int i = 0; i < shown; i++) {
            int index = pickScroll + i;
            CollectionView.Counted card = picks.get(index);
            int x = layout.pickX;
            int y = layout.pickY + i * (PICK_H + 2);
            CollectionTextures.slot(g, x, y, PICK_W, PICK_H);
            CardArt.drawIcon(g, card.code(), x, y, PICK_W);
            if (card.count() > 1 || (view.unlimited() && card.count() == 0)) {
                String text = card.count() == 0 ? "*" : Integer.toString(card.count());
                g.drawString(font, text, x + PICK_W + 2, y + PICK_H - 10, 0xFFFFFFFF, true);
            }
            if (mouseX >= x && mouseY >= y && mouseX < x + PICK_W + 16 && mouseY < y + PICK_H) {
                hit = index;
            }
        }
        return hit;
    }

    private Drag dragAt(double mx, double my) {
        Layout layout = layout();
        List<CollectionView.Counted> picks = picks();
        int row = pickRow(layout, picks.size(), mx, my);
        if (row >= 0) {
            return new Drag(picks.get(row).code(), -1, -1);
        }
        int main = cardAtRows(layout.main, view.main().size(), layout.mainRows, mx, my);
        if (main >= 0) {
            return new Drag(view.main().get(main), 0, main);
        }
        int extra = cardAt(layout.extra, view.extra().size(), mx, my);
        if (extra >= 0) {
            return new Drag(view.extra().get(extra), 1, extra);
        }
        int side = cardAt(layout.side, view.side().size(), mx, my);
        if (side >= 0) {
            return new Drag(view.side().get(side), 2, side);
        }
        return null;
    }

    private int pickRow(Layout layout, int total, double mx, double my) {
        if (mx < layout.pickX || mx >= layout.pickX + PICK_W + 20) {
            return -1;
        }
        int local = (int) ((my - layout.pickY) / (PICK_H + 2));
        if (local < 0 || local >= layout.pickRows) {
            return -1;
        }
        int index = pickScroll + local;
        return index >= 0 && index < total ? index : -1;
    }

    private static int cardAtRows(Band band, int count, int maxRows, double mx, double my) {
        if (count <= 0) {
            return -1;
        }
        int per = perRow(band.w, count, maxRows);
        int rows = (count + per - 1) / per;
        for (int r = rows - 1; r >= 0; r--) {
            int start = r * per;
            int n = Math.min(per, count - start);
            int[] xs = OverlapRow.xs(band.x, band.w, CARD_W, n);
            int y = band.y + 11 + r * (CARD_H + 2);
            int hit = OverlapRow.hit(xs, CARD_W, CARD_H, y, mx, my);
            if (hit >= 0) {
                return start + hit;
            }
        }
        return -1;
    }

    /**
     * 一行按卡宽排开，排满就换行。可用行数用完之后，才把同一行挤紧重叠。
     */
    private static int perRow(int width, int count, int maxRows) {
        if (count <= 1) {
            return 1;
        }
        int span = Math.max(CARD_W, width);
        int step = CARD_W + 2;
        int fit = Math.max(1, 1 + Math.max(0, span - CARD_W) / step);
        int rows = Math.min(Math.max(1, maxRows), (count + fit - 1) / fit);
        return (count + rows - 1) / rows;
    }

    private static int cardAt(Band band, int count, double mx, double my) {
        int[] xs = OverlapRow.xs(band.x, band.w, CARD_W, count);
        return OverlapRow.hit(xs, CARD_W, CARD_H, band.y + 11, mx, my);
    }

    private int zoneAt(Layout layout, double mx, double my) {
        if (contains(layout.main, mx, my)) {
            return 0;
        }
        if (contains(layout.extra, mx, my)) {
            return 1;
        }
        if (contains(layout.side, mx, my)) {
            return 2;
        }
        return -1;
    }

    private static boolean contains(Band band, double mx, double my) {
        return mx >= band.x && my >= band.y && mx < band.x + band.w && my < band.y + band.h;
    }

    private List<CollectionView.Counted> picks() {
        String q = query();
        String key = (view.unlimited() ? "u:" : "o:") + q + ":" + view.supply().size() + ":" + pool.length;
        if (key.equals(pickKey)) {
            return pickCache;
        }
        ArrayList<CollectionView.Counted> out = new ArrayList<>();
        if (view.unlimited()) {
            for (int code : pool) {
                if (q.isEmpty() || matches(code, q)) {
                    out.add(new CollectionView.Counted(code, 0));
                }
            }
        } else if (q.isEmpty()) {
            out.addAll(view.supply());
        } else {
            for (CollectionView.Counted card : view.supply()) {
                if (matches(card.code(), q)) {
                    out.add(card);
                }
            }
        }
        pickCache = out;
        pickKey = key;
        return out;
    }

    private String query() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(int code, String query) {
        String name = DataPacks.get().nameOf(code);
        return name != null && name.toLowerCase(Locale.ROOT).contains(query);
    }

    private void add(int code, DeckMove.Zone zone) {
        int action = switch (zone) {
            case EXTRA -> CollectionNet.ADD_EXTRA;
            case SIDE -> CollectionNet.ADD_SIDE;
            case MAIN -> CollectionNet.ADD_MAIN;
        };
        CollectionClient.act(view.token(), action, code);
    }

    private void take(int zone, int index) {
        int action = switch (zone) {
            case 1 -> CollectionNet.TAKE_EXTRA;
            case 2 -> CollectionNet.TAKE_SIDE;
            default -> CollectionNet.TAKE_MAIN;
        };
        CollectionClient.act(view.token(), action, index);
    }

    /** 滚动条和介绍区共用选卡列的上下沿，滚到第几张，条上的滑块就停在对应位置。 */
    private void drawScroll(GuiGraphics g, Layout layout, int total) {
        int trackX = layout.infoX - 8;
        int trackY = layout.pickY;
        int trackH = layout.pickRows * (PICK_H + 2);
        g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x55FFFFFF);
        if (total <= layout.pickRows) {
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, 0xFFDDDDDD);
            return;
        }
        int thumbH = Math.max(8, trackH * layout.pickRows / total);
        int travel = Math.max(1, trackH - thumbH);
        int maxScroll = total - layout.pickRows;
        int thumbY = trackY + travel * pickScroll / maxScroll;
        g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFDDDDDD);
    }

    /** 可用卡贴在卡组右边。再往右空出来的区域写悬停卡的简要介绍。 */
    private void drawIntro(GuiGraphics g, Layout layout, int code) {
        int x = layout.infoX;
        int y = layout.pickY;
        int w = layout.infoW;
        if (w < 40) {
            return;
        }
        if (code < 0) {
            g.drawString(font, CollectionText.c(CollectionText.INTRO_EMPTY), x, y, 0xFFB0A090, false);
            return;
        }
        int artW = Math.min(72, w);
        int artH = artW * 290 / 200;
        CardArt.draw(g, code, x, y, artW, artH, null);
        int ty = y + artH + 6;
        String name = CardTips.name(code);
        g.drawString(font, font.plainSubstrByWidth(name == null ? "#" + code : name, w), x, ty, 0xFFFFE080, false);
        ty += 12;
        String stats = CardTips.statsLine(code);
        if (stats != null) {
            g.drawString(font, font.plainSubstrByWidth(stats, w), x, ty, 0xFFCCCCCC, false);
            ty += 12;
        }
        String desc = CardTips.desc(code);
        if (desc == null) {
            return;
        }
        for (String line : CardTips.wrap(font, desc, w)) {
            if (ty > height - 12) {
                break;
            }
            g.drawString(font, line, x, ty, 0xFFB0B0B0, false);
            ty += 10;
        }
    }

    private Layout layout() {
        int deckW = Math.min(280, Math.max(140, width - 230));
        int strip = 11 + CARD_H + 6;
        int sideY = Math.max(80, height - 8 - strip);
        int extraY = sideY - strip;
        int mainY = 22;
        int mainH = Math.max(11 + CARD_H, extraY - mainY - 4);
        int mainRows = Math.max(1, (mainH - 11) / (CARD_H + 2));
        Band main = new Band(6, mainY, deckW, mainH);
        Band extra = new Band(6, extraY, deckW, strip);
        Band side = new Band(6, sideY, deckW, strip);
        int pickX = 6 + deckW + 8;
        int infoX = pickX + PICK_W + 18;
        int infoW = Math.max(0, width - infoX - 6);
        int pickY = 22;
        int pickRows = Math.max(1, (height - pickY - 8) / (PICK_H + 2));
        return new Layout(main, extra, side, pickX, pickY, pickRows, mainRows, infoX, infoW);
    }

    private static DeckMove.Zone zoneOf(int zone) {
        return switch (zone) {
            case 1 -> DeckMove.Zone.EXTRA;
            case 2 -> DeckMove.Zone.SIDE;
            default -> DeckMove.Zone.MAIN;
        };
    }

    private static boolean extraOnly(int code) {
        CardDataDb db = DataPacks.get().cardData();
        CardDataDb.Stats stats = db == null ? null : db.stats(code);
        return stats != null && (stats.type() & DeckMove.EXTRA_ONLY) != 0;
    }

    private record Band(int x, int y, int w, int h) {
    }

    private record Layout(Band main, Band extra, Band side, int pickX, int pickY, int pickRows, int mainRows,
                          int infoX, int infoW) {
    }

    private record Drag(int code, int fromZone, int fromIndex) {
    }
}
