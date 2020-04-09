package com.bonc.demo.drools.controller;

import com.bonc.demo.drools.sercive.DroolsRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DroolsRuleController {

    @Autowired
    private DroolsRuleService droolsRuleService;

    @GetMapping("/update")
    public String updateRule(@RequestParam("discount") double discount) {
        droolsRuleService.updateDroolsRule(discount);
        return "success";
    }
}
