document.addEventListener("DOMContentLoaded", function () {
    const ingredientsContainer = document.getElementById("ingredientsContainer");
    const addIngredientButton = document.getElementById("addIngredientButton");
    const backButton = document.getElementById("backButton");
    const recipeForm = document.getElementById("recipeForm");

    // Zutaten hinzufügen
    addIngredientButton.addEventListener("click", function () {
        const ingredientRow = document.createElement("tr");
        ingredientRow.classList.add("ingredient-row");

        ingredientRow.innerHTML = `
            <td><input type="text" class="ingredient-name" name="ingredients[].name" placeholder="Zutat" required></td>
            <td><input type="text" class="ingredient-amount" name="ingredients[].amount" placeholder="Menge" required></td>
            <td><input type="text" class="ingredient-unit" name="ingredients[].unit" placeholder="Einheit" required></td>
            <td><button type="button" class="btn-remove">x</button></td>
        `;

        // Event-Listener für die Entfernen-Schaltfläche
        const removeButton = ingredientRow.querySelector(".btn-remove");
        removeButton.addEventListener("click", function () {
            ingredientsContainer.removeChild(ingredientRow);
        });

        ingredientsContainer.appendChild(ingredientRow);
    });

    // Zutaten entfernen
    window.removeIngredient = function (button) {
        const ingredientRow = button.closest("tr");
        ingredientsContainer.removeChild(ingredientRow);
    };

    // Zurück-Button
    backButton.addEventListener("click", function () {
        window.location.href = "/recipes/internal";
    });

    // Formular absenden
    recipeForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const formData = new FormData(recipeForm);

        // Rezept-ID auslesen
        const recipeId = recipeForm.getAttribute("data-id");

        // Verarbeite 'diet' - Leerstring in leeres Array umwandeln
        const diet = formData.get("diet")
            ? formData.get("diet").split(",").map(d => d.trim())
            : [];

        // Zutaten-Daten auslesen
        const ingredients = [];
        const ingredientRows = document.querySelectorAll(".ingredient-row");
        ingredientRows.forEach(row => {
            const name = row.querySelector(".ingredient-name").value.trim();
            const amount = row.querySelector(".ingredient-amount").value.trim();
            const unit = row.querySelector(".ingredient-unit").value.trim();
            if (name && amount && unit) {
                ingredients.push({ name, amount, unit });
            }
        });

        const recipeData = {
            id: recipeId,
            title: formData.get("title"),
            difficulty: formData.get("difficulty"),
            keywords: formData.get("keywords") || "",
            nutrition: { kcal: parseInt(formData.get("nutrition.kcal")) || 0 },
            portions: parseInt(formData.get("portions")) || 1,
            source: null,
            source_url: null,
            totalTime: parseFloat(formData.get("totalTime")) || 0,
            diet: diet,
            ingredients: ingredients,
            image_urls: null,
            rating: null
        };

        try {
            const response = await fetch("/recipes", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(recipeData)
            });

            if (response.ok) {
                alert("Rezept erfolgreich gespeichert!");
            } else {
                throw new Error("Fehler beim Speichern des Rezepts");
            }
        } catch (error) {
            console.error(error);
            alert("Es gab ein Problem beim Speichern des Rezepts.");
        }
    });
});
