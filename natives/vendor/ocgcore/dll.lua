LUA_DIR = "./lua"

newoption { trigger = "lua-dir", description = "", value = "PATH" }

function GetParam(param)
    return _OPTIONS[param] or os.getenv(string.upper(string.gsub(param,"-","_")))
end

LUA_DIR = GetParam("lua-dir") or LUA_DIR
if not os.isdir(LUA_DIR) then
    LUA_DIR = "../lua"
end

workspace "ocgcoredll"
    location "build"
    language "C++"
    -- gmake/clang 默认可能是 C++98；ocgcore 头文件用了 constexpr，需要至少 C++14。
    filter "not action:vs*"
        cppdialect "C++17"
    filter {}

    configurations { "Release", "Debug" }
    platforms { "x32", "x64", "arm64" }

    filter "platforms:x32"
        architecture "x32"

    filter "platforms:x64"
        architecture "x64"

    -- Premake 5 里 ARM64 的架构名是 "ARM64"；显式带 -arch arm64 让 clang 真的产出 arm64，
    -- 否则 Apple Silicon 上会默认按 host 架构编，和 JNI 桥链接时架构不匹配。
    filter "platforms:arm64"
        architecture "ARM64"

    filter { "platforms:arm64", "system:macosx" }
        buildoptions { "-arch arm64" }
        linkoptions { "-arch arm64" }

    filter "configurations:Release"
        optimize "Speed"

    filter "configurations:Debug"
        symbols "On"
        defines "_DEBUG"

    filter "system:windows"
        systemversion "latest"
        startproject "ocgcore"

    filter { "configurations:Release", "action:vs*" }
        linktimeoptimization "On"
        staticruntime "On"
        disablewarnings { "4334" }

    filter "action:vs*"
        cdialect "C11"
        conformancemode "On"
        buildoptions { "/utf-8" }
        defines { "_CRT_SECURE_NO_WARNINGS" }

    filter "system:bsd"
        defines { "LUA_USE_POSIX" }

    filter "system:macosx"
        defines { "LUA_USE_MACOSX" }

    filter "system:linux"
        defines { "LUA_USE_LINUX" }
        pic "On"

filter {}

include(LUA_DIR)

project "ocgcore"

    kind "SharedLib"

    defines { "OCGCORE_EXPORT_FUNCTIONS" }
    files { "*.cpp", "*.h" }
    links { "lua" }
    
    includedirs { LUA_DIR .. "/src" }
