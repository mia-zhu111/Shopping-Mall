package com.leyou.search.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.service.SearchService;
//import org.elasticsearch.action.search.SearchRequest;
import com.leyou.search.pojo.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class SearchController {
    @Autowired
    private SearchService searchService;

    /**
     * ๆ็ดขๅๅ
     *
     * @param request
     * @return
     */
    @PostMapping("page")
    public ResponseEntity<SearchResult<Goods>> search(@RequestBody SearchRequest request) {
        SearchResult<Goods> result = this.searchService.search(request);
        if (result == null || CollectionUtils.isEmpty(result.getItems())) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(result);
    }
}
