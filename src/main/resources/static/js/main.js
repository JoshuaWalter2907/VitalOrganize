// Elemente referenzieren
const toggleFormButton = document.getElementById('toggleFormButton');
const formContainer = document.getElementById('formContainer');

// Formular ein- oder ausblenden
toggleFormButton.addEventListener('click', () => {
    formContainer.classList.toggle('show');
});

let lastScrollPosition = 0;
const header = document.getElementById("header");

window.addEventListener("scroll", () => {
    const currentScrollPosition = window.pageYOffset;

    if (currentScrollPosition > lastScrollPosition) {
        // Nutzer scrollt nach unten -> Header verstecken
        header.classList.add("hidden");
    } else {
        // Nutzer scrollt nach oben -> Header anzeigen
        header.classList.remove("hidden");
    }

    lastScrollPosition = currentScrollPosition;
});

