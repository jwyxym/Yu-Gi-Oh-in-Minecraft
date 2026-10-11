package cn.xm1221.ygomc.common.client;

import cn.xm1221.ygomc.common.collection.CollectionView;
import cn.xm1221.ygomc.common.net.CollectionNet;
import net.minecraft.client.Minecraft;

/**
 * 把组卡快照接到界面上。只在客户端初始化里调用，服务端不加载这个类。
 */
public final class CollectionClient {

    private CollectionClient() {
    }

    public static void init() {
        CollectionNet.registerClient(CollectionClient::onView);
    }

    static void act(int token, int action, int arg) {
        act(token, action, arg, 0);
    }

    static void act(int token, int action, int arg, int arg2) {
        act(token, action, arg, arg2, 0);
    }

    static void act(int token, int action, int arg, int arg2, int arg3) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        CollectionNet.sendAct(token, action, arg, arg2, arg3, mc.level.registryAccess());
    }

    private static void onView(CollectionView view) {
        Minecraft mc = Minecraft.getInstance();
        if (!view.open()) {
            if (mc.screen instanceof DeckEditorScreen || mc.screen instanceof BinderScreen
                    || mc.screen instanceof BinderSortScreen) {
                mc.setScreen(null);
            }
            return;
        }
        if (view.screen() == CollectionView.SCREEN_DECK) {
            if (mc.screen instanceof DeckEditorScreen screen && screen.token() == view.token()) {
                screen.apply(view);
            } else {
                mc.setScreen(new DeckEditorScreen(view));
            }
            return;
        }
        if (view.screen() == CollectionView.SCREEN_BINDER) {
            if (mc.screen instanceof BinderSortScreen screen && screen.token() == view.token()) {
                screen.apply(view);
            } else if (mc.screen instanceof BinderScreen screen && screen.token() == view.token()) {
                screen.apply(view);
            } else {
                mc.setScreen(new BinderScreen(view));
            }
        }
    }
}
