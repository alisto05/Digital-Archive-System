package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.SearchAndStatsDao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final SearchAndStatsDao searchAndStatsDao;

    public DashboardController(SearchAndStatsDao searchAndStatsDao) {
        this.searchAndStatsDao = searchAndStatsDao;
    }

    @GetMapping("/staff-stats")
    public ResponseEntity<Map<String, Object>> getStaffStats() {
        return ResponseEntity.ok(searchAndStatsDao.getStaffDashboardStats());
    }
}
