package com.backend.finledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.backend.finledger", "com.finledger"})
@EntityScan(basePackages = {"com.backend.finledger", "com.finledger"})
@EnableJpaRepositories(basePackages = {"com.backend.finledger", "com.finledger"})
public class FinledgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinledgerApplication.class, args);
	}

}
