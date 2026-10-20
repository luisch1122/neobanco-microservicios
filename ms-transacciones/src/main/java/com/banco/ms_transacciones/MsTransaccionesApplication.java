package com.banco.ms_transacciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsTransaccionesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsTransaccionesApplication.class, args);
	}

}
