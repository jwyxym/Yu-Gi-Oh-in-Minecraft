#!/usr/bin/env bash
# 构建当前系统的 ocgcore 动态库和 JNI 桥。
#
# 产物落到 natives/dist/<平台>/，Gradle 从那里打进 jar。
# Windows 不走这个脚本，继续用 natives/build-jni.ps1。
#
#   ./natives/build.sh
set -euo pipefail

NATIVES="$(cd "$(dirname "$0")" && pwd)"
CORE_SRC="$NATIVES/vendor/ocgcore"
JNI_SRC="$NATIVES/jni/ygomc_ocg.cpp"
CACHE="$NATIVES/.cache"

PREMAKE_VERSION="5.0.0-beta8"
PREMAKE_BASE="https://github.com/premake/premake-core/releases/download/v${PREMAKE_VERSION}"

os="$(uname -s)"
machine="$(uname -m)"
case "$os:$machine" in
    Linux:x86_64)
        PLATFORM=linux-x64
        # 官方 linux 二进制要 GLIBC_2.38，ubuntu-22.04（为了兼容旧服务端）跑不了，改从源码 bootstrap。
        PREMAKE_FROM_SOURCE=1
        CORE_NAME=libocgcore.so
        BRIDGE_NAME=libygomc_ocg.so
        JNI_INC=linux
        ;;
    Linux:aarch64)
        # 加载器和 Gradle 认 natives/dist/linux-arm64，但 premake 5.0.0-beta8
        # 没有 Linux arm64 官方包，这里编不了。
        echo "linux-arm64 的库可以放到 natives/dist/linux-arm64/ 再打包，但本脚本编不了：premake ${PREMAKE_VERSION} 没有 arm64 构建。" >&2
        exit 1
        ;;
    Darwin:x86_64)
        PLATFORM=osx-x64
        PREMAKE_FROM_SOURCE=0
        PREMAKE_ARCHIVE="premake-5.0.0-beta8-macosx-x64.tar.gz"
        PREMAKE_SHA256="84b5fa5a432dcebdc3dd12e8677d10e38e5b32a3fe06d83ae68967e4f5e2db8a"
        CORE_NAME=libocgcore.dylib
        BRIDGE_NAME=libygomc_ocg.dylib
        JNI_INC=darwin
        ;;
    Darwin:arm64)
        PLATFORM=osx-arm64
        PREMAKE_FROM_SOURCE=0
        PREMAKE_ARCHIVE="premake-5.0.0-beta8-macosx.tar.gz"
        PREMAKE_SHA256="fa73a46f093fa6f17494a3d063421aa6cae3ea825a61c62dd59fc2f07a256d03"
        CORE_NAME=libocgcore.dylib
        BRIDGE_NAME=libygomc_ocg.dylib
        JNI_INC=darwin
        ;;
    *)
        echo "natives/build.sh 只构建 Linux 与 macOS，当前是 $os/$machine。Windows 请用 natives/build-jni.ps1。" >&2
        exit 1
        ;;
esac

if [[ -n "${JAVA_HOME:-}" && -f "$JAVA_HOME/include/jni.h" ]]; then
    JDK="$JAVA_HOME"
else
    java_bin="$(command -v java || true)"
    if [[ -z "$java_bin" ]]; then
        echo "找不到 java，请安装 JDK 21 并设置 JAVA_HOME。" >&2
        exit 1
    fi
    java_bin="$(readlink -f "$java_bin" 2>/dev/null || python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$java_bin")"
    JDK="$(cd "$(dirname "$java_bin")/.." && pwd)"
fi
if [[ ! -f "$JDK/include/jni.h" ]]; then
    echo "JDK 目录里没有 include/jni.h: $JDK" >&2
    exit 1
fi
if [[ ! -d "$JDK/include/$JNI_INC" ]]; then
    echo "JDK 目录里没有 include/$JNI_INC: $JDK" >&2
    exit 1
fi

jobs="$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 2)"
if command -v gmake >/dev/null 2>&1; then
    MAKE=gmake
else
    MAKE=make
fi

sha256_check() {
    local expect="$1" file="$2"
    if command -v sha256sum >/dev/null 2>&1; then
        printf '%s  %s\n' "$expect" "$file" | sha256sum -c -
    else
        printf '%s  %s\n' "$expect" "$file" | shasum -a 256 -c -
    fi
}

mkdir -p "$CACHE"
PREMAKE="$CACHE/premake5"

