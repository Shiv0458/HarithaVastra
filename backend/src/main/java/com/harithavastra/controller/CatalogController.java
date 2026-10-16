package com.harithavastra.controller;

import com.harithavastra.repository.*;
import com.harithavastra.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {

  private final ProductRepository products;
  private final CatalogService catalog;
  private final CategoryRepository categories;
  private final ReviewRepository reviews;
  private final PaymentGateway gateway;
  private final boolean demo;

  public CatalogController(
    ProductRepository p,
    CatalogService c,
    CategoryRepository ca,
    ReviewRepository r,
    PaymentGateway g,
    @Value("${app.demo}") boolean d
  ) {
    products = p;
    catalog = c;
    categories = ca;
    reviews = r;
    gateway = g;
    demo = d;
  }

  @GetMapping("/config")
  Object config() {
    return Map.of(
      "demo",
      demo,
      "paymentConfigured",
      gateway.configured(),
      "paymentMode",
      gateway.mode(),
      "keyId",
      gateway.keyId()
    );
  }

  @GetMapping("/products/facets")
  Object facets() {
    return Map.of(
      "collections",
      products.collections(),
      "colours",
      products.colours(),
      "occasions",
      products.occasions()
    );
  }

  @GetMapping("/categories")
  Object categories() {
    return categories.findAll();
  }

  @GetMapping("/products")
  Object products(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String collection,
    @RequestParam(required = false) String colour,
    @RequestParam(required = false) String occasion,
    @RequestParam(required = false) BigDecimal min,
    @RequestParam(required = false) BigDecimal max,
    @RequestParam(defaultValue = "false") boolean available,
    @RequestParam(defaultValue = "false") boolean newest,
    @RequestParam(defaultValue = "relevance") String sort,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    return catalog.search(
      q,
      collection,
      colour,
      occasion,
      min,
      max,
      available,
      newest,
      sort,
      page,
      size
    );
  }

  @GetMapping("/products/{id}")
  Object product(@PathVariable Long id) {
    return catalog.view(catalog.get(id));
  }

  @GetMapping("/products/{id}/reviews")
  Object reviews(@PathVariable Long id) {
    catalog.get(id);
    return reviews
      .findByProductIdOrderByCreatedAtDesc(id)
      .stream()
      .map(r ->
        Map.of(
          "rating",
          r.rating,
          "text",
          r.text,
          "name",
          r.displayName,
          "createdAt",
          r.createdAt
        )
      )
      .toList();
  }
}
