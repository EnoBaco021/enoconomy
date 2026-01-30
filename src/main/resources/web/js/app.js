// API Configuration
const API_BASE = '/api';
let authToken = null;
let apiKey = null;
let currencySymbol = '$';

// DOM Elements
const loginModal = document.getElementById('loginModal');
const mainContainer = document.getElementById('mainContainer');
const loginForm = document.getElementById('loginForm');
const loginError = document.getElementById('loginError');
const logoutBtn = document.getElementById('logoutBtn');
const sidebarToggle = document.getElementById('sidebarToggle');
const sidebar = document.querySelector('.sidebar');
const navItems = document.querySelectorAll('.nav-item');
const pages = document.querySelectorAll('.page');
const pageTitle = document.getElementById('pageTitle');
const playerModal = document.getElementById('playerModal');
const closePlayerModal = document.getElementById('closePlayerModal');
const balanceForm = document.getElementById('balanceForm');
const toast = document.getElementById('toast');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
});

function checkAuth() {
    const savedToken = localStorage.getItem('authToken');
    const savedApiKey = localStorage.getItem('apiKey');

    if (savedToken && savedApiKey) {
        authToken = savedToken;
        apiKey = savedApiKey;
        showMainContent();
        loadDashboard();
    } else {
        showLogin();
    }
}

function setupEventListeners() {
    loginForm.addEventListener('submit', handleLogin);
    logoutBtn.addEventListener('click', handleLogout);
    sidebarToggle.addEventListener('click', () => sidebar.classList.toggle('open'));

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(item.dataset.page);
        });
    });

    closePlayerModal.addEventListener('click', () => playerModal.classList.remove('active'));
    balanceForm.addEventListener('submit', handleBalanceUpdate);

    document.getElementById('playerSearch').addEventListener('input', (e) => filterPlayers(e.target.value));
    document.getElementById('transactionFilter').addEventListener('change', (e) => filterTransactions(e.target.value));

    document.getElementById('toggleApiKey').addEventListener('click', () => {
        const input = document.getElementById('apiKeyDisplay');
        input.type = input.type === 'password' ? 'text' : 'password';
    });

    document.getElementById('copyApiKey').addEventListener('click', () => {
        const input = document.getElementById('apiKeyDisplay');
        input.select();
        document.execCommand('copy');
        showToast('API anahtarı kopyalandı!');
    });

    playerModal.addEventListener('click', (e) => {
        if (e.target === playerModal) playerModal.classList.remove('active');
    });

    // Config form submit
    document.getElementById('configForm').addEventListener('submit', handleConfigUpdate);
}

// Auth Functions
async function handleLogin(e) {
    e.preventDefault();
    loginError.textContent = '';

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (data.success) {
            authToken = data.token;
            apiKey = data.apiKey;
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('apiKey', apiKey);
            document.getElementById('adminUsername').textContent = data.username;
            document.getElementById('apiKeyDisplay').value = apiKey;
            showMainContent();
            loadDashboard();
            showToast('Giriş başarılı!');
        } else {
            loginError.textContent = data.error || 'Giriş başarısız!';
        }
    } catch (error) {
        loginError.textContent = 'Bağlantı hatası!';
    }
}

async function handleLogout() {
    try {
        await fetch(`${API_BASE}/auth/logout`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
    } catch (error) {}

    authToken = null;
    apiKey = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('apiKey');
    showLogin();
}

function showLogin() {
    loginModal.classList.add('active');
    mainContainer.classList.add('hidden');
}

function showMainContent() {
    loginModal.classList.remove('active');
    mainContainer.classList.remove('hidden');
    document.getElementById('apiKeyDisplay').value = apiKey || '';
}

// API Functions
async function apiRequest(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`,
        'X-API-Key': apiKey
    };

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers: { ...headers, ...options.headers }
        });

        if (response.status === 401) {
            handleLogout();
            return null;
        }

        return await response.json();
    } catch (error) {
        showToast('API hatası!', true);
        return null;
    }
}

// Navigation
function navigateTo(page) {
    navItems.forEach(item => item.classList.toggle('active', item.dataset.page === page));
    pages.forEach(p => p.classList.toggle('active', p.id === `${page}Page`));

    const titles = {
        dashboard: 'Dashboard',
        players: 'Oyuncular',
        transactions: 'İşlemler',
        leaderboard: 'Sıralama',
        settings: 'Ayarlar'
    };

    pageTitle.textContent = titles[page] || 'Dashboard';
    sidebar.classList.remove('open');

    switch (page) {
        case 'dashboard': loadDashboard(); break;
        case 'players': loadPlayers(); break;
        case 'transactions': loadTransactions(); break;
        case 'leaderboard': loadLeaderboard(); break;
        case 'settings': loadSettings(); break;
    }
}

// Dashboard
async function loadDashboard() {
    const stats = await apiRequest('/stats');
    const transactions = await apiRequest('/transactions?limit=10');
    const leaderboard = await apiRequest('/leaderboard?limit=5');

    if (stats) {
        currencySymbol = stats.currencySymbol || '$';
        document.getElementById('totalPlayers').textContent = formatNumber(stats.totalPlayers);
        document.getElementById('totalMoney').textContent = formatMoney(stats.totalMoney);
        document.getElementById('totalTransactions').textContent = formatNumber(stats.totalTransactions);
        document.getElementById('onlinePlayers').textContent = stats.onlinePlayers;
        document.getElementById('onlineCount').textContent = stats.onlinePlayers;
    }

    if (transactions && transactions.transactions) {
        renderRecentTransactions(transactions.transactions);
    }

    if (leaderboard && leaderboard.leaderboard) {
        renderTopPlayers(leaderboard.leaderboard);
    }
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('recentTransactions');
    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td>${formatDate(t.createdAt)}</td>
            <td>${t.senderName || 'Sistem'}</td>
            <td>${t.receiverName || 'Sistem'}</td>
            <td class="text-success">${formatMoney(t.amount)}</td>
            <td><span class="status-badge">${t.type}</span></td>
        </tr>
    `).join('');
}

