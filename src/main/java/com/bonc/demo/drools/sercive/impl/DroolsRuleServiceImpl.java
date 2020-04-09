package com.bonc.demo.drools.sercive.impl;

import com.bonc.demo.drools.sercive.DroolsRuleService;
import com.bonc.demo.drools.utils.FileManager;
import org.apache.commons.lang3.StringUtils;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.devguide.eshop.model.Customer;
import org.drools.devguide.eshop.model.Order;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.scanner.KieMavenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.util.stream.Collectors.joining;

@Service
public class DroolsRuleServiceImpl implements DroolsRuleService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void initDroolsRule() {
        String rule = redisTemplate.opsForValue().get("drools:demo:rule");
        if (StringUtils.isBlank(rule)) {
            rule = createDiscountRuleForSilverCustomers(10.0);
            redisTemplate.opsForValue().set("drools:demo:rule", rule);
        }
    }

    @Override
    public void updateDroolsRule(double discount) {
        String rule = createDiscountRuleForSilverCustomers(discount);
        redisTemplate.opsForValue().set("drools:demo:rule", rule);
    }

    private String createDiscountRuleForSilverCustomers(double discount) {
        StringBuilder ruleBuilder = new StringBuilder();
        ruleBuilder.append("rule 'Silver Customer - Discount'\n");
        ruleBuilder.append("when\n");
        ruleBuilder.append("    $o: Order($customer: customer, discount == null)\n");
        ruleBuilder.append("    $c: Customer(category == Category.SILVER, this == $customer)\n");
        ruleBuilder.append("then\n");
        ruleBuilder.append("    System.out.println(\"Executing Silver Customer ").append(discount).append("% Discount Rule!\");\n");
        ruleBuilder.append("    $o.setDiscount(new Discount(").append(discount).append("));\n");
        ruleBuilder.append("    update($o);\n");
        ruleBuilder.append("end\n");
        return ruleBuilder.toString();
    }

    private String getRule() {
        return redisTemplate.opsForValue().get("drools:demo:rule");
    }

    @Scheduled(fixedDelay = 2000)
    private void testDrools() throws Exception {
        KieMavenRepository repository = KieMavenRepository.getKieMavenRepository();

        Customer customerA = new Customer();
        customerA.setCategory(Customer.Category.SILVER);
        Order orderA = new Order();
        orderA.setCustomer(customerA);

        Customer customerB = new Customer();

        customerB.setCategory(Customer.Category.SILVER);
        Order orderB = new Order();
        orderB.setCustomer(customerB);

        String groupId = "com.bonc.test.drools";
        String artifactId = "test01";
        String version = "0.1-SNAPSHOT";

        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId(groupId, artifactId, version);

        InternalKieModule originalKJar = createKieJar(ks, releaseId, getRule());
        repository.installArtifact(releaseId, originalKJar, createKPom(releaseId));

        KieContainer kieContainer = ks.newKieContainer(releaseId);
        KieScanner scanner = ks.newKieScanner(kieContainer);
        KieSession kieSession = kieContainer.newKieSession();

        calculateAndPrint(customerA, orderA, kieSession);
        scanner.start(1000);
    }

    private void calculateAndPrint(Customer customer, Order order, KieSession ksession) {
        ksession.insert(customer);
        ksession.insert(order);
        ksession.fireAllRules();

        System.out.println("----------------------------------------------------");
        System.out.println("order.getDiscount().getPercentage():" + order.getDiscount().getPercentage());
        System.out.println("----------------------------------------------------");
    }

    private File createKPom(ReleaseId releaseId) throws IOException {
        FileManager fileManager = new FileManager();
        File pomFile = fileManager.newFile("pom.xml");
        fileManager.write(pomFile, getPom(releaseId));
        return pomFile;
    }

    private InternalKieModule createKieJar(KieServices ks, ReleaseId releaseId, String... rules) {

        KieFileSystem kfs = ks.newKieFileSystem();
        KieModuleModel kproj = ks.newKieModuleModel();

        kfs.writeKModuleXML(kproj.toXML());
        kfs.writePomXML(getPom(releaseId));

        StringBuilder packageBuilder = new StringBuilder();
        packageBuilder.append("package rules\n");
        packageBuilder.append("import org.drools.devguide.eshop.model.Customer;\n");
        packageBuilder.append("import org.drools.devguide.eshop.model.Order;\n");
        packageBuilder.append("import org.drools.devguide.eshop.model.Discount;\n");
        packageBuilder.append(Arrays.asList(rules).stream().collect(joining()));

        String file = "org/test/simple-discount-rules.drl";
        kfs.write("src/main/resources/KBase1/" + file, packageBuilder.toString());

        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        return (InternalKieModule) kieBuilder.getKieModule();
    }

    private String getPom(ReleaseId releaseId, ReleaseId... dependencies) {
        StringBuilder pomBuilder = new StringBuilder();

        pomBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        pomBuilder.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        pomBuilder.append("  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n");
        pomBuilder.append("  <modelVersion>4.0.0</modelVersion>\n");
        pomBuilder.append("\n");
        pomBuilder.append("  <groupId>" + releaseId.getGroupId() + "</groupId>\n");
        pomBuilder.append("  <artifactId>" + releaseId.getArtifactId() + "</artifactId>\n");
        pomBuilder.append("  <version>" + releaseId.getVersion() + "</version>\n");
        pomBuilder.append("\n");

        if (dependencies != null && dependencies.length > 0) {
            pomBuilder.append("  <dependencies>\n");
            for (ReleaseId dep : dependencies) {
                pomBuilder.append("    <dependency>\n");
                pomBuilder.append("      <groupId>" + dep.getGroupId() + "</groupId>\n");
                pomBuilder.append("      <artifactId>" + dep.getArtifactId() + "</artifactId>\n");
                pomBuilder.append("      <version>" + dep.getVersion() + "</version>\n");
                pomBuilder.append("    </dependency>\n");
            }
            pomBuilder.append("  </dependencies>\n");
        }
        pomBuilder.append("</project>");
        return pomBuilder.toString();
    }
}
