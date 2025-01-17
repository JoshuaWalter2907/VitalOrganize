document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("insertModal");
    const openButton = document.getElementById("insertButton");
    const closeButton = document.querySelector(".close");
    const cancelButton = document.getElementById("cancelButton");
    const recipeNameInput = document.getElementById("recipeName");
    const saveButton = document.getElementById("saveButton");

    // Modal öffnen
    if (openButton) {
        openButton.addEventListener("click", function () {
            modal.style.display = "block";
        });
    }

    // Modal schließen (x-Button und Abbrechen)
    if (closeButton) closeButton.addEventListener("click", closeModal);
    if (cancelButton) cancelButton.addEventListener("click", closeModal);

    function closeModal() {
        modal.style.display = "none";
        recipeNameInput.value = ""; // Eingabefeld leeren
    }

        // Rezept bearbeiten
        function editRecipe(button) {
            const recipeId = button.getAttribute('data-id');
            if (!recipeId) {
                console.error("Rezept-ID fehlt");
                return;
            }

            // Weiterleitung zur Bearbeitungsseite mit der Rezept-ID
            window.location.href = `/recipes/edit/${recipeId}`;
        }

        // Rezept löschen
        async function deleteRecipe(button) {
            const recipeId = button.getAttribute('data-id');
            if (!recipeId) {
                console.error("Rezept-ID fehlt");
                return;
            }

            const confirmed = confirm("Möchtest du dieses Rezept wirklich löschen?");
            if (confirmed) {
                try {
                    const response = await fetch(`/recipes/delete/${recipeId}`, { method: "DELETE" });

                    if (response.ok) {
                        alert("Rezept erfolgreich gelöscht!");
                        location.reload(); // Seite neu laden, um Änderungen zu sehen
                    } else {
                        alert("Fehler beim Löschen des Rezepts.");
                    }
                } catch (error) {
                    console.error("Fehler:", error);
                    alert("Es ist ein Fehler aufgetreten. Bitte versuche es erneut.");
                }
            }
        }

    // Rezept lokal speichern
    async function saveFavoriteRecipe(button) {
        const recipeData = {
            title: button.getAttribute("data-title"),
            difficulty: button.getAttribute("data-difficulty"),
            portions: parseInt(button.getAttribute("data-portions")) || 1,
            ingredients: button.getAttribute("data-ingredients"), // Bereits formatiert als String
            calories: parseInt(button.getAttribute("data-calories")) || 0,
            rating: parseFloat(button.getAttribute("data-rating")) || 0,
            source: button.getAttribute("data-source"),
            time: parseFloat(button.getAttribute("data-time")) || 0
        };

        // Debugging-Ausgabe
        console.log("Übermittelte Rezeptdaten:", recipeData);

        try {
            const response = await fetch("/recipes/saveLocal", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams(recipeData).toString()
            });

            if (response.ok) {
                alert("Rezept erfolgreich als Favorit gespeichert!");
                location.reload(); // Seite aktualisieren
            } else {
                const errorText = await response.text();
                console.error("Fehler beim Speichern:", errorText);
                alert("Fehler beim Speichern des Rezepts. Details in der Konsole.");
            }
        } catch (error) {
            console.error("Fehler beim Speichern:", error);
            alert("Es gab ein Problem beim Speichern des Rezepts.");
        }
    }

    // Funktionen global verfügbar machen
    window.saveRecipeLocal = saveFavoriteRecipe;
    window.editRecipe = editRecipe;
    window.deleteRecipe = deleteRecipe;
});
