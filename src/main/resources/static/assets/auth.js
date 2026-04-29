/**
 * 通用登录态工具
 * 作用：
 * 1. 页面打开时检查是否登录
 * 2. 检查当前用户身份是否匹配当前页面
 * 3. 显示当前用户信息
 * 4. 提供统一退出登录能力
 */

let CURRENT_LOGIN_USER = null;

/**
 * 获取当前登录用户
 * @param requiredRole 当前页面要求的身份：EMPLOYEE / HR / ADMIN
 */
async function getCurrentUser(requiredRole) {
    try {
        const res = await fetch('/auth/current', {
            method: 'GET',
            credentials: 'include'
        });

        const data = await res.json();

        if (data.code !== 0) {
            window.location.href = '/login.html';
            return null;
        }

        const user = data.data.user;

        if (requiredRole && user.role !== requiredRole) {
            alert('当前登录身份无权访问该页面，即将跳转到对应身份端。');
            window.location.href = data.data.redirectUrl || '/login.html';
            return null;
        }

        CURRENT_LOGIN_USER = user;
        return user;
    } catch (e) {
        console.error('获取当前登录用户失败：', e);
        window.location.href = '/login.html';
        return null;
    }
}

/**
 * 初始化页面右上角用户信息
 */
function fillUserInfo(user) {
    const userInfoEl = document.getElementById('currentUserInfo');
    if (userInfoEl && user) {
        const roleText = convertRoleText(user.role);
        userInfoEl.innerText = `${user.displayName || user.username}（${roleText}）`;
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.onclick = logout;
    }
}

/**
 * 退出登录
 */
async function logout() {
    try {
        await fetch('/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });
    } catch (e) {
        console.error('退出登录失败：', e);
    } finally {
        window.location.href = '/login.html';
    }
}

/**
 * 身份英文转中文
 */
function convertRoleText(role) {
    if (role === 'EMPLOYEE') {
        return '员工';
    }
    if (role === 'HR') {
        return 'HR';
    }
    if (role === 'ADMIN') {
        return '管理员';
    }
    return role || '未知身份';
}

/**
 * 带登录态的 fetch
 * 如果后端返回 401，自动跳登录页
 */
async function sessionFetch(url, options = {}) {
    const finalOptions = {
        ...options,
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        }
    };

    const res = await fetch(url, finalOptions);

    if (res.status === 401) {
        alert('登录状态已失效，请重新登录。');
        window.location.href = '/login.html';
        throw new Error('未登录');
    }

    if (res.status === 403) {
        alert('当前身份无权访问该资源。');
        throw new Error('无权限');
    }

    return res;
}