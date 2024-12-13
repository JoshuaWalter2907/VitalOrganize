
let currentPage = 0;
let user1 = "user1"; // Replace with dynamic user info
let user2 = "user2";

async function loadMessages() {
    const response = await fetch(`/api/chat/${user1}/${user2}?page=${currentPage}&size=50`);
    const messages = await response.json();
    const chatContainer = document.getElementById("chat-container");

    messages.reverse().forEach(msg => {
        const messageBox = document.createElement("div");
        messageBox.className = msg.sender.username === user1 ? "message-box my-message" : "message-box friend-message";
        messageBox.innerHTML = `<p>${msg.content}<br><span>${new Date(msg.timestamp).toLocaleTimeString()}</span></p>`;
        chatContainer.prepend(messageBox);
    });

    currentPage++;
}

document.getElementById("chat-container").addEventListener("scroll", async (e) => {
    if (e.target.scrollTop === 0) {
        await loadMessages();
    }
});

// Initial load
loadMessages();
