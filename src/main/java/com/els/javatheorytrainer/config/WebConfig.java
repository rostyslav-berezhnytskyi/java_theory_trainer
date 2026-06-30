package com.els.javatheorytrainer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC configuration.
 *
 * Makes uploaded files available in browser by URLs like:
 * /uploads/questions/1/image.png
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadsRoot;

    public WebConfig(@Value("${app.uploads.root}") String uploadsRoot) {
        this.uploadsRoot = Paths.get(uploadsRoot).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsRoot.toUri().toString());
    }
}
