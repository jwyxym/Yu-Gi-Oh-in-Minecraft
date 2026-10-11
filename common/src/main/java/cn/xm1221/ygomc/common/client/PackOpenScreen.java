package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.pack.model.PrintRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 开包结果。卡在打开这个界面之前已经进了背包，关掉它不会收回任何东西，
 * 界面里也没有「留下 / 丢弃」。
 *
 * <p>从左到右按稀有度从低到高排。「全部翻开」也按这个顺序，一张一张来，
 * 最高的一张最后翻。点某一张则只翻那一张。
 */
public final class PackOpenScreen extends Screen {

    private static final int GAP = 8;
    private static final int FLIP_INTERVAL = 4;

    private final List<PrintRef> cards;
    private final boolean[] faceUp;
    private Button reveal;
    private boolean revealing;
    private int tick;

    public PackOpenScreen(List<PrintRef> cards) {
        super(Component.translatable("ygomc.pack.screen"));
        this.cards = new ArrayList<>(cards);
        this.cards.sort(Comparator.comparingInt((PrintRef card) -> card.rarity().ordinal())
                .thenComparingInt(PrintRef::code));
        this.faceUp = new boolean[this.cards.size()];
    }

    @Override
    protected void init() {
        reveal = Button.builder(Component.translatable("ygomc.pack.reveal_all"), button -> {
            if (allFaceUp()) {
                onClose();
            } else {
                revealing = true;
            }
        }).bounds(width / 2 - 60, height - 28, 120, 20).build();
        addRenderableWidget(reveal);
    }

    @Override
    public void tick() {
        if (!revealing) {
            return;
        }
        tick++;
        if (tick % FLIP_INTERVAL != 0) {
            return;
        }
        for (int i = 0; i < faceUp.length; i++) {
            if (!faceUp[i]) {
                faceUp[i] = true;
                break;
            }
        }
        if (allFaceUp()) {
            revealing = false;
            reveal.setMessage(Component.translatable("ygomc.pack.close"));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int[] layout = layout();
            int cardW = layout[0];
            int cardH = layout[1];
            int x0 = layout[2];
            int y0 = layout[3];
            int columns = layout[4];
            for (int i = 0; i < cards.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = x0 + col * (cardW + GAP);
                int y = y0 + row * (cardH + GAP);
                if (mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH) {
                    faceUp[i] = true;
                    if (allFaceUp()) {
                        revealing = false;
                        reveal.setMessage(Component.translatable("ygomc.pack.close"));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PackTextures.background(g, 0, 0, width, height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(font, title, (width - font.width(title)) / 2, 12, 0xFFF0E2B0, true);
        int[] layout = layout();
        int cardW = layout[0];
        int cardH = layout[1];
        int x0 = layout[2];
        int y0 = layout[3];
        int columns = layout[4];
        for (int i = 0; i < cards.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = x0 + col * (cardW + GAP);
            int y = y0 + row * (cardH + GAP);
            PackTextures.slot(g, x - 2, y - 2, cardW + 4, cardH + 4);
            PrintRef card = cards.get(i);
            if (faceUp[i]) {
                CardArt.draw(g, card.code(), x, y, cardW, cardH, RarityLooks.of(card.rarity()));
            } else {
                CardArt.drawBack(g, x, y, cardW, cardH);
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (minecraft != null) {
            minecraft.tell(PackClient::openPending);
        }
    }

    /** 卡宽、卡高、起点 x、起点 y、每行张数。 */
    private int[] layout() {
        int columns = Math.max(1, Math.min(cards.size(), 5));
        int rows = Math.max(1, (cards.size() + columns - 1) / columns);
        int availW = width - 24 - GAP * (columns - 1);
        int availH = height - 64 - GAP * (rows - 1);
        int cardW = Math.min(96, availW / columns);
        int cardH = cardW * 290 / 200;
        if (cardH * rows + GAP * (rows - 1) > height - 64) {
            cardH = Math.max(48, availH / rows);
            cardW = cardH * 200 / 290;
        }
        int gridW = columns * cardW + (columns - 1) * GAP;
        int gridH = rows * cardH + (rows - 1) * GAP;
        int x0 = (width - gridW) / 2;
        int y0 = 32 + Math.max(0, (height - 64 - gridH) / 2);
        return new int[]{cardW, cardH, x0, y0, columns};
    }

    private boolean allFaceUp() {
        for (boolean up : faceUp) {
            if (!up) {
                return false;
            }
        }
        return true;
    }
}