# 官方 linux 二进制依赖 GLIBC_2.38，在 ubuntu-22.04 上无法运行；Linux 改为源码 bootstrap。
# 源码包：https://github.com/premake/premake-core/archive/refs/tags/v5.0.0-beta8.tar.gz
PREMAKE_SRC_ARCHIVE="premake-core-${PREMAKE_VERSION}.tar.gz"
PREMAKE_SRC_URL="https://github.com/premake/premake-core/archive/refs/tags/v${PREMAKE_VERSION}.tar.gz"
PREMAKE_SRC_SHA256="2a55195fd2b27e5aa3de8ff6d22cdb127232a86f801d06e7f673d30a0eba09ac"

ensure_premake() {
    if [[ -x "$PREMAKE" ]] && "$PREMAKE" --version >/dev/null 2>&1; then
        return 0
    fi
    rm -f "$PREMAKE"

    if [[ "${PREMAKE_FROM_SOURCE:-0}" == "1" ]]; then
        echo "从源码构建 premake $PREMAKE_VERSION（避免官方 linux 二进制要更新的 glibc）..."
        src_archive="$CACHE/$PREMAKE_SRC_ARCHIVE"
        if [[ ! -f "$src_archive" ]] || ! sha256_check "$PREMAKE_SRC_SHA256" "$src_archive" >/dev/null 2>&1; then
            curl -fsSL "$PREMAKE_SRC_URL" -o "$src_archive.partial"
            mv "$src_archive.partial" "$src_archive"
            sha256_check "$PREMAKE_SRC_SHA256" "$src_archive"
        fi
        src_dir="$CACHE/premake-core-${PREMAKE_VERSION}"
        rm -rf "$src_dir"
        tar -C "$CACHE" -xzf "$src_archive"
        # GitHub archive 解出 premake-core-5.0.0-beta8/
        if [[ ! -d "$src_dir" ]]; then
            # 兜底：找解压出来的唯一目录
            src_dir="$(find "$CACHE" -maxdepth 1 -type d -name 'premake-core-*' | head -n 1)"
        fi
        # Bootstrap.mak linux 链接 -luuid；ubuntu-22.04 runner 默认没有头文件。
        if [[ ! -f /usr/include/uuid/uuid.h ]]; then
            echo "安装 uuid 开发头（premake Bootstrap 需要）..."
            if command -v apt-get >/dev/null 2>&1; then
                sudo apt-get update
                sudo apt-get install -y uuid-dev
            else
                echo "找不到 /usr/include/uuid/uuid.h，且无法用 apt 安装 uuid-dev" >&2
                exit 1
            fi
        fi
        (
            cd "$src_dir"
            # Bootstrap 产物在 bin/release/premake5
            make -f Bootstrap.mak linux CONFIG=release
        )
        cp -f "$src_dir/bin/release/premake5" "$PREMAKE"
        chmod +x "$PREMAKE"
    else
        echo "下载 premake $PREMAKE_VERSION ..."
        archive="$CACHE/$PREMAKE_ARCHIVE"
        if [[ ! -f "$archive" ]] || ! sha256_check "$PREMAKE_SHA256" "$archive" >/dev/null 2>&1; then
            curl -fsSL "$PREMAKE_BASE/$PREMAKE_ARCHIVE" -o "$archive.partial"
            mv "$archive.partial" "$archive"
            sha256_check "$PREMAKE_SHA256" "$archive"
        fi
        tar -C "$CACHE" -xzf "$archive"
        chmod +x "$PREMAKE"
    fi

    if ! "$PREMAKE" --version >/dev/null 2>&1; then
        echo "premake 无法运行: $PREMAKE" >&2
        "$PREMAKE" --version >&2 || true
        exit 1
    fi
}

ensure_premake

echo "平台       : $PLATFORM"
echo "JDK        : $JDK"
echo "premake    : $PREMAKE"

echo "生成 ocgcore makefile ..."
( cd "$CORE_SRC" && "$PREMAKE" gmake2 --file=dll.lua )

echo "编译 ocgcore ..."
# 静态链 libstdc++/libgcc，这样只装了 JRE 的 Linux 服务端不用再装 g++ 运行库。
# macOS 用系统 libc++，不静态链接。
# dll.lua / 部分 gmake 配置不会带上标准，显式强制 C++17（修 osx-arm64 constexpr）。
CXXFLAGS_OCG="${CXXFLAGS:-} -std=c++17"
if [[ "$os" == Linux ]]; then
    "$MAKE" -C "$CORE_SRC/build" -j"$jobs" config=release_x64 \
        CXXFLAGS="$CXXFLAGS_OCG" \
        LDFLAGS="-static-libstdc++ -static-libgcc"
