package poly.edu.ASSM.Services.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import poly.edu.ASSM.component.AdminInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer{
	
	@Autowired
	AdminInterceptor adminInterceptor;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		/*
		 registry.addInterceptor(adminInterceptor)
				.addPathPatterns("/admin/**");
		*/
	}
}
