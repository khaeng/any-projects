package com.itcall.base.view.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class CommonResourceConfig implements WebMvcConfigurer {

    @Value("${external.resource.path:./}")
    private String externalPath;

    private final ThymeleafProperties thymeleafProperties;

    // Standard Spring Boot static resource locations
    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
            "classpath:/META-INF/resources/", "classpath:/resources/",
            "classpath:/static/", "classpath:/public/" };

    public CommonResourceConfig(ThymeleafProperties thymeleafProperties) {
        this.thymeleafProperties = thymeleafProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = externalPath.endsWith("/") ? externalPath : externalPath + "/";

        // Add handler for static resources: File System (Priority) -> Classpath
        // (Fallback)
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + path + "static/")
                .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
    }

    @Bean
    public ITemplateResolver fileTemplateResolver() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        String path = externalPath.endsWith("/") ? externalPath : externalPath + "/";

        // Configure file-based resolver to override classpath templates
        resolver.setPrefix("file:" + path + "templates/");
        resolver.setSuffix(thymeleafProperties.getSuffix());
        resolver.setTemplateMode(thymeleafProperties.getMode());
        if (thymeleafProperties.getEncoding() != null) {
            resolver.setCharacterEncoding(thymeleafProperties.getEncoding().name());
        }
        resolver.setCacheable(thymeleafProperties.isCache());
        resolver.setCheckExistence(true); // Crucial for fallback
        resolver.setOrder(1); // Higher priority than default Classpath resolver

        return resolver;
    }
}
