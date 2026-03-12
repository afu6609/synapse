package com.synapse.embedding.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将 /admin 和 /admin/ 重定向到 /admin/index.html
        registry.addViewController("/admin").setViewName("redirect:/admin/index.html");
        registry.addViewController("/admin/").setViewName("redirect:/admin/index.html");
    }
}
