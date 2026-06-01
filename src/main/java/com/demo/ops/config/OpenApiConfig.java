package com.demo.ops.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 接口文档配置类。
 * 配置 API 文档的标题、描述和版本信息，供 Swagger UI 渲染使用。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 文档 Bean。
     * 定义 API 的标题、描述、版本号和联系人信息。
     */
    @Bean
    public OpenAPI maintenanceDecisionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Maintenance Decision Service API")
                .description("API for aircraft maintenance and equipment maintenance workflows.")
                .version("v1")
                .contact(new Contact().name("Codex Demo Generator")));
    }
}
