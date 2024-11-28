document.addEventListener('DOMContentLoaded', function() {
    const themeLinks = document.querySelectorAll('.dropdown-content a');
    const body = document.body;

    // Lade das Theme aus dem Cookie, falls vorhanden, ansonsten verwende 'gold' als Standard
    const savedTheme = getCookie('theme') || 'gold';

    // Setze die Theme-Klasse
    body.className = `${savedTheme}-theme`; // Setzt die entsprechende Klasse auf das Body-Element

    // Event Listener für jedes Theme-Option im Dropdown
    themeLinks.forEach(link => {
        link.addEventListener('click', function(event) {
            event.preventDefault(); // Verhindere das Standardverhalten des Links

            const selectedTheme = this.getAttribute('data-theme');

            // Ändere die Theme-Klasse
            body.className = `${selectedTheme}-theme`;

            // Speichere das ausgewählte Theme in einem Cookie
            setCookie('theme', selectedTheme, 7); // Speichern für 7 Tage
        });
    });

    // Funktion zum Setzen des Cookies
    function setCookie(name, value, days) {
        let expires = "";
        if (days) {
            const date = new Date();
            date.setTime(date.getTime() + (days*24*60*60*1000));
            expires = "; expires=" + date.toUTCString();
        }
        document.cookie = name + "=" + (value || "")  + expires + "; path=/";
    }

    // Funktion zum Abrufen des Cookie-Wertes
    function getCookie(name) {
        const nameEQ = name + "=";
        const ca = document.cookie.split(';');
        for(let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) == ' ') c = c.substring(1, c.length);
            if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length, c.length);
        }
        return null;
    }
});

document.addEventListener("DOMContentLoaded", function () {
    const passwordInput = document.getElementById("password");
    const passwordIcon = document.getElementById("show-password-icon");
    const tooltipText = document.getElementById("tooltip-text");

    // Event-Listener für Hover
    passwordIcon.addEventListener("mouseenter", function () {
        tooltipText.style.display = "block";
    });

    passwordIcon.addEventListener("mouseleave", function () {
        tooltipText.style.display = "none";
    });

    // Event-Listener für Klick, um Passwort-Sichtbarkeit umzuschalten
    passwordIcon.addEventListener("click", function () {
        if (passwordInput.type === "password") {
            passwordInput.type = "text";
            tooltipText.textContent = "Hide Password"; // Tooltip ändern
        } else {
            passwordInput.type = "password";
            tooltipText.textContent = "Show Password"; // Tooltip zurücksetzen
        }
    });
});

const languageLinks = document.querySelectorAll('.language-dropdown a');
languageLinks.forEach(link => {
    link.addEventListener('click', function(event) {
        event.preventDefault(); // Verhindert das Standardverhalten des Links

        const selectedLang = this.getAttribute('lang'); // Hier wird der Sprachcode (z. B. 'de' oder 'en') abgerufen

        // URL aktualisieren und die Seite neu laden
        const url = new URL(window.location.href);
        url.searchParams.set('lang', selectedLang); // Sprachparameter hinzufügen
        window.location.href = url.toString(); // Seite mit der neuen Sprache laden
    });
});