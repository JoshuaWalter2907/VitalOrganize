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
            chatGroupId: chatGroupId
        };
        console.log("Message to send: ", message);
        stompClient.send("/app/chat/send", {}, JSON.stringify(message)); // Send the message to the server


        showMessage(message);
        // Clear the input field after sending
        document.getElementById('message-input').value = '';
    }
}

// Function to append new message to the chat UI
function showMessage(message) {
    console.log("Received message:", message);
    var messageContainer = document.querySelector('.chat-container');
    var newMessage = document.createElement('div');
    newMessage.classList.add('message-box');
    newMessage.classList.add(message.senderId === document.getElementById('current-user-id').value ? 'my-message' : 'friend-message');

    console.log(newMessage);

    const groupPrefix = message.chatGroupId ? `[Group ${message.chatGroupId}] ` : '';

    var formattedTimestamp = formatCurrentTimestamp();

    newMessage.innerHTML = `<p>${groupPrefix}${message.content}</p><span class="message-time">${formattedTimestamp}</span>`;
    messageContainer.appendChild(newMessage);
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

// Set up the form submit event handler
document.getElementById('message-form').addEventListener('submit', sendMessage);

// Connect to WebSocket server when the page loads
window.onload = connect;

document.querySelectorAll('.button').forEach(button => button.addEventListener('click', e => {
    // Stelle sicher, dass der Button nicht schon die 'delete' Klasse hat
    if (!button.classList.contains('delete')) {
        button.classList.add('delete'); // Füge die 'delete' Klasse hinzu
        setTimeout(() => button.classList.remove('delete'), 3200); // Entferne sie nach 3,2 Sekunden
    }

    // Verhindere das Standardverhalten (Formularabsenden) und sende das Formular explizit
    e.preventDefault();

    // Finde das Formular, das den Button enthält und sende es ab
    const form = button.closest('form'); // Sucht das nächstgelegene 'form' Element
    if (form) {
        setTimeout(() => form.submit(), 3000);    }
}));

