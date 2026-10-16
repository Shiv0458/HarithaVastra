package com.harithavastra.controller;

import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import com.harithavastra.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final CatalogService catalog;
  private final ProductRepository products;
  private final CategoryRepository categories;
  private final ShopOrderRepository orders;
  private final PaymentRepository payments;
  private final CheckoutService checkout;
  private final PaymentService paymentService;
  private final UploadService uploads;

  public AdminController(
    CatalogService c,
    ProductRepository p,
    CategoryRepository ca,
    ShopOrderRepository o,
    PaymentRepository pa,
    CheckoutService ch,
    PaymentService ps,
    UploadService up
  ) {
    catalog = c;
    products = p;
    categories = ca;
    orders = o;
    payments = pa;
    checkout = ch;
    paymentService = ps;
    uploads = up;
  }

  @GetMapping("/products")
  Object products(@RequestParam(defaultValue = "0") int page) {
    return products
      .findAll(
        PageRequest.of(Math.max(0, page), 24, Sort.by("id").descending())
      )
      .map(catalog::view);
  }

  @PostMapping("/products")
  Object create(@Valid @RequestBody ProductInput p) {
    return catalog.save(null, p);
  }

  @PutMapping("/products/{id}")
  Object edit(@PathVariable Long id, @Valid @RequestBody ProductInput p) {
    return catalog.save(id, p);
  }

  @DeleteMapping("/products/{id}")
  Object archive(@PathVariable Long id) {
    catalog.archive(id);
    return Map.of("ok", true);
  }

  @PostMapping("/images")
  Object upload(@RequestParam("file") MultipartFile file)
    throws java.io.IOException {
    return Map.of("url", uploads.upload(file));
  }

  @PostMapping("/categories")
  Object category(@Valid @RequestBody CategoryInput input) {
    Category c = new Category();
    c.name = input.name();
    c.slug = input.slug();
    return categories.save(c);
  }

  @PutMapping("/categories/{id}")
  Object editCategory(
    @PathVariable Long id,
    @Valid @RequestBody CategoryInput input
  ) {
    Category c = categories.findById(id).orElseThrow(Errors::missing);
    c.name = input.name();
    c.slug = input.slug();
    return categories.save(c);
  }

  @GetMapping("/orders")
  Object orders(@RequestParam(defaultValue = "0") int page) {
    return orders
      .findAll(
        PageRequest.of(Math.max(0, page), 24, Sort.by("createdAt").descending())
      )
      .map(checkout::view);
  }

  @GetMapping("/orders/{id}/payments")
  Object payments(@PathVariable String id) {
    return payments.findByOrderId(id);
  }

  @PostMapping("/orders/{id}/refresh")
  Object reconcile(@PathVariable String id) {
    return checkout.view(paymentService.reconcile(id, true));
  }

  @PutMapping("/orders/{id}/fulfillment")
  @Transactional
  Object fulfillment(
    @PathVariable String id,
    @Valid @RequestBody Fulfillment f
  ) {
    ShopOrder o = orders.lock(id).orElseThrow(Errors::missing);
    if (!"PAID".equals(o.status)) throw Errors.conflict(
      "Only verified paid orders may be fulfilled"
    );
    List<String> stages = List.of(
      "UNFULFILLED",
      "PACKING",
      "SHIPPED",
      "DELIVERED"
    );
    if (
      stages.indexOf(f.status()) < stages.indexOf(o.fulfillment)
    ) throw Errors.bad("Fulfilment cannot move backwards");
    o.fulfillment = f.status();
    return checkout.view(o);
  }
}
