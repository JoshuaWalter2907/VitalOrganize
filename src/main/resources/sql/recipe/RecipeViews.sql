CREATE VIEW RecipeView AS
SELECT
    r.id AS recipe_id,
    r.title,
    r.difficulty,
    r.keywords,
    r.nutrition_kcal,
    r.portions,
    r.source,
    r.source_url,
    r.total_time,
    GROUP_CONCAT(DISTINCT rd.diet ORDER BY rd.diet) AS diets,
    GROUP_CONCAT(DISTINCT ri.image_url ORDER BY ri.image_url) AS image_urls,
    GROUP_CONCAT(DISTINCT CONCAT_WS('|', rin.name, rin.amount, rin.unit) ORDER BY rin.name) AS ingredients,
    rr.rating_value,
    rr.rating_count
FROM
    Recipe r
        LEFT JOIN RecipeDiet rd ON r.id = rd.recipe_id
        LEFT JOIN RecipeImage ri ON r.id = ri.recipe_id
        LEFT JOIN RecipeIngredient rin ON r.id = rin.recipe_id
        LEFT JOIN RecipeRating rr ON r.id = rr.recipe_id
GROUP BY
    r.id;
