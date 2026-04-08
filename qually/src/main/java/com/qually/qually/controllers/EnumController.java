package com.qually.qually.controllers;

import com.qually.qually.models.enums.CopcCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/enum")
public class EnumController {

    @GetMapping("/categories")
    public List<Map<String, String>> getCopCategories() {
        return Arrays.stream(CopcCategory.values())
                .map(category -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", category.name());
                    map.put("label", formatDisplayLabel(category));
                    return map;
                })
                .toList();
    }

    private String formatDisplayLabel(CopcCategory category) {
        String name = category.name().toLowerCase();
        String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
        return "%s Critical".formatted(capitalized);

    }
}
