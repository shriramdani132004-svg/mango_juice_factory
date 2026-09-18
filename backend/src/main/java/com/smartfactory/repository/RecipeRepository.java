package com.smartfactory.repository;

import com.smartfactory.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByProductId(Long productId);

    Optional<Recipe> findByProductIdAndVersion(Long productId, Integer version);

    @Query("SELECT r FROM Recipe r WHERE r.product.id = :productId AND r.isActive = true ORDER BY r.version DESC")
    List<Recipe> findActiveRecipesByProductId(@Param("productId") Long productId);

    @Query("SELECT r FROM Recipe r JOIN FETCH r.recipeMaterials rm WHERE r.id = :recipeId")
    Optional<Recipe> findByIdWithMaterials(@Param("recipeId") Long recipeId);
}