else
    "$MAKE" -C "$CORE_SRC/build" -j"$jobs" config=release_x64 \
        CXXFLAGS="$CXXFLAGS_OCG"
fi

core_built="$(find "$CORE_SRC/build" -type f -name "$CORE_NAME" | head -n 1)"
if [[ -z "$core_built" ]]; then
    echo "ocgcore 编译结束但没有找到 $CORE_NAME" >&2
    exit 1
fi

DIST="$NATIVES/dist/$PLATFORM"
mkdir -p "$DIST"
cp -f "$core_built" "$DIST/$CORE_NAME"

if [[ "$os" == Darwin ]]; then
    install_name_tool -id "@rpath/$CORE_NAME" "$DIST/$CORE_NAME"
fi

echo "编译 JNI 桥 ..."
# Linux 上 /usr/bin/c++ 有时指向没配好 C++ 头文件的 clang。没指定 CXX 时用 g++。
if [[ -z "${CXX:-}" ]]; then
    if [[ "$os" == Linux ]] && command -v g++ >/dev/null 2>&1; then
        CXX=g++
    else
        CXX=c++
    fi
fi
common_flags=(
    -fPIC -std=c++17 -O2 -DNDEBUG -fvisibility=hidden
    -I"$CORE_SRC" -I"$JDK/include" -I"$JDK/include/$JNI_INC"
    "$JNI_SRC"
    -L"$DIST" -locgcore
)
if [[ "$os" == Linux ]]; then
    "$CXX" -shared "${common_flags[@]}" \
        -pthread \
        -static-libstdc++ -static-libgcc \
        -Wl,-rpath,'$ORIGIN' \
        -Wl,-soname,"$BRIDGE_NAME" \
        -o "$DIST/$BRIDGE_NAME"
else
    "$CXX" -dynamiclib "${common_flags[@]}" \
        -mmacosx-version-min=11.0 \
        -Wl,-rpath,@loader_path \
        -Wl,-install_name,"@rpath/$BRIDGE_NAME" \
        -o "$DIST/$BRIDGE_NAME"
    install_name_tool -change "$core_built" "@rpath/$CORE_NAME" "$DIST/$BRIDGE_NAME" 2>/dev/null || true
    install_name_tool -change "@rpath/$CORE_NAME" "@rpath/$CORE_NAME" "$DIST/$BRIDGE_NAME" 2>/dev/null || true
fi

echo "检查导出与依赖 ..."
# 不用「nm | grep -q」：pipefail 下 grep 提前退出会让 nm 收到 SIGPIPE，整条管道被当成失败。
if [[ "$os" == Linux ]]; then
    if ! nm -D "$DIST/$BRIDGE_NAME" | grep 'JNI_OnLoad' >/dev/null; then
        echo "JNI 桥没有导出 JNI_OnLoad" >&2
        exit 1
    fi
    echo "--- ldd $BRIDGE_NAME ---"
    bridge_deps="$(ldd "$DIST/$BRIDGE_NAME")"
    printf '%s\n' "$bridge_deps"
    if ! printf '%s\n' "$bridge_deps" | grep 'libocgcore\.so =>' >/dev/null; then
        echo "JNI 桥没有解析到同目录的 libocgcore.so" >&2
        exit 1
    fi
    if printf '%s\n' "$bridge_deps" | grep 'not found' >/dev/null; then
        echo "JNI 桥有未解析的依赖" >&2
        exit 1
    fi
    echo "--- ldd $CORE_NAME ---"
    core_deps="$(ldd "$DIST/$CORE_NAME")"
    printf '%s\n' "$core_deps"
    if printf '%s\n' "$core_deps" | grep 'not found' >/dev/null; then
        echo "ocgcore 有未解析的依赖" >&2
        exit 1
    fi
else
    if ! nm -gU "$DIST/$BRIDGE_NAME" | grep 'JNI_OnLoad' >/dev/null; then
        echo "JNI 桥没有导出 JNI_OnLoad" >&2
        exit 1
    fi
    echo "--- otool -L $BRIDGE_NAME ---"
    bridge_deps="$(otool -L "$DIST/$BRIDGE_NAME")"
    printf '%s\n' "$bridge_deps"
    if ! printf '%s\n' "$bridge_deps" | grep "@rpath/$CORE_NAME" >/dev/null; then
        echo "JNI 桥没有以 @rpath 依赖 $CORE_NAME" >&2
        exit 1
    fi
fi

echo "构建成功: $DIST"
ls -l "$DIST/$CORE_NAME" "$DIST/$BRIDGE_NAME"
