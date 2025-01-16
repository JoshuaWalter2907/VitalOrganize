package com.springboot.vitalorganize.model.Ingredient;

import com.springboot.vitalorganize.entity.IngredientEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@AllArgsConstructor
public class IngredientListData {
    Page<IngredientEntity> page;
    String filter;
    String sort;
}