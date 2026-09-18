package com.smartfactory.repository;

import com.smartfactory.entity.RecipeMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeMaterialRepository extends JpaRepository<RecipeMaterial, Long> {

    List<RecipeMaterial> findByRecipeIdOrderByBomOrderAsc(Long recipeId);

    List<RecipeMaterial> findByRecipeIdAndIsOptionalFalse(Long recipeId);
}
