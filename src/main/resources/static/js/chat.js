document.addEventListener("DOMContentLoaded", function () {
    const chatContainer = document.querySelector(".chat-container");

    // Funktion: Scrollt zum Ende des Containers
    function scrollToBottom() {
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    // Überwacht Änderungen im Chat-Container
    const observer = new MutationObserver(() => {
        const isUserScrolling = chatContainer.scrollTop + chatContainer.clientHeight < chatContainer.scrollHeight;

        if (!isUserScrolling) {
            // Nur automatisch scrollen, wenn der Benutzer nicht manuell scrollt
            scrollToBottom();
        }
    });

    observer.observe(chatContainer, { childList: true }); // Beobachtet neue Nachrichten

    // Optional: Initial einmal scrollen
    scrollToBottom();
});


var stompClient = null;

// Function to connect to the WebSocket server
function connect() {
    var socket = new SockJS('/ws/chat'); // WebSocket-Endpoint
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        var senderId = document.getElementById('current-user-id').value;

        // Prüfe, ob der Benutzer sich in einem Gruppenchat oder Einzelchat befindet
        const chatGroupId = document.getElementById('chat-group-id').value || null;
        console.log(senderId);

        console.log(chatGroupId);

        if (chatGroupId) {
            console.log("Subscribing to /topic/messages/group/" + chatGroupId);
            stompClient.subscribe('/topic/messages/group/' + chatGroupId, function (messageOutput) {
                var message = JSON.parse(messageOutput.body);
                console.log(message);
                showMessage(message);
            });
        } else {
            console.log("Subscribing to /topic/messages/" + senderId);
            stompClient.subscribe('/topic/messages/' + senderId, function (messageOutput) {
                var message = JSON.parse(messageOutput.body);
                showMessage(message);
            });
        }
    });
}

// Function to send a message to the WebSocket server
function sendMessage(event) {
    event.preventDefault();
    var messageInput = document.getElementById('message-input').value;
    var senderId = document.getElementById('current-user-id').value;
    var recipientId = document.getElementById('recipient-id').value || null;
    const chatGroupId = document.getElementById('chat-group-id').value || null;

    console.log(recipientId + " " + chatGroupId);

    if (messageInput.trim() !== "") {
        var message = {
            content: messageInput,
            senderId: senderId,
            recipientId: recipientId,
            chatGroupId: chatGroupId,
        };
        console.log("Message to send: ", message);
        stompClient.send("/app/chat/send", {}, JSON.stringify(message)); // Send the message to the server
        // Clear the input field after sending
        document.getElementById('message-input').value = '';
        if(!chatGroupId)
            showMessage(message);
    }
}

// Function to append new message to the chat UI
function showMessage(message) {
    console.log("Received message:", message);

    // Container für Nachrichten
    var messageContainer = document.querySelector('.chat-container');
    if (!messageContainer) {
        console.error("Chat container not found.");
        return;
    }

    // Neues Nachrichten-Element erstellen
    var newMessage = document.createElement('div');
    newMessage.classList.add('message-box');

    // Aktuelle Benutzer-ID abrufen
    var currentUserId = document.getElementById('current-user-id').value;

    // Gruppennachrichten vs. Direktnachrichten
    if (message.recipient === null) {
        console.log("Message is a group message.");
        console.log(currentUserId + " " + message.sender.id);
        // Gruppennachricht: Prüfen, ob der Absender der aktuelle Benutzer ist
        if (String(currentUserId) === String(message.sender.id)) {
            newMessage.classList.add('my-message'); // Nachricht vom aktuellen Benutzer
            console.log("my-message")
        } else {
            newMessage.classList.add('friend-message'); // Nachricht von einem anderen Benutzer
            console.log("friend-message")
        }

        // Gruppennachrichten-HTML
        newMessage.innerHTML = `
            <p class="message-sender">
                <a href="/profile?profileId=${message.sender.id}" class="sender-link">
                    ${message.sender.username}
                </a>
            </p>
            <p class="message">
                <span>${message.content}</span>
            </p>
            <span class="message-time">${formatCurrentTimestamp(message.timestamp || new Date())}</span>
        `;
    } else {
        // Direktnachricht: Prüfen, ob der Absender der aktuelle Benutzer ist
        if (String(currentUserId) === String(message.senderId)) {
            newMessage.classList.add('my-message'); // Nachricht vom aktuellen Benutzer
        } else {
            newMessage.classList.add('friend-message'); // Nachricht von einem anderen Benutzer
        }

        // Direktnachrichten-HTML
        newMessage.innerHTML = `
            <p class="message">
                <span>${message.content}</span>
            </p>
            <span class="message-time">${formatCurrentTimestamp(message.timestamp || new Date())}</span>
        `;
    }

    // Nachricht zum Container hinzufügen
    messageContainer.appendChild(newMessage);

    // Automatisch nach unten scrollen
    messageContainer.scrollTop = messageContainer.scrollHeight;
}


function formatCurrentTimestamp() {
    var now = new Date(); // Aktuelles Datum und Uhrzeit

    var hours = now.getHours(); // Stunden
    var minutes = now.getMinutes(); // Minuten

    // Stelle sicher, dass die Stunden und Minuten immer zweizifrig sind
    hours = hours < 10 ? '0' + hours : hours;
    minutes = minutes < 10 ? '0' + minutes : minutes;

    return hours + ':' + minutes; // Rückgabe im Format "HH:mm"
}

document.getElementById('message-form').addEventListener('submit', sendMessage);

// Connect to WebSocket server when the page loads
window.onload = connect;
