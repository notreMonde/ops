package com.demo.ops.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON 资源文件读取服务。
 * 从 classpath 下的 datasets 目录中加载 JSON 文件并解析为 JsonNode 对象。
 * 使用 ConcurrentHashMap 缓存已加载的文件，避免重复读取 I/O。
 */
@Service
public class JsonResourceService {

    private final ObjectMapper objectMapper;

    /** 资源缓存，key 为资源路径，value 为解析后的 JsonNode */
    private final Map<String, JsonNode> cache = new ConcurrentHashMap<>();

    public JsonResourceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 读取指定路径的 JSON 资源文件。
     * 优先从缓存中获取，缓存未命中则从 classpath 加载并缓存。
     * 返回深度拷贝后的副本，避免调用方修改缓存内容。
     */
    public JsonNode read(String resourcePath) {
        JsonNode cached = cache.computeIfAbsent(resourcePath, this::loadResource);
        return cached.deepCopy();
    }

    /**
     * 读取 JSON 资源并以 ObjectNode 形式返回。
     * 适用于顶层为 JSON 对象的数据文件。
     */
    public ObjectNode readObject(String resourcePath) {
        return (ObjectNode) read(resourcePath);
    }

    /**
     * 从 classpath 加载 JSON 资源文件并解析为 JsonNode。
     * 使用 try-with-resources 确保 InputStream 正确关闭。
     */
    private JsonNode loadResource(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load resource: " + resourcePath, exception);
        }
    }
}
