-- Tabelle für Rezepte
CREATE TABLE Recipe (
    id INT AUTO_INCREMENT PRIMARY KEY, -- Eindeutige ID für das Rezept
    difficulty VARCHAR(50),
    keywords TEXT,
    nutrition_kcal INT,
    portions INT,
    source VARCHAR(255),
    source_url VARCHAR(255),
    title VARCHAR(255),
    total_time DOUBLE
);

-- Tabelle für Diäten (eine Diät pro Rezept)
CREATE TABLE RecipeDiet (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recipe_id INT,
    diet VARCHAR(50),
    FOREIGN KEY (recipe_id) REFERENCES Recipe(id) ON DELETE CASCADE
);

-- Tabelle für Zutaten
CREATE TABLE RecipeIngredient (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recipe_id INT,
    name VARCHAR(255),
    amount VARCHAR(50),
    unit VARCHAR(50),
    FOREIGN KEY (recipe_id) REFERENCES Recipe(id) ON DELETE CASCADE
);

-- Tabelle für Bilder
CREATE TABLE RecipeImage (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recipe_id INT,
    image_url VARCHAR(255),
    FOREIGN KEY (recipe_id) REFERENCES Recipe(id) ON DELETE CASCADE
);

-- Tabelle für Bewertungen
CREATE TABLE RecipeRating (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recipe_id INT,
    rating_value DOUBLE,
    rating_count INT,
    FOREIGN KEY (recipe_id) REFERENCES Recipe(id) ON DELETE CASCADE
);

-- Tabelle für gekochte Mahlzeiten
CREATE TABLE user_meals (
    id INT NOT NULL AUTO_INCREMENT,
    userId INT NOT NULL,
    recipeId INT NOT NULL,
    cookDate DATE NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (recipeId) REFERENCES recipes(id) ON DELETE CASCADE ON UPDATE CASCADE
);