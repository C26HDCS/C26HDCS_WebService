package kr.kwater.hdcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.DefaultPaginationManager;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationRenderer;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.DefaultPaginationRenderer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EgovConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 필요 시 eGov 공통 인터셉터 등록
    }

    @Bean
    public DefaultPaginationManager paginationManager() {
        Map<String, PaginationRenderer> rendererMap = new HashMap<>();
        rendererMap.put("default", new DefaultPaginationRenderer());

        DefaultPaginationManager manager = new DefaultPaginationManager();
        manager.setRendererType(rendererMap);
        return manager;
    }

}
