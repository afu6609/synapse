package com.synapse.embedding.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 绕过 GraalVM Native Image 中 System.load(absolutePath) 失败的问题。
 * 当 DJL HuggingFace LibUtils 试图从缓存加载提取出的绝对路径 DLL 时，
 * 本助手接管加载过程，改为仅通过文件名加载，依赖 OS 的默认 DLL 搜索路径（包含当前执行目录）。
 */
public class NativeHelper {
    private static final Logger log = LoggerFactory.getLogger(NativeHelper.class);

    public static void load(String path) {
        log.info("DJL NativeHelper intercepting load for path: {}", path);

        File file = new File(path);
        String name = file.getName();

        // 移除扩展名和前缀（Windows 上的 tokenizers.dll 不带 lib，而 Linux 带有 lib 前缀等）
        if (name.endsWith(".dll")) {
            name = name.substring(0, name.length() - 4);
        } else if (name.endsWith(".so")) {
            name = name.substring(0, name.length() - 3);
            if (name.startsWith("lib")) {
                name = name.substring(3);
            }
        } else if (name.endsWith(".dylib")) {
            name = name.substring(0, name.length() - 6);
            if (name.startsWith("lib")) {
                name = name.substring(3);
            }
        }

        // 我们已经在 pom.xml 中将所有这几个 DLL 打包到 exe 同目录（或用户手动放置在同目录）
        // 在 Windows 和各种 OS 上，System.loadLibrary 都会在当前执行目录（或者 PATH）搜索
        try {
            System.loadLibrary(name);
            log.info("Successfully loaded library via System.loadLibrary: {}", name);
        } catch (UnsatisfiedLinkError e) {
            log.warn("Failed to load library '{}' via loadLibrary. Message: {}", name, e.getMessage());
            // 如果是 tokenizers 核心库加载失败，必须要抛出异常阻止程序错误运行
            if ("tokenizers".equals(name) || "djl_tokenizer".equals(name)) {
                log.error("CRITICAL: 无法加载核心库 {}, 请确保该 DLL 在程序根目录下。", name);
                throw e;
            }
            // 对于 libwinpthread-1 等依赖库，System.loadLibrary 可能会找不到，
            // 但是如果 tokenizers.dll 在加载时由 Windows Loader 自动解析到同目录的依赖，它其实是能加载的。
            // 因此这块的 UnsatisfiedLinkError 我们捕获并静默忽略，不让 DJL 初始化中断。
        }
    }
}
