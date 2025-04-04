package kr.cocoh.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS 설정 (SecurityConfig에서도 설정했지만 여기서도 추가)
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI 리소스 핸들링
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/4.15.5/");
        
        // 정적 리소스 핸들링 (이미지, CSS, JS 등)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 파일 업로드 디렉토리 접근 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}