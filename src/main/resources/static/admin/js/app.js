/* ===== Synapse Admin SPA ===== */
(function () {
    'use strict';

    // ========== i18n ==========
    const LANG = {
        en: {
            'login.subtitle': 'Management Console',
            'login.password': 'Password',
            'login.submit': 'Sign In',
            'login.error': 'Invalid password',
            'nav.dashboard': 'Dashboard',
            'nav.config': 'Configuration',
            'nav.graph': 'Memory Graph',
            'nav.search': 'Search Test',
            'nav.embed': 'Embed Test',
            'nav.memory': 'Memory',
            'nav.logout': 'Logout',
            'dashboard.title': 'Dashboard',
            'dashboard.status': 'Status',
            'dashboard.dimension': 'Dimension',
            'dashboard.totalChats': 'Chats',
            'dashboard.totalEmbeddings': 'Embeddings',
            'dashboard.embeddingProvider': 'Embedding Provider',
            'dashboard.rerankProvider': 'Rerank Provider',
            'dashboard.chatList': 'Chat List',
            'dashboard.embeddings': 'Embeddings',
            'dashboard.graphEdges': 'Graph Edges',
            'config.title': 'Configuration',
            'config.save': 'Save Changes',
            'config.detect': 'Detect Dimension',
            'config.saved': 'Configuration saved',
            'config.provider': 'Embedding Provider',
            'config.rerank': 'Rerank Provider',
            'config.slidingWindow': 'Sliding Window',
            'config.chunk': 'Chunking',
            'config.graphSection': 'Memory Graph',
            'config.storage': 'Storage',
            'field.type': 'Type',
            'field.baseUrl': 'Base URL',
            'field.model': 'Model',
            'field.apiKey': 'API Key',
            'field.windowSize': 'Window Size',
            'field.separator': 'Separator',
            'field.enabled': 'Enabled',
            'field.maxLength': 'Max Length',
            'field.decayFactor': 'Decay Factor',
            'field.pruneThreshold': 'Prune Threshold',
            'field.queryThreshold': 'Query Threshold',
            'field.maxResults': 'Max Results',
            'field.decayCron': 'Decay Cron',
            'field.basePath': 'Base Path',
            'field.vectorSuffix': 'Vector Suffix',
            'models.fetch': 'Fetch Models',
            'models.fetching': 'Fetching...',
            'models.fetched': 'Found {count} models',
            'models.error': 'Failed to fetch models',
            'models.select': '— Select a model —',
            'graph.title': 'Memory Graph',
            'graph.selectChat': 'Select Chat',
            'graph.noChats': 'No chats available',
            'graph.load': 'Load',
            'graph.decay': 'Decay',
            'graph.weight': 'Weight',
            'graph.actions': 'Actions',
            'graph.edgeDetails': 'Edge Details',
            'graph.edgeList': 'Edge List',
            'graph.placeholder': 'Select a chat and click Load to visualize the memory graph',
            'graph.weaken': 'Weaken',
            'graph.decayed': 'Decay completed',
            'graph.weakened': 'Edge weakened',
            'graph.noEdges': 'No edges found',
            'search.title': 'Search Test',
            'search.query': 'Query',
            'search.queryPlaceholder': 'Enter search text...',
            'search.nearby': 'Nearby',
            'search.useGraph': 'Use Graph',
            'search.useRerank': 'Use Rerank',
            'search.run': 'Search',
            'search.noResults': 'No results found',
            'search.score': 'Score',
            'search.windowId': 'Window',
            'embed.title': 'Embedding Test',
            'embed.singleTitle': 'Quick Text Vectorize',
            'embed.inputPlaceholder': 'Enter text to vectorize...',
            'embed.runSingle': 'Vectorize Text',
            'embed.multiTitle': 'Message Vectorization',
            'embed.slidingWindow': 'Sliding Window',
            'embed.windowSize': 'Window Size',
            'embed.addMessage': '+ Add Message',
            'embed.runMulti': 'Vectorize Messages',
            'embed.dimensions': 'Dimensions',
            'embed.msgIds': 'Message IDs',
            'embed.vectorPreview': 'Vector (click to expand)',
            'embed.noText': 'Please enter text',
            'embed.noMessages': 'Please add at least one message',
            'embed.noChatId': 'Chat ID is required',
            'embed.success': 'Vectorization complete',
            'embed.contentPlaceholder': 'Message content...',
            'memory.title': 'Memory Management',
            'memory.selectChat': 'Select Chat',
            'memory.load': 'Load',
            'memory.deleteChat': 'Delete Chat',
            'memory.totalEmbeddings': 'Total Embeddings',
            'memory.confirmDelete': 'Delete this embedding?',
            'memory.confirmDeleteChat': 'Delete ALL data for this chat? This cannot be undone.',
            'memory.deleted': 'Embedding deleted',
            'memory.chatDeleted': 'Chat deleted',
            'memory.noEmbeddings': 'No embeddings found',
            'memory.createdAt': 'Created',
            'confirm.yes': 'Delete',
            'confirm.cancel': 'Cancel',
            'common.noData': 'No data',
            'common.running': 'Running',
            'common.configured': 'Configured',
            'common.notConfigured': 'Not Configured',
            'common.error': 'An error occurred',
            'password.title': 'Admin Password',
            'password.current': 'Current Password',
            'password.new': 'New Password',
            'password.confirm': 'Confirm Password',
            'password.save': 'Update Password',
            'password.clear': 'Remove Password',
            'password.saved': 'Password updated',
            'password.cleared': 'Password removed, no authentication required',
            'password.mismatch': 'Passwords do not match',
            'password.currentRequired': 'Current password is required',
            'password.hint': 'Leave empty to disable authentication',
        },
        zh: {
            'login.subtitle': '管理控制台',
            'login.password': '密码',
            'login.submit': '登 录',
            'login.error': '密码错误',
            'nav.dashboard': '仪表盘',
            'nav.config': '配置管理',
            'nav.graph': '关联图谱',
            'nav.search': '搜索测试',
            'nav.embed': '向量化测试',
            'nav.memory': '记忆管理',
            'nav.logout': '退出',
            'dashboard.title': '仪表盘',
            'dashboard.status': '状态',
            'dashboard.dimension': '向量维度',
            'dashboard.totalChats': '会话数',
            'dashboard.totalEmbeddings': '向量总数',
            'dashboard.embeddingProvider': '嵌入模型',
            'dashboard.rerankProvider': '重排模型',
            'dashboard.chatList': '会话列表',
            'dashboard.embeddings': '向量数',
            'dashboard.graphEdges': '图边数',
            'config.title': '配置管理',
            'config.save': '保存修改',
            'config.detect': '检测维度',
            'config.saved': '配置已保存',
            'config.provider': '嵌入模型',
            'config.rerank': '重排模型',
            'config.slidingWindow': '滑动窗口',
            'config.chunk': '分块设置',
            'config.graphSection': '记忆关联图',
            'config.storage': '存储设置',
            'field.type': '类型',
            'field.baseUrl': '接口地址',
            'field.model': '模型',
            'field.apiKey': 'API 密钥',
            'field.windowSize': '窗口大小',
            'field.separator': '分隔符',
            'field.enabled': '启用',
            'field.maxLength': '最大长度',
            'field.decayFactor': '衰减系数',
            'field.pruneThreshold': '修剪阈值',
            'field.queryThreshold': '查询阈值',
            'field.maxResults': '最大结果数',
            'field.decayCron': '衰减定时',
            'field.basePath': '存储路径',
            'field.vectorSuffix': '向量文件后缀',
            'models.fetch': '获取模型',
            'models.fetching': '获取中…',
            'models.fetched': '找到 {count} 个模型',
            'models.error': '获取模型失败',
            'models.select': '— 请选择模型 —',
            'graph.title': '记忆关联图',
            'graph.selectChat': '选择会话',
            'graph.noChats': '暂无会话',
            'graph.load': '加载',
            'graph.decay': '衰减',
            'graph.weight': '权重',
            'graph.actions': '操作',
            'graph.edgeDetails': '边详情',
            'graph.edgeList': '边列表',
            'graph.placeholder': '选择一个会话并点击"加载"以可视化关联图谱',
            'graph.weaken': '削弱',
            'graph.decayed': '衰减完成',
            'graph.weakened': '边已削弱',
            'graph.noEdges': '未找到边',
            'search.title': '搜索测试',
            'search.query': '查询文本',
            'search.queryPlaceholder': '请输入搜索文本…',
            'search.nearby': '附近消息',
            'search.useGraph': '图关联',
            'search.useRerank': '重排序',
            'search.run': '搜 索',
            'search.noResults': '未找到结果',
            'search.score': '分数',
            'search.windowId': '窗口',
            'embed.title': '向量化测试',
            'embed.singleTitle': '快速文本向量化',
            'embed.inputPlaceholder': '请输入要向量化的文本…',
            'embed.runSingle': '向量化文本',
            'embed.multiTitle': '消息向量化',
            'embed.slidingWindow': '滑动窗口',
            'embed.windowSize': '窗口大小',
            'embed.addMessage': '+ 添加消息',
            'embed.runMulti': '向量化消息',
            'embed.dimensions': '维度',
            'embed.msgIds': '消息ID',
            'embed.vectorPreview': '向量（点击展开）',
            'embed.noText': '请输入文本',
            'embed.noMessages': '请至少添加一条消息',
            'embed.noChatId': '请填写 Chat ID',
            'embed.success': '向量化完成',
            'embed.contentPlaceholder': '消息内容…',
            'memory.title': '记忆管理',
            'memory.selectChat': '选择会话',
            'memory.load': '加载',
            'memory.deleteChat': '删除会话',
            'memory.totalEmbeddings': '嵌入总数',
            'memory.confirmDelete': '确认删除此嵌入记录？',
            'memory.confirmDeleteChat': '确认删除该会话的所有数据？此操作不可恢复。',
            'memory.deleted': '嵌入已删除',
            'memory.chatDeleted': '会话已删除',
            'memory.noEmbeddings': '未找到嵌入记录',
            'memory.createdAt': '创建时间',
            'confirm.yes': '删 除',
            'confirm.cancel': '取 消',
            'common.noData': '暂无数据',
            'common.running': '运行中',
            'common.configured': '已配置',
            'common.notConfigured': '未配置',
            'common.error': '发生错误',
            'password.title': '管理密码',
            'password.current': '当前密码',
            'password.new': '新密码',
            'password.confirm': '确认密码',
            'password.save': '修改密码',
            'password.clear': '移除密码',
            'password.saved': '密码已更新',
            'password.cleared': '密码已移除，无需认证',
            'password.mismatch': '两次密码不一致',
            'password.currentRequired': '请输入当前密码',
            'password.hint': '留空则无需认证',
        }
    };

    let currentLang = localStorage.getItem('synapse_lang') || 'en';

    function t(key) { return (LANG[currentLang] && LANG[currentLang][key]) || key; }

    function applyI18n() {
        document.querySelectorAll('[data-i18n]').forEach(el => {
            el.textContent = t(el.dataset.i18n);
        });
        document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            el.placeholder = t(el.dataset.i18nPlaceholder);
        });
        document.getElementById('lang-label').textContent = currentLang.toUpperCase();
    }

    function toggleLang() {
        currentLang = currentLang === 'en' ? 'zh' : 'en';
        localStorage.setItem('synapse_lang', currentLang);
        applyI18n();
        // Re-render dynamic content
        const hash = location.hash.slice(1) || 'dashboard';
        if (hash === 'config') renderConfigPage();
    }

    // ========== API ==========
    const API_BASE = location.origin;

    async function api(path, opts = {}) {
        const url = API_BASE + path;
        const config = {
            headers: { 'Content-Type': 'application/json' },
            ...opts
        };
        const resp = await fetch(url, config);
        if (resp.status === 401) {
            showLogin();
            throw new Error('Unauthorized');
        }
        if (!resp.ok) {
            const errText = await resp.text();
            let errMsg = `HTTP ${resp.status}`;
            try {
                const errJson = JSON.parse(errText);
                errMsg = errJson.error || errJson.message || errText || errMsg;
            } catch {
                errMsg = errText || errMsg;
            }
            throw new Error(errMsg);
        }
        const text = await resp.text();
        return text ? JSON.parse(text) : {};
    }

    // ========== Toast ==========
    function toast(msg, type = 'info') {
        const container = document.getElementById('toast-container');
        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.textContent = msg;
        container.appendChild(el);
        setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 300); }, 3000);
    }

    // ========== Auth ==========
    function showLogin() {
        document.getElementById('login-screen').classList.remove('hidden');
        document.getElementById('app').classList.add('hidden');
    }

    function showApp() {
        document.getElementById('login-screen').classList.add('hidden');
        document.getElementById('app').classList.remove('hidden');
    }

    async function checkAuth() {
        try {
            const data = await api('/api/admin/check');
            if (data.authenticated) {
                showApp();
                navigate(location.hash.slice(1) || 'dashboard');
            } else if (data.required) {
                showLogin();
            } else {
                showApp();
                navigate(location.hash.slice(1) || 'dashboard');
            }
        } catch {
            showLogin();
        }
    }

    document.getElementById('login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const pw = document.getElementById('login-password').value;
        const errEl = document.getElementById('login-error');
        try {
            const data = await api('/api/admin/login', {
                method: 'POST',
                body: JSON.stringify({ password: pw })
            });
            if (data.success) {
                errEl.classList.add('hidden');
                showApp();
                navigate('dashboard');
            }
        } catch {
            errEl.textContent = t('login.error');
            errEl.classList.remove('hidden');
        }
    });

    document.getElementById('btn-logout').addEventListener('click', async () => {
        try { await api('/api/admin/logout', { method: 'POST' }); } catch { /* ignore */ }
        showLogin();
    });

    // ========== Router ==========
    const pages = ['dashboard', 'config', 'graph', 'search', 'embed', 'memory'];

    function navigate(page) {
        if (!pages.includes(page)) page = 'dashboard';
        location.hash = '#' + page;

        pages.forEach(p => {
            document.getElementById('page-' + p).classList.toggle('hidden', p !== page);
        });
        document.querySelectorAll('.nav-item').forEach(el => {
            el.classList.toggle('active', el.dataset.page === page);
        });

        // Load page data
        switch (page) {
            case 'dashboard': loadDashboard(); break;
            case 'config': loadConfig(); break;
            case 'graph': loadGraphChatList(); break;
            case 'search': loadSearchChatList(); break;
            case 'embed': initEmbedPage(); break;
            case 'memory': loadMemoryChatList(); break;
        }
    }

    window.addEventListener('hashchange', () => {
        const page = location.hash.slice(1) || 'dashboard';
        navigate(page);
    });

    // ========== Dashboard ==========
    let statsCache = null;

    async function loadDashboard() {
        try {
            const data = await api('/api/admin/stats');
            statsCache = data;
            document.getElementById('stat-status').textContent = t('common.running');
            document.getElementById('stat-dimension').textContent = data.dimension ?? '—';
            document.getElementById('stat-chats').textContent = data.totalChats ?? 0;
            document.getElementById('stat-embeddings').textContent = data.totalEmbeddings ?? 0;

            renderProviderCard('provider-info', data.provider);
            renderProviderCard('rerank-info', data.rerank);
            renderChatTable(data.chats || []);
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    function renderProviderCard(id, info) {
        const container = document.getElementById(id);
        if (!info) { container.innerHTML = '—'; return; }
        const dotClass = info.configured ? 'active' : 'inactive';
        const statusText = info.configured ? t('common.configured') : t('common.notConfigured');
        container.innerHTML = `
            <div><span class="status-dot ${dotClass}"></span><span class="value">${statusText}</span></div>
            <div><span class="label">Type: </span><span class="value">${info.type || '—'}</span></div>
            <div><span class="label">Model: </span><span class="value">${info.model || '—'}</span></div>
            <div><span class="label">URL: </span><span class="value">${info.baseUrl || '—'}</span></div>
        `;
    }

    function renderChatTable(chats) {
        const tbody = document.getElementById('chat-table-body');
        if (!chats.length) {
            tbody.innerHTML = `<tr><td colspan="3" class="empty">${t('common.noData')}</td></tr>`;
            return;
        }
        tbody.innerHTML = chats.map(c => `
            <tr>
                <td><code>${c.chatId}</code></td>
                <td>${c.embeddingCount}</td>
                <td>${c.graphEdgeCount}</td>
            </tr>
        `).join('');
    }

    // ========== Config ==========
    const CONFIG_SECTIONS = [
        {
            key: 'config.provider', icon: '🔗', fields: [
                { key: 'provider.type', labelKey: 'field.type', type: 'select', options: ['api', 'local'] },
                { key: 'provider.baseUrl', labelKey: 'field.baseUrl', type: 'text' },
                { key: 'provider.model', labelKey: 'field.model', type: 'text', fetchBtn: 'provider' },
                { key: 'provider.apiKey', labelKey: 'field.apiKey', type: 'password' },
            ]
        },
        {
            key: 'config.rerank', icon: '🔀', fields: [
                { key: 'rerank.baseUrl', labelKey: 'field.baseUrl', type: 'text' },
                { key: 'rerank.model', labelKey: 'field.model', type: 'text', fetchBtn: 'rerank' },
                { key: 'rerank.apiKey', labelKey: 'field.apiKey', type: 'password' },
            ]
        },
        {
            key: 'config.slidingWindow', icon: '📐', fields: [
                { key: 'slidingWindow.size', labelKey: 'field.windowSize', type: 'number' },
                { key: 'slidingWindow.separator', labelKey: 'field.separator', type: 'text' },
            ]
        },
        {
            key: 'config.chunk', icon: '📦', fields: [
                { key: 'chunk.enabled', labelKey: 'field.enabled', type: 'checkbox' },
                { key: 'chunk.maxLength', labelKey: 'field.maxLength', type: 'number' },
            ]
        },
        {
            key: 'config.graphSection', icon: '🕸️', fields: [
                { key: 'graph.enabled', labelKey: 'field.enabled', type: 'checkbox' },
                { key: 'graph.decayFactor', labelKey: 'field.decayFactor', type: 'number', step: '0.01' },
                { key: 'graph.pruneThreshold', labelKey: 'field.pruneThreshold', type: 'number', step: '0.001' },
                { key: 'graph.queryThreshold', labelKey: 'field.queryThreshold', type: 'number', step: '0.1' },
                { key: 'graph.maxGraphResults', labelKey: 'field.maxResults', type: 'number' },
                { key: 'graph.decayCron', labelKey: 'field.decayCron', type: 'text' },
            ]
        },
        {
            key: 'config.storage', icon: '💾', fields: [
                { key: 'storage.basePath', labelKey: 'field.basePath', type: 'text' },
                { key: 'storage.vectorFileSuffix', labelKey: 'field.vectorSuffix', type: 'text' },
            ]
        },
    ];

    let configData = {};

    async function loadConfig() {
        try {
            configData = await api('/api/v1/config');
            renderConfigPage();
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    function renderConfigPage() {
        const container = document.getElementById('config-form-container');
        let html = '';

        for (const section of CONFIG_SECTIONS) {
            html += `<div class="config-section"><h3>${section.icon} ${t(section.key)}</h3><div class="form-grid">`;
            for (const field of section.fields) {
                const val = configData[field.key] ?? '';
                const label = t(field.labelKey);
                if (field.type === 'checkbox') {
                    const checked = val === true || val === 'true' ? 'checked' : '';
                    html += `<div class="form-group"><label class="check-label"><input type="checkbox" id="cfg-${field.key}" ${checked}> ${label}</label></div>`;
                } else if (field.type === 'select') {
                    const opts = field.options.map(o => `<option value="${o}" ${val === o ? 'selected' : ''}>${o}</option>`).join('');
                    html += `<div class="form-group"><label>${label}</label><select id="cfg-${field.key}" class="form-select">${opts}</select></div>`;
                } else {
                    const step = field.step ? `step="${field.step}"` : '';
                    const displayVal = field.type === 'password' ? '' : val;
                    const placeholder = field.type === 'password' && val ? val : '';
                    html += `<div class="form-group"><label>${label}</label><input type="${field.type}" id="cfg-${field.key}" value="${displayVal}" placeholder="${placeholder}" ${step}></div>`;
                }
                // Add fetch button for model fields
                if (field.fetchBtn) {
                    html += `<div class="form-group" style="align-self:flex-end"><button class="btn btn-outline btn-sm" data-fetch-role="${field.fetchBtn}" style="margin-bottom:0.1rem">${t('models.fetch')}</button></div>`;
                }
            }
            html += '</div></div>';
        }

        html += `<div class="config-section">
            <h3>🔒 ${t('password.title')}</h3>
            <p style="color:var(--text-muted);font-size:0.85rem;margin-bottom:1rem">${t('password.hint')}</p>
            <div class="form-grid">
                <div class="form-group" id="pw-current-group">
                    <label>${t('password.current')}</label>
                    <input type="password" id="pw-current" autocomplete="current-password">
                </div>
                <div class="form-group">
                    <label>${t('password.new')}</label>
                    <input type="password" id="pw-new" autocomplete="new-password">
                </div>
                <div class="form-group">
                    <label>${t('password.confirm')}</label>
                    <input type="password" id="pw-confirm" autocomplete="new-password">
                </div>
            </div>
            <div style="margin-top:0.75rem;display:flex;gap:0.5rem">
                <button id="btn-pw-save" class="btn btn-primary btn-sm">${t('password.save')}</button>
                <button id="btn-pw-clear" class="btn btn-outline btn-sm">${t('password.clear')}</button>
            </div>
        </div>`;

        html += `<div class="config-actions">
            <button id="btn-save-config" class="btn btn-primary">${t('config.save')}</button>
            <button id="btn-detect-dim" class="btn btn-outline">${t('config.detect')}</button>
        </div>`;

        container.innerHTML = html;

        document.getElementById('btn-save-config').addEventListener('click', saveConfig);
        document.getElementById('btn-detect-dim').addEventListener('click', detectDimension);
        document.getElementById('btn-pw-save').addEventListener('click', savePassword);
        document.getElementById('btn-pw-clear').addEventListener('click', clearPassword);

        // Lock fields when provider type is 'local'
        const typeSelect = document.getElementById('cfg-provider.type');
        if (typeSelect) {
            function syncLocalModel() {
                const isLocal = typeSelect.value === 'local';
                // Model field (might be input or select after fetch)
                const modelEl = document.getElementById('cfg-provider.model');
                const baseUrlEl = document.getElementById('cfg-provider.baseUrl');
                const apiKeyEl = document.getElementById('cfg-provider.apiKey');
                const fetchBtn = document.querySelector('[data-fetch-role="provider"]');

                if (modelEl) {
                    if (isLocal) {
                        // Replace with input if it's a select
                        if (modelEl.tagName === 'SELECT') {
                            const input = document.createElement('input');
                            input.type = 'text';
                            input.id = modelEl.id;
                            modelEl.parentElement.replaceChild(input, modelEl);
                        }
                        const el = document.getElementById('cfg-provider.model');
                        el.value = 'bge-small-zh-v15';
                        el.disabled = true;
                        el.style.opacity = '0.6';
                    } else {
                        modelEl.disabled = false;
                        modelEl.style.opacity = '1';
                    }
                }
                if (baseUrlEl) { baseUrlEl.disabled = isLocal; baseUrlEl.style.opacity = isLocal ? '0.6' : '1'; }
                if (apiKeyEl) { apiKeyEl.disabled = isLocal; apiKeyEl.style.opacity = isLocal ? '0.6' : '1'; }
                if (fetchBtn) { fetchBtn.disabled = isLocal; fetchBtn.style.opacity = isLocal ? '0.6' : '1'; }
            }
            typeSelect.addEventListener('change', syncLocalModel);
            syncLocalModel(); // apply on initial render
        }

        // Fetch models buttons
        document.querySelectorAll('[data-fetch-role]').forEach(btn => {
            btn.addEventListener('click', () => fetchModelsForRole(btn.dataset.fetchRole, btn));
        });
    }

    async function fetchModelsForRole(role, btn) {
        // Get the baseUrl and apiKey from form fields
        const baseUrlKey = role === 'provider' ? 'provider.baseUrl' : 'rerank.baseUrl';
        const apiKeyKey = role === 'provider' ? 'provider.apiKey' : 'rerank.apiKey';
        const modelKey = role === 'provider' ? 'provider.model' : 'rerank.model';

        const baseUrlEl = document.getElementById('cfg-' + baseUrlKey);
        const apiKeyEl = document.getElementById('cfg-' + apiKeyKey);
        const modelEl = document.getElementById('cfg-' + modelKey);

        const baseUrl = baseUrlEl ? baseUrlEl.value.trim() : '';
        // For apiKey, if input is empty, use the value from configData (might be masked)
        let apiKey = apiKeyEl ? apiKeyEl.value.trim() : '';

        if (!baseUrl) {
            toast(t('field.baseUrl') + ' required', 'error');
            return;
        }

        const origText = btn.textContent;
        btn.disabled = true;
        btn.textContent = t('models.fetching');

        try {
            const data = await api('/api/admin/models', {
                method: 'POST',
                body: JSON.stringify({ baseUrl, apiKey, role })
            });

            const models = data.models || [];
            if (models.length === 0) {
                toast(t('models.error'), 'error');
                return;
            }

            toast(t('models.fetched').replace('{count}', models.length), 'success');

            // Replace the text input with a select dropdown
            const currentModel = modelEl.value || configData[modelKey] || '';
            const parent = modelEl.parentElement;
            const selectEl = document.createElement('select');
            selectEl.id = modelEl.id;
            selectEl.className = 'form-select';

            // Add placeholder option
            selectEl.innerHTML = `<option value="">${t('models.select')}</option>` +
                models.map(m => `<option value="${m}" ${m === currentModel ? 'selected' : ''}>${m}</option>`).join('');

            parent.replaceChild(selectEl, modelEl);
        } catch (e) {
            toast(t('models.error') + ': ' + e.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = origText;
        }
    }

    async function saveConfig() {
        const updates = {};
        for (const section of CONFIG_SECTIONS) {
            for (const field of section.fields) {
                const el = document.getElementById('cfg-' + field.key);
                if (!el) continue;
                let val;
                if (field.type === 'checkbox') {
                    val = el.checked;
                } else if (field.type === 'number') {
                    val = el.value !== '' ? Number(el.value) : undefined;
                } else if (field.type === 'password') {
                    if (el.value === '') continue; // Don't send empty password fields (keep existing)
                    val = el.value;
                } else {
                    val = el.value;
                }
                // provider.type 始终发送（即使与 configData 相同），因为重启后 configData 会重置为默认值 "api"
                const alwaysSend = field.key === 'provider.type';
                if (val !== undefined && (alwaysSend || val !== configData[field.key])) {
                    updates[field.key] = val;
                }
            }
        }

        if (Object.keys(updates).length === 0) {
            toast(t('config.saved'), 'info');
            return;
        }

        try {
            const resp = await api('/api/v1/config', {
                method: 'PATCH',
                body: JSON.stringify(updates)
            });
            configData = resp.config || configData;
            renderConfigPage();
            toast(t('config.saved'), 'success');
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    async function detectDimension() {
        try {
            const resp = await api('/api/v1/config/detect-dimension', { method: 'POST' });
            configData = resp.config || configData;
            renderConfigPage();
            toast('Dimension: ' + resp.detectedDimension, 'success');
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    async function savePassword() {
        const currentPw = document.getElementById('pw-current').value;
        const newPw = document.getElementById('pw-new').value;
        const confirmPw = document.getElementById('pw-confirm').value;

        if (newPw !== confirmPw) {
            toast(t('password.mismatch'), 'error');
            return;
        }

        try {
            await api('/api/admin/password', {
                method: 'POST',
                body: JSON.stringify({ currentPassword: currentPw, newPassword: newPw })
            });
            toast(t('password.saved'), 'success');
            // Clear fields
            document.getElementById('pw-current').value = '';
            document.getElementById('pw-new').value = '';
            document.getElementById('pw-confirm').value = '';
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    async function clearPassword() {
        const currentPw = document.getElementById('pw-current').value;
        try {
            await api('/api/admin/password', {
                method: 'POST',
                body: JSON.stringify({ currentPassword: currentPw, newPassword: '' })
            });
            toast(t('password.cleared'), 'success');
            document.getElementById('pw-current').value = '';
            document.getElementById('pw-new').value = '';
            document.getElementById('pw-confirm').value = '';
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    // ========== Graph ==========
    let graphNetwork = null;
    let currentGraphChat = '';

    async function loadGraphChatList() {
        try {
            if (!statsCache) {
                statsCache = await api('/api/admin/stats');
            }
            const select = document.getElementById('graph-chat-select');
            const chats = statsCache.chats || [];
            if (chats.length === 0) {
                select.innerHTML = `<option value="">${t('graph.noChats')}</option>`;
            } else {
                select.innerHTML = chats.map(c => `<option value="${c.chatId}">${c.chatId} (${c.graphEdgeCount} edges)</option>`).join('');
            }
        } catch { /* ignore */ }
    }

    document.getElementById('btn-load-graph').addEventListener('click', async () => {
        const chatId = document.getElementById('graph-chat-select').value;
        if (!chatId) return;
        currentGraphChat = chatId;
        await loadGraphData(chatId);
    });

    document.getElementById('btn-decay').addEventListener('click', async () => {
        if (!currentGraphChat) return;
        try {
            await api(`/api/v1/chat/${currentGraphChat}/graph/decay`, { method: 'POST' });
            toast(t('graph.decayed'), 'success');
            await loadGraphData(currentGraphChat);
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    });

    async function loadGraphData(chatId) {
        try {
            const data = await api(`/api/v1/chat/${chatId}/graph`);
            const edges = data.edges || [];
            renderGraphVisualization(edges);
            renderEdgeTable(edges, chatId);
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    function renderGraphVisualization(edges) {
        const container = document.getElementById('graph-container');
        container.innerHTML = '';

        if (edges.length === 0) {
            container.innerHTML = `<p class="graph-placeholder">${t('graph.noEdges')}</p>`;
            graphNetwork = null;
            return;
        }

        // Collect nodes
        const nodeSet = new Set();
        edges.forEach(e => { nodeSet.add(e.nodeA); nodeSet.add(e.nodeB); });

        const colors = ['#a78bfa', '#60a5fa', '#34d399', '#f472b6', '#fb923c', '#fbbf24', '#f87171', '#818cf8'];
        const nodeArr = Array.from(nodeSet);
        const nodes = new vis.DataSet(nodeArr.map((id, i) => ({
            id, label: id.length > 12 ? id.substring(0, 12) + '…' : id,
            title: id,
            color: {
                background: colors[i % colors.length] + '33',
                border: colors[i % colors.length],
                highlight: { background: colors[i % colors.length] + '66', border: colors[i % colors.length] }
            },
            font: { color: '#e8eaf0', size: 11 },
            shape: 'dot', size: 16,
            borderWidth: 2
        })));

        const maxWeight = Math.max(...edges.map(e => e.weight), 1);
        const edgeData = new vis.DataSet(edges.map((e, i) => ({
            id: i,
            from: e.nodeA,
            to: e.nodeB,
            value: e.weight,
            title: `Weight: ${e.weight.toFixed(4)}`,
            label: e.weight.toFixed(2),
            width: 1 + (e.weight / maxWeight) * 4,
            color: { color: '#6366f155', highlight: '#a5b4fc' },
            font: { color: '#9ca3b8', size: 10, strokeWidth: 0 },
            smooth: { type: 'continuous' }
        })));

        const options = {
            physics: {
                solver: 'forceAtlas2Based',
                forceAtlas2Based: { gravitationalConstant: -60, springLength: 120, springConstant: 0.04 },
                stabilization: { iterations: 100 }
            },
            interaction: { hover: true, tooltipDelay: 100 },
            edges: { selectionWidth: 2 },
            layout: { improvedLayout: true }
        };

        graphNetwork = new vis.Network(container, { nodes, edges: edgeData }, options);

        graphNetwork.on('selectEdge', (params) => {
            if (params.edges.length === 1) {
                const edge = edgeData.get(params.edges[0]);
                const panel = document.getElementById('graph-edge-panel');
                panel.classList.remove('hidden');
                document.getElementById('edge-detail-content').innerHTML = `
                    <div style="margin-bottom: 0.75rem">
                        <div><span style="color:var(--text-muted)">Node A:</span> <code>${edge.from}</code></div>
                        <div><span style="color:var(--text-muted)">Node B:</span> <code>${edge.to}</code></div>
                        <div><span style="color:var(--text-muted)">Weight:</span> <strong>${edge.value.toFixed(4)}</strong></div>
                    </div>
                    <button class="btn btn-danger btn-sm" onclick="window._weakenEdge('${edge.from}','${edge.to}')">
                        ${t('graph.weaken')} (-1.0)
                    </button>
                `;
            }
        });

        graphNetwork.on('deselectEdge', () => {
            document.getElementById('graph-edge-panel').classList.add('hidden');
        });
    }

    window._weakenEdge = async function (nodeA, nodeB) {
        if (!currentGraphChat) return;
        try {
            await api(`/api/v1/chat/${currentGraphChat}/graph/weaken`, {
                method: 'POST',
                body: JSON.stringify({ nodeA, nodeB, amount: 1.0 })
            });
            toast(t('graph.weakened'), 'success');
            await loadGraphData(currentGraphChat);
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    };

    function renderEdgeTable(edges, chatId) {
        const tbody = document.getElementById('edge-table-body');
        if (!edges.length) {
            tbody.innerHTML = `<tr><td colspan="4" class="empty">${t('common.noData')}</td></tr>`;
            return;
        }
        tbody.innerHTML = edges.sort((a, b) => b.weight - a.weight).map(e => `
            <tr>
                <td><code>${e.nodeA}</code></td>
                <td><code>${e.nodeB}</code></td>
                <td><strong>${e.weight.toFixed(4)}</strong></td>
                <td><button class="btn btn-danger btn-sm" onclick="window._weakenEdge('${e.nodeA}','${e.nodeB}')">${t('graph.weaken')}</button></td>
            </tr>
        `).join('');
    }

    // ========== Search ==========
    async function loadSearchChatList() {
        try {
            if (!statsCache) {
                statsCache = await api('/api/admin/stats');
            }
            const select = document.getElementById('search-chat-select');
            const chats = statsCache.chats || [];
            if (chats.length === 0) {
                select.innerHTML = '<option value="">—</option>';
            } else {
                select.innerHTML = chats.map(c => `<option value="${c.chatId}">${c.chatId}</option>`).join('');
            }
        } catch { /* ignore */ }
    }

    document.getElementById('btn-search').addEventListener('click', async () => {
        const query = document.getElementById('search-query').value.trim();
        const chatId = document.getElementById('search-chat-select').value;
        const topK = parseInt(document.getElementById('search-topk').value) || 5;
        const nearbyCount = parseInt(document.getElementById('search-nearby').value) || 0;
        const useGraph = document.getElementById('search-use-graph').checked;
        const useRerank = document.getElementById('search-use-rerank').checked;

        if (!query || !chatId) {
            toast('Query and Chat ID are required', 'error');
            return;
        }

        const btn = document.getElementById('btn-search');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>';

        try {
            const results = await api('/api/v1/search', {
                method: 'POST',
                body: JSON.stringify({ chatId, query, topK, nearbyCount, useGraph, useRerank })
            });
            renderSearchResults(results);
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = t('search.run');
        }
    });

    function renderSearchResults(results) {
        const container = document.getElementById('search-results');
        if (!results || !results.length) {
            container.innerHTML = `<div class="card" style="text-align:center;color:var(--text-muted);padding:2rem">${t('search.noResults')}</div>`;
            return;
        }

        container.innerHTML = results.map((r, i) => {
            const badgeClass = r.matchType || 'vector';
            return `
                <div class="result-card" style="animation: fadeInUp ${0.1 + i * 0.05}s ease">
                    <div class="result-header">
                        <span class="result-score">${typeof r.score === 'number' ? r.score.toFixed(4) : '—'}</span>
                        <span class="result-badge ${badgeClass}">${r.matchType || 'vector'}</span>
                        <span class="result-meta">${t('search.windowId')}: ${r.windowId || '—'}</span>
                    </div>
                    <div class="result-content">${escapeHtml(r.content || '')}</div>
                    <div class="result-meta">
                        Message IDs: ${(r.messageIds || []).join(', ') || '—'} &nbsp;|&nbsp; Index: ${r.messageIndex ?? '—'}
                    </div>
                </div>
            `;
        }).join('');
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ========== Embed Test ==========
    let embedMsgCounter = 0;
    let embedPageInitialized = false;

    function initEmbedPage() {
        if (!embedPageInitialized) {
            addMessageRow('user', '');
            addMessageRow('assistant', '');
            embedPageInitialized = true;
        }
    }

    function addMessageRow(role = 'user', content = '') {
        const container = document.getElementById('embed-messages-container');
        const id = embedMsgCounter++;
        const div = document.createElement('div');
        div.className = 'message-row';
        div.dataset.msgId = id;
        div.innerHTML = `
            <select class="form-select msg-role">
                <option value="user" ${role === 'user' ? 'selected' : ''}>User</option>
                <option value="assistant" ${role === 'assistant' ? 'selected' : ''}>Assistant</option>
                <option value="system" ${role === 'system' ? 'selected' : ''}>System</option>
            </select>
            <textarea class="msg-content" rows="1" placeholder="${t('embed.contentPlaceholder')}">${escapeHtml(content)}</textarea>
            <button class="btn btn-danger btn-sm btn-remove-msg" title="Remove">✕</button>
        `;
        div.querySelector('.btn-remove-msg').addEventListener('click', () => {
            div.style.opacity = '0';
            setTimeout(() => div.remove(), 200);
        });
        // Auto-grow textarea
        const ta = div.querySelector('.msg-content');
        ta.addEventListener('input', () => {
            ta.style.height = 'auto';
            ta.style.height = ta.scrollHeight + 'px';
        });
        container.appendChild(div);
    }

    document.getElementById('btn-add-message').addEventListener('click', () => addMessageRow());

    document.getElementById('btn-embed-text').addEventListener('click', async () => {
        const text = document.getElementById('embed-input').value.trim();
        if (!text) { toast(t('embed.noText'), 'error'); return; }

        const btn = document.getElementById('btn-embed-text');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>';

        try {
            const result = await api('/api/v1/embed/text', {
                method: 'POST',
                body: JSON.stringify({ text })
            });
            const container = document.getElementById('embed-text-result');
            const vectorArr = result.vector || [];
            const preview = vectorArr.slice(0, 20).map(v => v.toFixed(6)).join(', ') + (vectorArr.length > 20 ? ', …' : '');
            container.innerHTML = `
                <div class="embed-result-card">
                    <div class="embed-result-header">
                        <span class="tag dim-tag">${result.dimensions || vectorArr.length} ${t('embed.dimensions')}</span>
                    </div>
                    <div class="embed-result-content">${escapeHtml(result.text || text)}</div>
                    <div class="embed-vector-preview" title="${t('embed.vectorPreview')}">[${preview}]</div>
                </div>
            `;
            toast(t('embed.success'), 'success');
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = t('embed.runSingle');
        }
    });

    document.getElementById('btn-embed-messages').addEventListener('click', async () => {
        const chatId = document.getElementById('embed-chat-id').value.trim();
        if (!chatId) { toast(t('embed.noChatId'), 'error'); return; }

        const rows = document.querySelectorAll('.message-row');
        if (rows.length === 0) { toast(t('embed.noMessages'), 'error'); return; }

        const messages = [];
        let ts = Date.now();
        rows.forEach((row, i) => {
            const role = row.querySelector('.msg-role').value;
            const content = row.querySelector('.msg-content').value.trim();
            if (content) {
                messages.push({
                    id: 'msg-' + (i + 1),
                    role,
                    content,
                    timestamp: ts + i
                });
            }
        });

        if (messages.length === 0) { toast(t('embed.noMessages'), 'error'); return; }

        const useSlidingWindow = document.getElementById('embed-sliding-window').checked;
        const windowSize = parseInt(document.getElementById('embed-window-size').value) || 2;

        const btn = document.getElementById('btn-embed-messages');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner"></span>';

        try {
            const results = await api('/api/v1/embed', {
                method: 'POST',
                body: JSON.stringify({ chatId, messages, useSlidingWindow, windowSize })
            });

            const container = document.getElementById('embed-messages-result');
            if (!results || !results.length) {
                container.innerHTML = `<div class="card" style="text-align:center;color:var(--text-muted);padding:2rem">${t('common.noData')}</div>`;
                return;
            }

            container.innerHTML = results.map((r, i) => {
                const vecLen = r.vector ? r.vector.length : 0;
                const preview = r.vector ? r.vector.slice(0, 10).map(v => v.toFixed(4)).join(', ') + (vecLen > 10 ? ', …' : '') : '—';
                const msgIds = (r.messageIds || []).join(', ');
                return `
                    <div class="embed-result-card" style="animation-delay:${i * 0.05}s">
                        <div class="embed-result-header">
                            <span class="tag">${r.windowId || '—'}</span>
                            <span class="tag dim-tag">${vecLen} ${t('embed.dimensions')}</span>
                        </div>
                        <div class="embed-result-content">${escapeHtml(r.content || '')}</div>
                        <div class="embed-result-meta">${t('embed.msgIds')}: ${msgIds || '—'} &nbsp;|&nbsp; Index: ${r.messageIndex ?? '—'}</div>
                        <div class="embed-vector-preview" title="${t('embed.vectorPreview')}">[${preview}]</div>
                    </div>
                `;
            }).join('');

            toast(t('embed.success') + ` (${results.length})`, 'success');
            // Invalidate stats cache since new embeddings were created
            statsCache = null;
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = t('embed.runMulti');
        }
    });

    // ========== Memory Management ==========
    let currentMemoryChat = '';

    async function loadMemoryChatList() {
        try {
            if (!statsCache) {
                statsCache = await api('/api/admin/stats');
            }
            const select = document.getElementById('memory-chat-select');
            const chats = statsCache.chats || [];
            if (chats.length === 0) {
                select.innerHTML = `<option value="">${t('graph.noChats')}</option>`;
            } else {
                select.innerHTML = chats.map(c => `<option value="${c.chatId}">${c.chatId} (${c.embeddingCount})</option>`).join('');
            }
        } catch { /* ignore */ }
    }

    document.getElementById('btn-load-memory').addEventListener('click', async () => {
        const chatId = document.getElementById('memory-chat-select').value;
        if (!chatId) return;
        currentMemoryChat = chatId;
        await loadMemoryData(chatId);
    });

    async function loadMemoryData(chatId) {
        try {
            const data = await api(`/api/v1/chat/${chatId}/embeddings`);
            const embeddings = data.embeddings || [];
            renderMemoryStats(embeddings.length);
            renderMemoryList(chatId, embeddings);
        } catch (e) {
            toast(t('common.error') + ': ' + e.message, 'error');
        }
    }

    function renderMemoryStats(count) {
        const container = document.getElementById('memory-stats');
        container.innerHTML = `
            <div class="memory-stat-badge">
                <span>${t('memory.totalEmbeddings')}:</span>
                <span class="num">${count}</span>
            </div>
        `;
    }

    function renderMemoryList(chatId, embeddings) {
        const container = document.getElementById('memory-list');
        if (!embeddings.length) {
            container.innerHTML = `<div class="card" style="text-align:center;color:var(--text-muted);padding:2rem">${t('memory.noEmbeddings')}</div>`;
            return;
        }
        container.innerHTML = embeddings.map((e, i) => `
            <div class="memory-card" style="animation-delay:${i * 0.03}s">
                <div class="memory-card-header">
                    <span class="window-id">${escapeHtml(e.windowId || '—')}</span>
                    <span class="msg-index">#${e.messageIndex ?? '—'}</span>
                </div>
                <div class="memory-card-content">${escapeHtml(e.content || '')}</div>
                <div class="memory-card-footer">
                    <span>${t('memory.createdAt')}: ${e.createdAt || '—'} &nbsp;|&nbsp; IDs: ${e.messageIds || '—'}</span>
                    <button class="btn btn-danger btn-sm" onclick="window._deleteEmbedding('${chatId}','${escapeAttr(e.windowId)}')">✕</button>
                </div>
            </div>
        `).join('');
    }

    window._deleteEmbedding = function (chatId, windowId) {
        showConfirm(t('memory.confirmDelete'), async () => {
            try {
                await api(`/api/v1/chat/${chatId}/embedding/${windowId}`, { method: 'DELETE' });
                toast(t('memory.deleted'), 'success');
                statsCache = null;
                await loadMemoryData(chatId);
            } catch (e) {
                toast(t('common.error') + ': ' + e.message, 'error');
            }
        });
    };

    document.getElementById('btn-delete-chat').addEventListener('click', () => {
        const chatId = document.getElementById('memory-chat-select').value;
        if (!chatId) return;
        showConfirm(t('memory.confirmDeleteChat'), async () => {
            try {
                await api(`/api/v1/chat/${chatId}`, { method: 'DELETE' });
                toast(t('memory.chatDeleted'), 'success');
                statsCache = null;
                document.getElementById('memory-stats').innerHTML = '';
                document.getElementById('memory-list').innerHTML = '';
                currentMemoryChat = '';
                await loadMemoryChatList();
            } catch (e) {
                toast(t('common.error') + ': ' + e.message, 'error');
            }
        });
    });

    // ========== Confirm Dialog ==========
    function showConfirm(message, onConfirm) {
        const overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = `
            <div class="confirm-dialog">
                <p>${message}</p>
                <div class="confirm-actions">
                    <button class="btn btn-outline" id="confirm-cancel">${t('confirm.cancel')}</button>
                    <button class="btn btn-danger" id="confirm-yes">${t('confirm.yes')}</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);

        overlay.querySelector('#confirm-cancel').addEventListener('click', () => overlay.remove());
        overlay.querySelector('#confirm-yes').addEventListener('click', () => {
            overlay.remove();
            onConfirm();
        });
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) overlay.remove();
        });
    }

    function escapeAttr(str) {
        return (str || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
    }

    // ========== Init ==========
    document.getElementById('btn-lang').addEventListener('click', toggleLang);

    applyI18n();
    checkAuth();
})();
