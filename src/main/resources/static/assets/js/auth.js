function getCurrentUser() {
    return window.APP_CONFIG.currentUser;
}

function setCurrentUser(user) {
    window.APP_CONFIG.currentUser = {
        ...window.APP_CONFIG.currentUser,
        ...user
    };
}

function buildChatRequest(message) {
    const user = getCurrentUser();
    return {
        chatId: `chat-${Date.now()}`,
        userId: user.userId,
        tenantId: user.tenantId,
        role: user.role,
        message
    };
}