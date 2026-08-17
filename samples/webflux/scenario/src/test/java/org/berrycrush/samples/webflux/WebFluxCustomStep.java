package org.berrycrush.samples.webflux;

import org.berrycrush.samples.webflux.service.DependencyService;
import org.berrycrush.step.Step;
import org.springframework.stereotype.Component;

@Component
public class WebFluxCustomStep {
    private final DependencyService dependencyService;
    public WebFluxCustomStep(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @Step("check name {string} and price {int}")
    public void checkProduct(String name, int price) {
        System.out.println("Checking product with name: " + name + " and price: " + price);
        dependencyService.performAction();
    }
}
