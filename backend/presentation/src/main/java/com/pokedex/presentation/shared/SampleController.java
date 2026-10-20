package com.pokedex.presentation.shared;

import com.pokedex.application.shared.SampleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder controller proving the webmvc slice (@WebMvcTest) builds and runs on Java 25. It returns
 * a DTO, never a domain object. Replaced by the real controllers in P4.
 */
@RestController
public class SampleController {

    private final SampleService service;

    public SampleController(SampleService service) {
        this.service = service;
    }

    @GetMapping("/sample")
    public SampleResponse sample() {
        return new SampleResponse(service.doubled());
    }
}
