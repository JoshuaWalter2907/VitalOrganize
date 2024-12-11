let currentChatPartner = null;

// Funktion zum Starten eines neuen Chats
function startNewChat() {
    const username = document.getElementById("username").value.trim();

    if (username) {
        // Sende Anfrage an den Backend-Controller, um den Benutzer zu finden
        axios.post('/api/chat/start', { username })
            .then(response => {
                if (response.data.success) {
                    currentChatPartner = username;
                    document.getElementById("chat-partner").textContent = username;
                    document.getElementById("new-chat").style.display = 'none';
                    document.getElementById("chat-window").style.display = 'block';

                    // Holen der ersten Nachrichten für den neuen Chat
                    loadMessages();
                } else {
                    alert("Benutzer nicht gefunden.");
                }
            })
            .catch(error => {
                console.error(error);
                alert("Fehler beim Starten des Chats.");
            });
    } else {
        alert("Bitte gib einen Benutzernamen ein.");
    }
}

// Nachrichten anzeigen
function loadMessages() {
    // Hier kannst du die Nachrichten vom Server abrufen
    axios.get(`/api/chat/messages?partner=${currentChatPartner}`)
        .then(response => {
            const messagesContainer = document.getElementById("messages");
            messagesContainer.innerHTML = '';  // Leeren der bisherigen Nachrichten

            response.data.messages.forEach(message => {
                const messageElement = document.createElement("div");
                messageElement.textContent = `${message.sender}: ${message.content}`;
                messagesContainer.appendChild(messageElement);
            });

            messagesContainer.scrollTop = messagesContainer.scrollHeight;  // Scrollen zum neuesten Beitrag
        })
        .catch(error => {
            console.error(error);
            alert("Fehler beim Laden der Nachrichten.");
        });
}

// Nachricht senden
function sendMessage() {
    const messageContent = document.getElementById("message").value.trim();

    if (messageContent) {
        axios.post('/api/chat/send', {
            recipient: currentChatPartner,
            content: messageContent
        })
            .then(response => {
                if (response.data.success) {
                    loadMessages();  // Nach dem Senden die Nachrichten erneut laden
                    document.getElementById("message").value = '';  // Eingabefeld leeren
                } else {
                    alert("Fehler beim Senden der Nachricht.");
                }
            })
            .catch(error => {
                console.error(error);
                alert("Fehler beim Senden der Nachricht.");
            });
    } else {
        alert("Bitte gib eine Nachricht ein.");
    }
}

// Chat beenden
function endChat() {
    currentChatPartner = null;
    document.getElementById("chat-partner").textContent = '';
    document.getElementById("new-chat").style.display = 'block';
    document.getElementById("chat-window").style.display = 'none';
}