function renderTopPlayers(players) {
    const list = document.getElementById('topPlayersList');
    list.innerHTML = players.map((p, i) => {
        const rankClass = i === 0 ? 'gold' : i === 1 ? 'silver' : i === 2 ? 'bronze' : 'default';
        return `
            <li>
                <span class="rank ${rankClass}">${p.rank}</span>
                <span class="player-name">${p.username}</span>
                <span class="player-balance">${formatMoney(p.balance)}</span>
            </li>
        `;
    }).join('');
}

// Players
let allPlayers = [];

async function loadPlayers() {
    const data = await apiRequest('/players');
    if (data && data.players) {
        allPlayers = data.players;
        renderPlayers(allPlayers);
    }
}

function renderPlayers(players) {
    const tbody = document.getElementById('playersList');
    tbody.innerHTML = players.map(p => `
        <tr>
            <td>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <img src="https://mc-heads.net/avatar/${p.uuid}/32" style="border-radius: 4px;">
                    ${p.username}
                </div>
            </td>
            <td>${formatMoney(p.balance)}</td>
            <td>
                <span class="status-badge ${p.online ? 'online' : 'offline'}">
                    <span class="status-dot ${p.online ? 'online' : ''}"></span>
                    ${p.online ? 'Online' : 'Offline'}
                </span>
            </td>
            <td>
                <button class="btn btn-primary btn-sm" onclick="openPlayerModal('${p.uuid}')">
                    <i class="fas fa-edit"></i> Düzenle
                </button>
            </td>
        </tr>
    `).join('');
}

function filterPlayers(query) {
    const filtered = allPlayers.filter(p => p.username.toLowerCase().includes(query.toLowerCase()));
    renderPlayers(filtered);
}

async function openPlayerModal(uuid) {
    const player = await apiRequest(`/players/${uuid}`);
    if (!player) return;

    document.getElementById('playerAvatar').src = `https://mc-heads.net/avatar/${uuid}/80`;
    document.getElementById('playerModalName').textContent = player.username;
    document.getElementById('playerModalUUID').textContent = uuid;
    document.getElementById('playerModalBalance').textContent = formatMoney(player.balance);
    document.getElementById('editPlayerUUID').value = uuid;
    document.getElementById('balanceAmount').value = '';

    const tbody = document.getElementById('playerTransactionsList');
    if (player.transactions && player.transactions.length > 0) {
        tbody.innerHTML = player.transactions.slice(0, 10).map(t => `
            <tr>
                <td>${formatDate(t.createdAt)}</td>
                <td>${t.type}</td>
                <td>${formatMoney(t.amount)}</td>
            </tr>
        `).join('');
    } else {
        tbody.innerHTML = '<tr><td colspan="3">İşlem bulunamadı</td></tr>';
    }

    playerModal.classList.add('active');
}

async function handleBalanceUpdate(e) {
    e.preventDefault();

    const uuid = document.getElementById('editPlayerUUID').value;
    const action = document.getElementById('balanceAction').value;
    const amount = parseFloat(document.getElementById('balanceAmount').value);

    if (!amount || amount < 0) {
        showToast('Geçersiz miktar!', true);
        return;
    }

    const result = await apiRequest(`/players/${uuid}/balance`, {
        method: 'PUT',
        body: JSON.stringify({ action, amount })
    });

    if (result && result.success) {
        showToast(result.message);
        document.getElementById('playerModalBalance').textContent = formatMoney(result.newBalance);
        loadPlayers();
        loadDashboard();
    } else {
        showToast(result?.error || 'İşlem başarısız!', true);
    }
}

