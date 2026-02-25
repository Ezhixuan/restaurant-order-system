package com.restaurant.table.controller;

import com.restaurant.common.Result;
import com.restaurant.table.dto.CreateTableRequest;
import com.restaurant.table.dto.UpdateTableRequest;
import com.restaurant.table.entity.RestaurantTable;
import com.restaurant.table.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping
    public Result<List<RestaurantTable>> list(@RequestParam(required = false) Integer type) {
        if (type != null) {
            return Result.success(tableService.listByType(type));
        }
        return Result.success(tableService.listAll());
    }

    @GetMapping("/{id}")
    public Result<RestaurantTable> getById(@PathVariable Long id) {
        return Result.success(tableService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateTableRequest request) {
        tableService.create(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateTableRequest request) {
        tableService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/open")
    public Result<Void> openTable(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer customerCount) {
        tableService.openTable(id, customerCount);
        return Result.success();
    }

    @PostMapping("/{id}/clear")
    public Result<Void> clearTable(@PathVariable Long id) {
        tableService.clearTable(id);
        return Result.success();
    }

    @PostMapping("/{id}/pending-clear")
    public Result<Void> setPendingClear(@PathVariable Long id) {
        tableService.setPendingClear(id);
        return Result.success();
    }
}
