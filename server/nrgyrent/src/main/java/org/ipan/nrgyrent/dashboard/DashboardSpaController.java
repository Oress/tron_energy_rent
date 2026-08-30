package org.ipan.nrgyrent.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Supports direct navigation to Angular routes without handling any server API path. */
@Controller
public class DashboardSpaController {

    @GetMapping({"/dashboard", "/dashboard/**"})
    public String forwardDashboardRoute() {
        return "forward:/index.html";
    }
}
