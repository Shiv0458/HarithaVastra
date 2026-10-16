package com.harithavastra.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String uploads;

  public WebConfig(@Value("${app.upload-dir}") String u) {
    uploads = u;
  }

  public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/uploads/**")
      .addResourceLocations(
        Path.of(uploads).toAbsolutePath().toUri().toString()
      )
      .setCachePeriod(86400);
  }
}