// Transactions
let allTransactions = [];

async function loadTransactions() {
    const data = await apiRequest('/transactions?limit=100');
    if (data && data.transactions) {
        allTransactions = data.transactions;
        renderTransactions(allTransactions);
    }
}

function renderTransactions(transactions) {
    const tbody = document.getElementById('transactionsList');
    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td>#${t.id}</td>
            <td>${formatDate(t.createdAt)}</td>
            <td>${t.senderName || 'Sistem'}</td>
            <td>${t.receiverName || 'Sistem'}</td>
            <td class="text-success">${formatMoney(t.amount)}</td>
            <td><span class="status-badge">${t.type}</span></td>
            <td>${t.description || '-'}</td>
        </tr>
    `).join('');
}

function filterTransactions(type) {
    if (type === 'all') {
        renderTransactions(allTransactions);
    } else {
        renderTransactions(allTransactions.filter(t => t.type.includes(type)));
    }
}

// Leaderboard
async function loadLeaderboard() {
    const data = await apiRequest('/leaderboard?limit=50');
    if (data && data.leaderboard) {
        renderFullLeaderboard(data.leaderboard);
    }
}

function renderFullLeaderboard(players) {
    const container = document.getElementById('fullLeaderboard');
    container.innerHTML = players.map((p, i) => {
        const topClass = i === 0 ? 'top-1' : i === 1 ? 'top-2' : i === 2 ? 'top-3' : '';
        return `
            <div class="leaderboard-item ${topClass}">
                <span class="rank">#${p.rank}</span>
                <div class="player-info">
                    <div class="player-avatar">
                        <img src="https://mc-heads.net/avatar/${p.uuid}/45" alt="${p.username}">
                    </div>
                    <span class="player-name">${p.username}</span>
                </div>
                <span class="player-balance">${formatMoney(p.balance)}</span>
            </div>
        `;
    }).join('');
}

// Settings
async function loadSettings() {
    const config = await apiRequest('/config');
    const server = await apiRequest('/server');

    if (config) {
        document.getElementById('configCurrencySymbol').value = config.currencySymbol || '$';
        document.getElementById('configCurrencyName').value = config.currencyName || 'Coin';
        document.getElementById('configCurrencyNamePlural').value = config.currencyNamePlural || 'Coins';
        document.getElementById('configStartingBalance').value = config.startingBalance || 100;
        document.getElementById('configMaxBalance').value = config.maxBalance || 1000000000;
        document.getElementById('configTransferTax').value = config.transferTax || 0;
        document.getElementById('configMinTransfer').value = config.minTransfer || 1;
    }

    if (server) {
        document.getElementById('serverVersion').textContent = server.version;
        document.getElementById('pluginVersion').textContent = server.pluginVersion;
        document.getElementById('maxPlayers').textContent = server.maxPlayers;
    }
}

async function handleConfigUpdate(e) {
    e.preventDefault();

    const config = {
        currencySymbol: document.getElementById('configCurrencySymbol').value,
        currencyName: document.getElementById('configCurrencyName').value,
        currencyNamePlural: document.getElementById('configCurrencyNamePlural').value,
        startingBalance: parseFloat(document.getElementById('configStartingBalance').value),
        maxBalance: parseFloat(document.getElementById('configMaxBalance').value),
        transferTax: parseFloat(document.getElementById('configTransferTax').value),
        minTransfer: parseFloat(document.getElementById('configMinTransfer').value)
    };

    const result = await apiRequest('/config', {
        method: 'PUT',
        body: JSON.stringify(config)
    });

    if (result && result.success) {
        showToast(result.message || 'Ayarlar kaydedildi!');
        currencySymbol = config.currencySymbol;
        loadDashboard();
    } else {
        showToast(result?.error || 'Ayarlar kaydedilemedi!', true);
    }
}

// Utility Functions
function formatMoney(amount) {
    return currencySymbol + new Intl.NumberFormat('tr-TR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount || 0);
}

function formatNumber(num) {
    return new Intl.NumberFormat('tr-TR').format(num || 0);
}

function formatDate(timestamp) {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleString('tr-TR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showToast(message, isError = false) {
    const toastEl = document.getElementById('toast');
    const icon = toastEl.querySelector('.toast-icon');
    const msg = toastEl.querySelector('.toast-message');

    toastEl.classList.toggle('error', isError);
    icon.className = `toast-icon fas ${isError ? 'fa-exclamation-circle' : 'fa-check-circle'}`;
    msg.textContent = message;

    toastEl.classList.add('show');
    setTimeout(() => {
        toastEl.classList.remove('show');
    }, 3000);
}

// Auto-refresh stats every 30 seconds
setInterval(() => {
    if (authToken && document.getElementById('dashboardPage').classList.contains('active')) {
        loadDashboard();
    }
}, 30000);
