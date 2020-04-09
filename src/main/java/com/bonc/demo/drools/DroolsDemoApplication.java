package com.bonc.demo.drools;

import com.bonc.demo.drools.sercive.DroolsRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DroolsDemoApplication implements CommandLineRunner {

    @Autowired
    private DroolsRuleService droolsRuleService;

	public static void main(String[] args) {
		SpringApplication.run(DroolsDemoApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
	    droolsRuleService.initDroolsRule();
    }
}
