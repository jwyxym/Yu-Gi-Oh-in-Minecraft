# Yu-Gi-Oh in Minecraft（ygomc）

在 Minecraft 里打游戏王。规则由 [ygopro-core](https://github.com/Fluorohydride/ygopro-core)（ocgcore，新大师规则）驱动，界面是这个模组自己画的。

Minecraft **1.21.1**，Architectury（Fabric 与 NeoForge 同一份公共代码）。许可 **GPL-3.0-or-later**。原生库目前只提供 **Windows x64**。

卡图、卡文和卡片脚本的版权属于 KONAMI 与各脚本作者，**不随模组分发**。需要自己准备数据，见下文。

## 玩

游戏目录（版本文件夹）下要有这三处，缺一处对应的功能就起不来：

| 路径 | 内容 |
|---|---|
| `ygomc/datapack/` | `cards.bin`、`texts.bin`、`strings.bin`、`pics.bin` |
| `ygomc/script/` | ocgcore 的 Lua 脚本（`c*.lua` 以及 `constant.lua` 等） |
| `ygomc/decks/` | `.ydk` 卡组 |

数据包用仓库里的 `tools/mkdatapack.py`、`tools/mkpics.py` 从本地的 `cards.cdb` 和卡图生成，产物放进 `ygomc/datapack/`。生成物体积大，而且含版权素材，不要提交进 git。

进游戏后：

- `/ygomc duel` 用人机开一局。后面可以跟卡组名。
- `/ygomc duel deck <卡组>`、`/ygomc duel invite <玩家>`、`/ygomc duel accept` 开双人局。
- `/ygomc duel abort` 中止自己这一局。超时默认 100 秒判负，不代答。
- `/ygomc deck list` 列出 `ygomc/decks/` 里的卡组。
- 手持决斗盘对空气右键：回到当前对局界面。
- 右键卡册：浏览卡图。

决斗界面里，左上角那颗键切换连锁时点（忽略 / 显示 / 可用），默认是「可用时点」。

## 构建

需要 JDK 21。Windows 上还要先编出两个 DLL（见 `natives/build-jni.ps1`），否则模组能进游戏，但开不了局。

```
gradlew.bat :neoforge:build
gradlew.bat :fabric:build
```

产物在 `neoforge/build/libs/` 和 `fabric/build/libs/`。运行还依赖 Cloth Config 与 Architectury。

## 场地贴图

决斗界面的空槽和牌垫在 `common/src/main/resources/assets/ygomc/textures/gui/duel/`。用同名 PNG 覆盖即可，说明写在那个目录的 `README.txt` 里。
