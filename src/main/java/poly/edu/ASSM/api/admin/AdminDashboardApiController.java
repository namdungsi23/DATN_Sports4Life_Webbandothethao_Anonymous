package poly.edu.ASSM.api.admin;



import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;



import poly.edu.ASSM.Services.core.AdminDashboardService;

import poly.edu.ASSM.dto.response.DashboardChartsResponse;

import poly.edu.ASSM.dto.response.DashboardResponse;



@RestController

@RequestMapping("/api/admin/dashboard")

@PreAuthorize("@adminAuth.has('DASHBOARD_VIEW')")

public class AdminDashboardApiController {



    @Autowired

    private AdminDashboardService adminDashboardService;



    @GetMapping

    public DashboardResponse stats(@RequestParam(defaultValue = "12") int months) {

        return adminDashboardService.getDashboard(months);

    }



    @GetMapping("/charts")

    public DashboardChartsResponse charts(@RequestParam(defaultValue = "12") int months) {

        return adminDashboardService.getCharts(months);

    }

}

