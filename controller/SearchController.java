package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.SearchDao;
import com.syncpoint.archive.dto.SearchRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchDao searchDao;

    public SearchController(SearchDao searchDao) {
        this.searchDao = searchDao;
    }

   
    @PostMapping
    public ResponseEntity<Void> recordSearch(@Valid @RequestBody SearchRequest request) {
        searchDao.recordSearch(request.userId(), request.searchTerm(),
                request.searchScope(), request.resultsCount());
        return ResponseEntity.noContent().build();
    }
}
