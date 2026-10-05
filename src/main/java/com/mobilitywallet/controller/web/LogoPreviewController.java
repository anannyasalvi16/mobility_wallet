package com.mobilitywallet.controller.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Temporary development-only controller for previewing and selecting logo designs.
 */
@Controller
@Profile({"dev", "test"})
public class LogoPreviewController {

    @GetMapping("/logo-preview")
    public String logoPreview() {
        return "logo-preview";
    }
}
