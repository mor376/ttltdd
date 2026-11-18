package com.example.tintuc.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping({"/", "/admin"})
    public String home() {
        return "redirect:/admin/dashboard";
    }
}
