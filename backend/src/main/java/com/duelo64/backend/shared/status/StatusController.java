package com.duelo64.backend.shared.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    @GetMapping
    public ApplicationStatus getStatus() {
        return new ApplicationStatus("online", "Duelo64 API");
    }
}
