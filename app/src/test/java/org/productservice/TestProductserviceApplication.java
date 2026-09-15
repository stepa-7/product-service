package org.productservice;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestProductserviceApplication {

    public static void main(String[] args) {
        SpringApplication.from(ProductServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
