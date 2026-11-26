package com.shopeasy;

import com.shopeasy.Domain.PRC.PricingRules;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PricingRules.class)
public class ShopEasyApp {

	public static void main(String[] args) {
		SpringApplication.run(ShopEasyApp.class, args);
	}

}
