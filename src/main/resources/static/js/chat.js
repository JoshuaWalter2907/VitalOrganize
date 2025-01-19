document.addEventListener("DOMContentLoaded", function () {
    const chatContainer = document.querySelector(".chat-container");

    function scrollToBottom() {
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }

    const observer = new MutationObserver(() => {
        const isUserScrolling = chatContainer.scrollTop + chatContainer.clientHeight < chatContainer.scrollHeight;

        if (!isUserScrolling) {
            scrollToBottom();
        }
    });

    observer.observe(chatContainer, { childList: true }); // Beobachtet neue Nachrichten

    scrollToBottom();
});


var stompClient = null;

function connect() {
    var socket = new SockJS('/ws/chat');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        var senderId = document.getElementById('current-user-id').value;

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
        stompClient.send("/app/chat/send", {}, JSON.stringify(message));

        document.getElementById('message-input').value = '';
        if(!chatGroupId)
            showMessage(message);
    }
}

function showMessage(message) {
    console.log("Received message:", message);

    var messageContainer = document.querySelector('.chat-container');
    if (!messageContainer) {
        console.error("Chat container not found.");
        return;
    }

    var newMessage = document.createElement('div');
    newMessage.classList.add('message-box');

    var currentUserId = document.getElementById('current-user-id').value;

    if (message.recipient === null) {
        console.log("Message is a group message.");
        console.log(currentUserId + " " + message.sender.id);
        if (String(currentUserId) === String(message.sender.id)) {
            newMessage.classList.add('my-message'); // Nachricht vom aktuellen Benutzer
            console.log("my-message")
        } else {
            newMessage.classList.add('friend-message'); // Nachricht von einem anderen Benutzer
            console.log("friend-message")
        }

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
        if (String(currentUserId) === String(message.senderId)) {
            newMessage.classList.add('my-message');
        } else {
            newMessage.classList.add('friend-message');
        }

        newMessage.innerHTML = `
            <p class="message">
                <span>${message.content}</span>
            </p>
            <span class="message-time">${formatCurrentTimestamp(message.timestamp || new Date())}</span>
        `;
    }

    messageContainer.appendChild(newMessage);

    messageContainer.scrollTop = messageContainer.scrollHeight;
}


function formatCurrentTimestamp() {
    var now = new Date();

    var hours = now.getHours();
    var minutes = now.getMinutes();

    hours = hours < 10 ? '0' + hours : hours;
    minutes = minutes < 10 ? '0' + minutes : minutes;

    return hours + ':' + minutes;
}

document.getElementById('message-form').addEventListener('submit', sendMessage);

window.onload = connect;
