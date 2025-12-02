package com.example.HotelBooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình: Bất cứ request nào bắt đầu bằng /rooms/** // sẽ được tìm trong thư mục E:/hotel_booking_data/rooms/
        registry.addResourceHandler("/rooms/**")
                .addResourceLocations("file:///E:/hotel_booking_project/data/rooms/");
    }
}
