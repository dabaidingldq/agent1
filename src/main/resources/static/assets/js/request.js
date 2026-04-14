async function request(url, options = {}) {
    const finalOptions = {
        method: options.method || 'GET',
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        }
    };

    if (options.body !== undefined) {
        finalOptions.body = typeof options.body === 'string'
            ? options.body
            : JSON.stringify(options.body);
    }

    const response = await fetch(`${window.APP_CONFIG.baseURL}${url}`, finalOptions);

    if (!response.ok) {
        throw new Error(`请求失败: ${response.status}`);
    }

    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
        return response.json();
    }
    return response.text();
}

async function get(url) {
    return request(url, { method: 'GET' });
}

async function post(url, body) {
    return request(url, { method: 'POST', body });
}