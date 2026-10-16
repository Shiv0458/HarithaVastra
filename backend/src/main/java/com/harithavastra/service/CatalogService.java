package com.harithavastra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harithavastra.dto.Requests.*;
import com.harithavastra.model.*;
import com.harithavastra.repository.*;
import jakarta.persistence.criteria.*;
import java.math.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

  private final ProductRepository products;
  private final ProductImageRepository images;
  private final ReviewRepository reviews;
  private final CategoryRepository categories;
  private final ObjectMapper json;

  public CatalogService(
    ProductRepository p,
    ProductImageRepository i,
    ReviewRepository r,
    CategoryRepository c,
    ObjectMapper j
  ) {
    products = p;
    images = i;
    reviews = r;
    categories = c;
    json = j;
  }

  public Product getIncludingArchived(Long id) {
    return products.findById(id).orElseThrow(Errors::missing);
  }

  public Product get(Long id) {
    return products
      .findById(id)
      .filter(p -> !p.archived)
      .orElseThrow(Errors::missing);
  }

  public Map<String, Object> view(Product p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", p.id);
    m.put("name", p.name);
    m.put("categoryId", p.categoryId);
    m.put("collection", p.collection);
    m.put("occasion", p.occasion);
    m.put("colour", p.colour);
    m.put("description", p.description);
    m.put("price", p.price);
    m.put("originalPrice", p.originalPrice);
    m.put(
      "discountPercent",
      p.originalPrice != null && p.originalPrice.compareTo(p.price) > 0
        ? p.originalPrice
            .subtract(p.price)
            .multiply(BigDecimal.valueOf(100))
            .divide(p.originalPrice, 0, RoundingMode.FLOOR)
            .intValue()
        : 0
    );
    m.put("stock", p.stock);
    m.put("demo", p.demo);
    m.put("archived", p.archived);
    m.put("createdAt", p.createdAt);
    m.put("images", images.findByProductIdOrderByPositionAsc(p.id));
    List<Review> rs = reviews.findByProductIdOrderByCreatedAtDesc(p.id);
    m.put(
      "rating",
      rs
        .stream()
        .mapToInt(r -> r.rating)
        .average()
        .orElse(0)
    );
    m.put("reviewCount", rs.size());
    try {
      m.put(
        "specifications",
        json.readValue(Objects.toString(p.specifications, "{}"), Map.class)
      );
    } catch (Exception e) {
      m.put("specifications", Map.of());
    }
    return m;
  }

  public Map<String, Object> search(
    String q,
    String collection,
    String colour,
    String occasion,
    BigDecimal min,
    BigDecimal max,
    boolean available,
    boolean newest,
    String sort,
    int page,
    int size
  ) {
    Specification<Product> filter = (root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      ps.add(cb.isFalse(root.get("archived")));
      if (q != null && !q.isBlank()) {
        String like =
          "%" +
          q
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_") +
          "%";
        ps.add(
          cb.or(
            cb.like(cb.lower(root.get("name")), like, '\\'),
            cb.like(cb.lower(root.get("description")), like, '\\'),
            cb.like(cb.lower(root.get("collection")), like, '\\'),
            cb.like(cb.lower(root.get("colour")), like, '\\'),
            cb.like(cb.lower(root.get("occasion")), like, '\\')
          )
        );
      }
      if (collection != null && !collection.isBlank()) ps.add(
        cb.equal(root.get("collection"), collection)
      );
      if (colour != null && !colour.isBlank()) ps.add(
        cb.equal(root.get("colour"), colour)
      );
      if (occasion != null && !occasion.isBlank()) ps.add(
        cb.equal(root.get("occasion"), occasion)
      );
      if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("price"), min));
      if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("price"), max));
      if (available) ps.add(cb.gt(root.get("stock"), 0));
      if (newest) ps.add(
        cb.greaterThan(
          root.get("createdAt"),
          java.time.Instant.now().minus(java.time.Duration.ofDays(30))
        )
      );
      if ("rating".equals(sort) && query.getResultType() != Long.class) {
        Subquery<Double> rating = query.subquery(Double.class);
        Root<Review> review = rating.from(Review.class);
        rating
          .select(cb.avg(review.get("rating")))
          .where(cb.equal(review.get("productId"), root.get("id")));
        query.orderBy(
          cb.desc(cb.coalesce(rating, 0.0)),
          cb.desc(root.get("id"))
        );
      }
      return cb.and(ps.toArray(Predicate[]::new));
    };
    Sort order = switch (Objects.toString(sort, "relevance")) {
      case "price-asc" -> Sort.by("price").ascending().and(Sort.by("id"));
      case "price-desc" -> Sort.by("price").descending().and(Sort.by("id"));
      case "rating" -> Sort.unsorted();
      default -> Sort.by("createdAt")
        .descending()
        .and(Sort.by("id").descending());
    };
    Page<Product> result = products.findAll(
      filter,
      PageRequest.of(Math.max(0, page), Math.min(48, Math.max(1, size)), order)
    );
    return Map.of(
      "items",
      result.getContent().stream().map(this::view).toList(),
      "total",
      result.getTotalElements(),
      "pages",
      result.getTotalPages(),
      "page",
      result.getNumber()
    );
  }

  @Transactional
  public Map<String, Object> save(Long id, ProductInput r) {
    if (
      r.originalPrice() != null && r.originalPrice().compareTo(r.price()) < 0
    ) throw Errors.bad("Original price cannot be below selling price");
    if (
      r.categoryId() != null && !categories.existsById(r.categoryId())
    ) throw Errors.bad("Choose a valid category");
    Product p =
      id == null
        ? new Product()
        : products.lockById(id).orElseThrow(Errors::missing);
    p.name = r.name();
    p.categoryId = r.categoryId();
    p.collection = r.collection();
    p.colour = r.colour();
    p.occasion = r.occasion();
    p.description = r.description();
    p.price = r.price();
    p.originalPrice = r.originalPrice();
    p.stock = r.stock();
    p.demo = r.demo();
    if (p.createdAt == null) p.createdAt = java.time.Instant.now();
    for (var entry : r.specifications().entrySet())
      if (
        entry.getKey().length() > 100 ||
        entry.getValue() == null ||
        entry.getValue().length() > 2000
      ) throw Errors.bad("Invalid specification");
    try {
      p.specifications = json.writeValueAsString(r.specifications());
    } catch (Exception e) {
      throw Errors.bad("Invalid specifications");
    }
    products.saveAndFlush(p);
    images.deleteByProductId(p.id);
    images.flush();
    int position = 0;
    for (ImageInput input : r.images()) {
      if (
        !input.url().matches("^(/images/|/uploads/)[A-Za-z0-9._/-]+$") &&
        !input.url().startsWith("https://")
      ) throw Errors.bad("Use uploaded images or HTTPS image URLs");
      ProductImage im = new ProductImage();
      im.productId = p.id;
      im.url = input.url();
      im.label = input.label();
      im.position = position++;
      images.save(im);
    }
    return view(p);
  }

  @Transactional
  public void archive(Long id) {
    Product p = products.lockById(id).orElseThrow(Errors::missing);
    p.archived = true;
  }
}
