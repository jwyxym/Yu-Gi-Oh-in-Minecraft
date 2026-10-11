package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.net.PackNet;
import cn.xm1221.ygomc.common.pack.model.PrintRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.List;

/**
 * 开包结果送到界面上。界面开着时先排队，关掉当前这一包再看下一包。
 */
public final class PackClient {

    private static final ArrayDeque<List<PrintRef>> PENDING = new ArrayDeque<>();

    private PackClient() {
    }

    public static void init() {
        PackTips.shiftDown = Screen::hasShiftDown;
        PackNet.registerClient(PackClient::accept);
    }

    private static void accept(List<PrintRef> cards) {
        if (cards.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof PackOpenScreen) {
            PENDING.add(List.copyOf(cards));
        } else {
            minecraft.setScreen(new PackOpenScreen(cards));
        }
    }

    /** 当前界面关掉之后再打开排队的下一包。已经有别的界面时不动。 */
    public static void openPending() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || PENDING.isEmpty()) {
            return;
        }
        minecraft.setScreen(new PackOpenScreen(PENDING.removeFirst()));
    }
}
