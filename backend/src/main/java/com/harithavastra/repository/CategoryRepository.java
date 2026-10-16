package com.harithavastra.repository;

import com.harithavastra.model.Category;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
