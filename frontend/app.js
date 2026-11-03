
// E-Commerce Semester Project - Frontend JavaScript
// Handles login, products, shopping cart, orders, and the admin page.

const KEYCLOAK_URL = 'http://localhost:8080/realms/ecommerce';
const CLIENT_ID = 'ecommerce-project';
const REDIRECT_URL = window.location.origin + '/';

const $ = (id) => document.getElementById(id);
const money = (amount) => '$' + Number(amount || 0).toFixed(2);

let products = [];
let cart = [];
let token = null;
let userInfo = {};
let userRoles = new Set();
let currentPage = 'shop';
let adminOrdersCache = [];
const ORDER_STATUSES = ['NEW', 'PROCESSING', 'SHIPPED', 'DELIVERED'];

// General helper functions
function showMessage(message) {
    const toast = $('toast');
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3800);
}

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (character) => {
        const replacements = {
            '&': '&amp;', '<': '&lt;', '>': '&gt;',
            '"': '&quot;', "'": '&#39;'
        };
        return replacements[character];
    });
}

function hasPermission(permission) {
    return userRoles.has(permission);
}

// --------------------------------------------------
// KEYCLOAK LOGIN AND AUTHENTICATION
// --------------------------------------------------

// Converts bytes to a URL-safe Base64 string.
function base64UrlEncode(bytes) {
    return btoa(String.fromCharCode(...new Uint8Array(bytes)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

// Reads the information stored in a JWT token.
function decodeToken(jwt) {
    const payload = jwt.split('.')[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/');

    return JSON.parse(atob(payload));
}

// Gets the user's permissions from the Keycloak token.
function getUserRoles() {
    const roles = userInfo.resource_access?.[CLIENT_ID]?.roles || [];
    return new Set(roles);
}

// Updates the navigation based on whether the user is logged in.
function updateLoginStatus(data) {
    token = data?.access_token || null;
    userInfo = token ? decodeToken(token) : {};
    userRoles = getUserRoles();

    $('identity').textContent = token
        ? (userInfo.preferred_username || 'Signed in')
        : 'Guest';

    $('register').hidden = !!token;
    $('login').hidden = !!token;
    $('logout').hidden = !token;

    $('adminNav').hidden = !hasPermission('MANAGE_PRODUCTS');
    $('manageOrdersNav').hidden = !hasPermission('MANAGE_ORDERS');

    if ((currentPage === 'admin' && !hasPermission('MANAGE_PRODUCTS')) ||
        (currentPage === 'manageOrders' && !hasPermission('MANAGE_ORDERS'))) {
        showPage('shop');
    }
}

// Saves login information for the current browser session.
function saveLogin(data) {
    if (data) {
        const loginData = {
            ...data,
            expires_at: Date.now() + Number(data.expires_in || 300) * 1000
        };

        sessionStorage.setItem('auth', JSON.stringify(loginData));
    } else {
        sessionStorage.removeItem('auth');
    }

    updateLoginStatus(data);
}

// Redirects the user to Keycloak to sign in.
async function signIn(register = false) {
    const verifier = base64UrlEncode(
        crypto.getRandomValues(new Uint8Array(32))
    );

    const hash = await crypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(verifier)
    );

    const challenge = base64UrlEncode(hash);

    const state = base64UrlEncode(
        crypto.getRandomValues(new Uint8Array(16))
    );

    sessionStorage.setItem('oidc', JSON.stringify({
        verifier: verifier,
        state: state
    }));

    const endpoint = register ? 'registrations' : 'auth';

    const url = new URL(
        KEYCLOAK_URL + '/protocol/openid-connect/' + endpoint
    );

    const parameters = {
        client_id: CLIENT_ID,
        redirect_uri: REDIRECT_URL,
        response_type: 'code',
        scope: 'openid profile email',
        code_challenge: challenge,
        code_challenge_method: 'S256',
        state: state
    };

    for (const [key, value] of Object.entries(parameters)) {
        url.searchParams.set(key, value);
    }

    window.location.href = url.href;
}

// Exchanges an authorization code or refresh token for an access token.
async function requestToken(parameters) {
    const response = await fetch(
        KEYCLOAK_URL + '/protocol/openid-connect/token',
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams(parameters)
        }
    );

    const data = await response.json();

    if (!response.ok) {
        throw new Error(
            data.error_description || data.error || 'Login failed'
        );
    }

    return data;
}

// Handles the redirect back from Keycloak after login.
async function handleLoginCallback() {
    const parameters = new URLSearchParams(window.location.search);

    if (parameters.has('error')) {
        throw new Error(
            parameters.get('error_description') || parameters.get('error')
        );
    }

    if (!parameters.has('code')) {
        return;
    }

    const pendingLogin = JSON.parse(
        sessionStorage.getItem('oidc') || 'null'
    );

    if (!pendingLogin || parameters.get('state') !== pendingLogin.state) {
        throw new Error('Invalid login state');
    }

    const data = await requestToken({
        grant_type: 'authorization_code',
        code: parameters.get('code'),
        redirect_uri: REDIRECT_URL,
        code_verifier: pendingLogin.verifier,
        client_id: CLIENT_ID
    });

    saveLogin(data);

    sessionStorage.removeItem('oidc');
    window.history.replaceState({}, '', REDIRECT_URL);

    showMessage('Signed in successfully');
}

// Refreshes the access token when it expires.
async function getValidToken() {
    const savedLogin = JSON.parse(
        sessionStorage.getItem('auth') || 'null'
    );

    if (!savedLogin) {
        return null;
    }

    // Reuse the token if it has not expired.
    if (Date.now() < savedLogin.expires_at - 30000) {
        updateLoginStatus(savedLogin);
        return savedLogin.access_token;
    }

    // Log out locally if there is no refresh token.
    if (!savedLogin.refresh_token) {
        saveLogin(null);
        return null;
    }

    try {
        const refreshed = await requestToken({
            grant_type: 'refresh_token',
            refresh_token: savedLogin.refresh_token,
            client_id: CLIENT_ID
        });

        saveLogin(refreshed);
        return refreshed.access_token;
    } catch (error) {
        saveLogin(null);
        return null;
    }
}

// Sends requests to the API Gateway.
async function apiRequest(path, options = {}) {
    const accessToken = await getValidToken();

    const headers = {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(accessToken ? { Authorization: 'Bearer ' + accessToken } : {}),
        ...options.headers
    };

    const response = await fetch('/api' + path, {
        ...options,
        headers: headers
    });

    if (!response.ok) {
        const errorText = await response.text();

        throw new Error(
            'HTTP ' + response.status + ': ' + errorText.slice(0, 180)
        );
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

// Signs out of the application and Keycloak.
async function signOut() {
    const savedLogin = JSON.parse(
        sessionStorage.getItem('auth') || 'null'
    );

    saveLogin(null);

    const url = new URL(
        KEYCLOAK_URL + '/protocol/openid-connect/logout'
    );

    url.searchParams.set('client_id', CLIENT_ID);
    url.searchParams.set('post_logout_redirect_uri', REDIRECT_URL);

    if (savedLogin?.id_token) {
        url.searchParams.set('id_token_hint', savedLogin.id_token);
    }

    window.location.href = url.href;
}

// --------------------------------------------------
// PAGE NAVIGATION
// --------------------------------------------------

function showPage(page) {
    currentPage = page;
    $('shopHero').hidden = page !== 'shop';

    document.querySelectorAll('main .panel').forEach((panel) => {
        panel.hidden = panel.id !== page;
    });

    document.querySelectorAll('.tab').forEach((tab) => {
        tab.classList.toggle('active', tab.dataset.tab === page);
    });

    if (page === 'orders') {
        loadOrders();
    }

    if (page === 'profile') {
        loadProfile();
    }

    if (page === 'admin') {
        loadAdmin();
    }

    if (page === 'manageOrders') {
        loadManageOrders();
    }
}

// --------------------------------------------------
// PRODUCTS AND INVENTORY
// --------------------------------------------------

async function loadProducts() {
    try {
        products = await apiRequest('/v1/product');

        $('catalogStatus').textContent = products.length + ' products';

        updateCategoryFilter();
        displayProducts();
    } catch (error) {
        $('catalogStatus').textContent = 'Catalog unavailable';
        showMessage(error.message);
    }
}

function updateCategoryFilter() {
    const filter = $('categoryFilter');
    const selected = filter.value;
    const categories = [...new Set(products.map(p => p.category).filter(Boolean))].sort();
    filter.innerHTML = '<option value="">All categories</option>' + categories.map(category =>
        `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`
    ).join('');
    filter.value = categories.includes(selected) ? selected : '';
}

function displayProducts() {
    const icons = ['⌨', '◈', '◉', '✦', '▣', '◎'];

    let html = '';

    const selectedCategory = $('categoryFilter').value;
    const visibleProducts = selectedCategory ? products.filter(p => p.category === selectedCategory) : products;

    visibleProducts.forEach((product, index) => {
        const productId = escapeHtml(product.productId);
        const productName = escapeHtml(product.productName);
        const description = escapeHtml(
            product.description || 'Made for everyday use.'
        );
        const category = escapeHtml(product.category || 'Featured');
        const stock = Number(product.quantity) || 0;

        const disabled = stock < 1 ? 'disabled' : '';

        html += `
            <article class="product">
                <div class="product-visual">
                    ${icons[index % icons.length]}
                </div>

                <div class="product-body">
                    <span class="chip">${category}</span>

                    <div class="product-top" style="margin-top:14px">
                        <h3>${productName}</h3>
                        <span class="price">${money(product.price)}</span>
                    </div>

                    <p>${description}</p>

                    <div class="product-bottom">
                        <small>${stock} available</small>

                        <label class="quantity-label">
                            Qty
                            <input
                                class="quantity-input"
                                type="number"
                                min="1"
                                max="${stock}"
                                step="1"
                                value="1"
                                data-qty="${productId}"
                                ${disabled}
                            >
                        </label>

                        <button
                            class="primary"
                            data-add="${productId}"
                            ${disabled}
                        >
                            Add to bag
                        </button>
                    </div>
                </div>
            </article>
        `;
    });

    $('products').innerHTML = html || '<p>No products found in this category.</p>';

    document.querySelectorAll('[data-add]').forEach((button) => {
        button.onclick = () => addToCart(button.dataset.add);
    });
}

// --------------------------------------------------
// SHOPPING CART
// --------------------------------------------------

function addToCart(productId) {
    const product = products.find(
        (item) => item.productId === productId
    );

    if (!product) {
        return;
    }

    const quantityInput = Array.from(
        document.querySelectorAll('[data-qty]')
    ).find((input) => input.dataset.qty === productId);

    const quantity = Number(quantityInput?.value);

    if (!Number.isSafeInteger(quantity) || quantity < 1) {
        showMessage('Enter a positive whole-number quantity');
        return;
    }

    const existingItem = cart.find(
        (item) => item.productId === productId
    );

    const currentQuantity = existingItem?.quantity || 0;
    const availableStock = Number(product.quantity) || 0;

    if (currentQuantity + quantity > availableStock) {
        showMessage(
            'Only ' + (availableStock - currentQuantity) + ' more available'
        );
        return;
    }

    if (existingItem) {
        existingItem.quantity += quantity;
    } else {
        cart.push({
            productId: product.productId,
            productName: product.productName,
            price: product.price,
            quantity: quantity
        });
    }

    displayCart();
    showMessage('Added ' + quantity + ' to your bag');
}

function displayCart() {
    let itemCount = 0;
    let total = 0;

    cart.forEach((item) => {
        itemCount += item.quantity;
        total += item.quantity * item.price;
    });

    $('cartCount').textContent = itemCount;
    $('bagCount').textContent = itemCount;
    $('cartTotal').textContent = money(total);

    let html = '';

    cart.forEach((item, index) => {
        html += `
            <div class="item-row">
                <div>
                    <strong>${escapeHtml(item.productName)}</strong>
                    <p>${money(item.price)} × ${item.quantity}</p>
                </div>

                <button data-remove="${index}">
                    Remove
                </button>
            </div>
        `;
    });

    $('cartItems').innerHTML = html ||
        '<p class="muted">Your bag is empty.</p>';

    document.querySelectorAll('[data-remove]').forEach((button) => {
        button.onclick = () => {
            cart.splice(Number(button.dataset.remove), 1);
            displayCart();
        };
    });
}

// --------------------------------------------------
// CHECKOUT AND CUSTOMER ORDERS
// --------------------------------------------------

async function checkout() {
    if (!token) {
        showMessage('Sign in to place an order');
        return signIn();
    }

    if (!hasPermission('PLACE_ORDERS')) {
        showMessage('Your account cannot place orders');
        return;
    }

    if (cart.length === 0) {
        showMessage('Add a product first');
        return;
    }

    const orderItems = cart.map((item) => ({
        productId: item.productId,
        quantity: item.quantity
    }));

    try {
        await apiRequest('/v1/order', {
            method: 'POST',
            body: JSON.stringify({ items: orderItems })
        });

        cart = [];

        displayCart();
        await loadProducts();

        $('cart').classList.remove('open');

        showMessage('Order placed!');
        showPage('orders');
    } catch (error) {
        showMessage(error.message);
    }
}

async function loadOrders() {
    if (!token) {
        $('ordersList').innerHTML = '<p>Sign in to see your orders.</p>';
        return;
    }

    try {
        const orders = await apiRequest('/v1/order');
        $('ordersList').innerHTML = orders.map(order => orderDetailsHtml(order)).join('') || '<p>No orders yet.</p>';
    } catch (error) {
        showMessage(error.message);
    }
}

// Shared by My Orders and Manage Orders.
function orderDetailsHtml(order, admin = false) {
    const id = escapeHtml(order.orderId || '');
    const status = escapeHtml(order.status || 'NEW');
    const items = order.items || [];
    const rows = items.map(item => `
        <tr><td>${escapeHtml(item.productName)}</td><td>${Number(item.quantity)}</td>
        <td>${money(item.price)}</td><td>${money(Number(item.price) * Number(item.quantity))}</td></tr>
    `).join('');
    const statusControl = admin ? `
        <label class="status-control">Update status
            <select data-order-status="${id}">
                ${ORDER_STATUSES.map(value => `<option value="${value}" ${value === order.status ? 'selected' : ''}>${value}</option>`).join('')}
            </select>
        </label>
        <button class="primary" data-save-status="${id}">Save status</button>
    ` : '';

    return `<details class="order-detail">
        <summary><span>Order ${escapeHtml(id.slice(0, 8))} · <strong>${status}</strong> · ${escapeHtml(order.orderDate || '')}</span>
            <strong>${money(order.total)}</strong></summary>
        <div class="order-detail-body">
            <p><strong>Order ID:</strong> ${id}</p>
            <p><strong>Customer:</strong> ${escapeHtml(order.customerName || '')}</p>
            <p><strong>Customer ID:</strong> ${escapeHtml(order.customerId || '')}</p>
            <p><strong>Status:</strong> ${status}</p>
            <table class="order-table"><thead><tr><th>Product</th><th>Qty</th><th>Unit price</th><th>Subtotal</th></tr></thead>
                <tbody>${rows}</tbody></table>
            <p><strong>Total: ${money(order.total)}</strong></p>
            ${statusControl}
        </div>
    </details>`;
}

// --------------------------------------------------
// CUSTOMER PROFILE
// --------------------------------------------------

async function loadProfile() {
    if (!token) {
        showMessage('Sign in to view your profile');
        return signIn();
    }

    try {
        const customer = await apiRequest('/v1/customer/me');
        const form = $('profileForm');

        for (const [key, value] of Object.entries(customer)) {
            if (form.elements[key]) {
                form.elements[key].value = value || '';
            }
        }
    } catch (error) {
        showMessage(error.message);
    }
}

async function saveProfile(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const customer = Object.fromEntries(formData);

    try {
        await apiRequest('/v1/customer/me', {
            method: 'PUT',
            body: JSON.stringify(customer)
        });

        showMessage('Profile updated');
    } catch (error) {
        showMessage(error.message);
    }
}

// --------------------------------------------------
// ADMIN PAGE - PRODUCTS
// --------------------------------------------------

function displayAdminProducts() {
    let html = '';

    products.forEach((product) => {
        html += `
            <div class="item-row">
                <div>
                    <strong>${escapeHtml(product.productName)}</strong>
                    <p>
                        ${money(product.price)}
                        · Stock ${product.quantity}
                    </p>
                </div>

                <div class="item-actions">
                    <button data-edit="${escapeHtml(product.productId)}">
                        Edit
                    </button>

                    <button data-delete="${escapeHtml(product.productId)}">
                        Delete
                    </button>
                </div>
            </div>
        `;
    });

    $('adminProducts').innerHTML = html;

    document.querySelectorAll('[data-edit]').forEach((button) => {
        button.onclick = () => editProduct(button.dataset.edit);
    });

    document.querySelectorAll('[data-delete]').forEach((button) => {
        button.onclick = () => deleteProduct(button.dataset.delete);
    });
}

function editProduct(productId) {
    const product = products.find(
        (item) => item.productId === productId
    );

    if (!product) {
        return;
    }

    const form = $('productForm');
    // Keep an existing category editable even if it is not in the default list.
    const categorySelect = form.elements.category;
    if (product.category && ![...categorySelect.options].some(option => option.value === product.category)) {
        categorySelect.add(new Option(product.category, product.category));
    }

    for (const [key, value] of Object.entries(product)) {
        if (form.elements[key]) {
            form.elements[key].value = value ?? '';
        }
    }

    form.scrollIntoView({ behavior: 'smooth' });
}

async function deleteProduct(productId) {
    if (!confirm('Delete this product?')) {
        return;
    }

    try {
        await apiRequest('/v1/product/' + productId, {
            method: 'DELETE'
        });

        showMessage('Deleted');
        loadAdmin();
    } catch (error) {
        showMessage(error.message);
    }
}

async function saveProduct(event) {
    event.preventDefault();

    const form = event.target;
    const product = Object.fromEntries(new FormData(form));

    product.price = Number(product.price);
    product.quantity = Number(product.quantity);

    const method = product.productId ? 'PUT' : 'POST';

    try {
        await apiRequest('/v1/product', {
            method: method,
            body: JSON.stringify(product)
        });

        form.reset();

        showMessage('Product saved');
        loadAdmin();
    } catch (error) {
        showMessage(error.message);
    }
}

// --------------------------------------------------
// ADMIN PAGE - CUSTOMER ORDERS
// --------------------------------------------------

// Groups orders by customer ID so each customer has their own section.
function groupOrdersByCustomer(orders) {
    const groupedOrders = {};

    orders.forEach((order) => {
        const customerId = order.customerId;

        if (!groupedOrders[customerId]) {
            groupedOrders[customerId] = [];
        }

        groupedOrders[customerId].push(order);
    });

    return groupedOrders;
}

function displayAdminOrders(orders) {
    const search = $('customerIdFilter').value.trim().toLowerCase();
    const filtered = orders.filter(order => String(order.customerId || '').toLowerCase().includes(search));
    const groups = groupOrdersByCustomer(filtered);
    let html = '';

    for (const [customerId, customerOrders] of Object.entries(groups)) {
        const name = customerOrders[0].customerName || 'Customer';
        html += `<section class="order-group">
            <h4>${escapeHtml(name)} <small>(${customerOrders.length} orders)</small></h4>
            <p class="muted">Customer ID: ${escapeHtml(customerId)}</p>
            ${customerOrders.map(order => orderDetailsHtml(order, true)).join('')}
        </section>`;
    }

    $('adminOrders').innerHTML = html || '<p>No orders match this customer ID.</p>';
    document.querySelectorAll('[data-save-status]').forEach(button => {
        button.onclick = () => saveOrderStatus(button.dataset.saveStatus);
    });
}

async function loadManageOrders() {
    if (!hasPermission('MANAGE_ORDERS')) return showPage('shop');
    try {
        adminOrdersCache = await apiRequest('/v1/order');
        displayAdminOrders(adminOrdersCache);
    } catch (error) {
        showMessage(error.message);
    }
}

async function saveOrderStatus(orderId) {
    const select = [...document.querySelectorAll('[data-order-status]')]
        .find(element => element.dataset.orderStatus === orderId);
    if (!select || !ORDER_STATUSES.includes(select.value)) return;
    try {
        await apiRequest('/v1/order/' + encodeURIComponent(orderId) + '/status', {
            method: 'PATCH',
            body: JSON.stringify({ status: select.value })
        });
        showMessage('Order status updated');
        await loadManageOrders();
    } catch (error) {
        showMessage(error.message);
    }
}

// --------------------------------------------------
// ADMIN PAGE - CUSTOMERS
// --------------------------------------------------

function displayAdminCustomers(customers) {
    let html = '';

    customers.forEach((customer) => {
        html += `
            <div class="item-row">
                <div>
                    <strong>
                        ${escapeHtml(customer.firstName)}
                        ${escapeHtml(customer.lastName)}
                    </strong>

                    <p>${escapeHtml(customer.email)}</p>
                </div>
            </div>
        `;
    });

    $('adminCustomers').innerHTML = html ||
        '<p>No customer profiles.</p>';
}

// Loads all the information for the admin page.
async function loadAdmin() {
    if (!hasPermission('MANAGE_PRODUCTS')) {
        showPage('shop');
        return;
    }

    try {
        await loadProducts();
        displayAdminProducts();


    } catch (error) {
        showMessage(error.message);
    }
}

// --------------------------------------------------
// START THE APPLICATION
// --------------------------------------------------

async function initializeApp() {
    // Navigation buttons
    document.querySelectorAll('[data-tab]').forEach((button) => {
        button.onclick = () => showPage(button.dataset.tab);
    });

    // Browse products
    $('browse').onclick = () => {
        showPage('shop');
        $('shop').scrollIntoView({ behavior: 'smooth' });
    };

    // Authentication buttons
    $('login').onclick = () => signIn();
    $('register').onclick = () => signIn(true);
    $('logout').onclick = signOut;

    // Shopping cart buttons
    $('openCart').onclick = () => $('cart').classList.add('open');
    $('closeCart').onclick = () => $('cart').classList.remove('open');
    $('checkout').onclick = checkout;

    // Orders and forms
    $('reloadOrders').onclick = loadOrders;
    $('categoryFilter').onchange = displayProducts;
    $('customerIdFilter').oninput = () => displayAdminOrders(adminOrdersCache);
    $('clearCustomerFilter').onclick = () => { $('customerIdFilter').value = ''; displayAdminOrders(adminOrdersCache); };
    $('refreshAdminOrders').onclick = loadManageOrders;
    $('profileForm').onsubmit = saveProfile;
    $('productForm').onsubmit = saveProduct;

    $('resetProduct').onclick = () => {
        $('productForm').reset();
    };

    // Restore an existing login session.
    try {
        await handleLoginCallback();
        await getValidToken();
    } catch (error) {
        showMessage(error.message);
        window.history.replaceState({}, '', REDIRECT_URL);
    }

    displayCart();
    await loadProducts();
}

initializeApp();
