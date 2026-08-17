package org.berrycrush.samples.webflux.service;

import org.springframework.stereotype.Service;

@Service
public class DependencyService {
    public void performAction() {
        System.out.println("DependencyComponent action performed.");
    }
}
