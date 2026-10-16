# Yu-Gi-Oh in Minecraft（ygomc）

在 Minecraft 里打游戏王。规则由 [ygopro-core](https://github.com/Fluorohydride/ygopro-core)（ocgcore，新大师规则）驱动，界面是这个模组自己画的。

Minecraft **1.21.1**，Architectury（Fabric 与 NeoForge 同一份公共代码）。许可 **GPL-3.0-or-later**。原生库提供 **Windows x64**、**Linux x64**、**macOS x64** 与 **macOS arm64**。Linux arm64 的目录加载器认，发布包里没有；`natives/build.sh` 也编不了它，因为用的 premake5 没有 arm64 官方包。

卡图、卡文和卡片脚本的版权属于 KONAMI 与各脚本作者，**不随模组分发**。需要自己准备数据，见下文。

## 玩

游戏目录（版本文件夹）下要有这三处，缺一处对应的功能就起不来：

| 路径 | 内容 |
|---|---|
| `ygomc/datapack/` | `cards.bin`、`texts.bin`、`strings.bin`、`pics.bin` |
| `ygomc/script/` | ocgcore 的 Lua 脚本（`c*.lua` 以及 `constant.lua` 等） |
| `ygomc/decks/` | `.ydk` 卡组 -可选|

数据包用仓库里的 `tools/mkdatapack.py`、`tools/mkpics.py` 从本地的 `cards.cdb` 和卡图生成，产物放进 `ygomc/datapack/`。生成物体积大，而且含版权素材，所以不进行公开分发

进游戏后：

- `/ygomc duel [卡组]` 开始单人对局，你操作一席，另一席由本地贪心 AI 应答。
- `/ygomc selftest [卡组]` 让双方自动应答并观战。
- `/ygomc duel deck <卡组>`、`/ygomc duel invite <玩家>`、`/ygomc duel accept` 开双人局。
- `/ygomc duel abort` 中止自己这一局。超时默认 100 秒判负，不代答。
- `/ygomc deck list` 列出 `ygomc/decks/` 里的卡组。
- 手持决斗盘对空气右键：回到当前对局界面。
- 右键卡册：浏览卡图。

决斗界面里，左上角那颗键切换连锁时点（忽略 / 显示 / 可用），默认是「可用时点」。

## 构建

需要 JDK 21。先编出当前系统的原生库，否则模组能进游戏，但开不了局。Windows 用 `natives/build-jni.ps1`（要先按 `natives/vendor/VENDOR.md` 编出 `ocgcore.dll`）。Linux 与 macOS 用 `natives/build.sh`，它会下载 premake5 并编出内核和 JNI 桥。

本地只编当前系统。GitHub Actions（`.github/workflows/natives.yml`）在 Windows、Linux 和两套 macOS 上各编一份，再打进同一个 jar。那份发布包拿到 Linux 服务端或 Mac 上可以直接开局，不用再编原生库。

```
# Windows
gradlew.bat :neoforge:build
gradlew.bat :fabric:build

# Linux / macOS
./gradlew :neoforge:build
./gradlew :fabric:build
```

产物在 `neoforge/build/libs/` 和 `fabric/build/libs/`。运行还依赖 Cloth Config 与 Architectury。

## 场地贴图

决斗界面的空槽和牌垫在 `common/src/main/resources/assets/ygomc/textures/gui/duel/`。用同名 PNG 覆盖即可，说明写在那个目录的 `README.txt` 里。
