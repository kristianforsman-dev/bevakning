document.addEventListener('DOMContentLoaded', function () {
    const API = {
        dashboard: '/api/dashboard',
        rules: '/api/rules',
        acknowledge: '/api/acknowledge',
        history: '/api/history',
        health: '/api/health',
        adminConfig: '/api/admin/config',
        exportRules: '/api/admin/rules/export',
        importRules: '/api/admin/rules/import'
    };

    const tbody = document.querySelector('#rulesTable tbody');
    const historyBody = document.querySelector('#historyTable tbody');
    const ruleForm = document.getElementById('ruleForm');
    const filterInput = document.getElementById('filterInput');

    let currentRows = [];

    function qs(id) { return document.getElementById(id); }
    function statusClass(status) { return 'status-' + String(status || '').toLowerCase(); }
    function formDataFromForm(form) { return new URLSearchParams(new FormData(form)); }

    async function fetchJson(url, options) {
        const response = await fetch(url, options || { cache: 'no-store' });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || ('HTTP ' + response.status));
        }
        return await response.json();
    }

    async function fetchText(url, options) {
        const response = await fetch(url, options || { cache: 'no-store' });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || ('HTTP ' + response.status));
        }
        return await response.text();
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function formatTimestamp(value) {
        if (!value) return '-';
        return value.replace('T', ' ').slice(0, 19);
    }

    function showBackendBanner(ok, message) {
        const banner = qs('backendBanner');
        if (ok) {
            banner.classList.add('hidden');
            banner.textContent = '';
            return;
        }
        banner.classList.remove('hidden');
        banner.textContent = 'Tekniskt backend-fel: ' + (message || 'okänt fel');
    }

    function resetForm() {
        qs('ruleId').value = '';
        qs('sender').value = '';
        qs('receiver').value = '';
        qs('msgType').value = '';
        qs('description').value = '';
        qs('scheduleType').value = 'DAILY';
        qs('minExpected').value = '1';
        qs('maxExpected').value = '1';
        qs('deadline').value = '15:00';
        qs('warningMinutesBeforeDeadline').value = '60';
        qs('active').value = 'true';
        qs('weekdays').value = '';
        qs('monthDays').value = '';
        qs('specificDates').value = '';
        qs('useHistoricalBaseline').value = 'false';
        qs('historicalDays').value = '10';
        qs('minPercentOfAverage').value = '85';
        qs('windowsSpec').value = '';
    }

    function renderDashboardRows(rows) {
        const filter = (filterInput.value || '').trim().toLowerCase();
        const filtered = rows.filter(function (row) {
            const haystack = [
                row.sender,
                row.receiver,
                row.msgType,
                row.description,
                row.scheduleType,
                row.status,
                row.message
            ].join(' ').toLowerCase();
            return !filter || haystack.includes(filter);
        });

        tbody.innerHTML = '';

        filtered.forEach(function (row) {
            const ackButton = (row.status === 'WARNING' || row.status === 'ERROR')
                ? `<button class="btn warn" data-ack="${escapeHtml(row.id)}" data-status="${escapeHtml(row.status)}">Kvittera</button>`
                : '';

            const tr = document.createElement('tr');
            tr.className = 'fade-up';
            tr.innerHTML = `
                <td class="mono">${escapeHtml(row.sender)}</td>
                <td class="mono">${escapeHtml(row.receiver)}</td>
                <td class="mono">${escapeHtml(row.msgType)}</td>
                <td class="mono">${escapeHtml(row.scheduleType || '')}</td>
                <td><span class="status-chip ${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
                <td>${escapeHtml(row.countToday)}</td>
                <td class="mono">${escapeHtml(row.deadline)}</td>
                <td>${escapeHtml(row.description || '')}</td>
                <td>${escapeHtml(row.message || '')}</td>
                <td>
                    <div class="actions">
                        <button class="btn secondary" data-edit="${escapeHtml(row.id)}">Editera</button>
                        <button class="btn danger" data-delete="${escapeHtml(row.id)}">Radera</button>
                        ${ackButton}
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    async function loadDashboard() {
        const data = await fetchJson(API.dashboard, { cache: 'no-store' });

        qs('statTotal').textContent = data.totalFlowsToday;
        qs('statOk').textContent = data.ok;
        qs('statInfo').textContent = data.info;
        qs('statWarning').textContent = data.warning;
        qs('statError').textContent = data.error;
        qs('refreshedAt').textContent = formatTimestamp(data.refreshedAt);
        showBackendBanner(data.backendOk, data.backendMessage);

        currentRows = data.rows || [];
        renderDashboardRows(currentRows);
    }

    async function loadRules() {
        return await fetchJson(API.rules, { cache: 'no-store' });
    }

    async function loadRuleForEdit(ruleId) {
        const rules = await loadRules();
        const rule = rules.find(function (r) { return r.id === ruleId; });
        if (!rule) return;

        qs('ruleId').value = rule.id || '';
        qs('sender').value = rule.sender || '';
        qs('receiver').value = rule.receiver || '';
        qs('msgType').value = rule.msgType || '';
        qs('description').value = rule.description || '';
        qs('scheduleType').value = rule.scheduleType || 'DAILY';
        qs('minExpected').value = rule.minExpected || 0;
        qs('maxExpected').value = rule.maxExpected || 0;
        qs('deadline').value = rule.deadline || '15:00';
        qs('warningMinutesBeforeDeadline').value = rule.warningMinutesBeforeDeadline || 60;
        qs('active').value = String(rule.active);
        qs('weekdays').value = rule.weekdays || '';
        qs('monthDays').value = rule.monthDays || '';
        qs('specificDates').value = rule.specificDates || '';
        qs('useHistoricalBaseline').value = String(rule.useHistoricalBaseline);
        qs('historicalDays').value = rule.historicalDays || 10;
        qs('minPercentOfAverage').value = rule.minPercentOfAverage || 85;
        qs('windowsSpec').value = (rule.windowsSpec || '').replaceAll('\\n', '\n');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    async function loadHistory() {
        const rows = await fetchJson(API.history, { cache: 'no-store' });

        historyBody.innerHTML = '';
        rows.slice().reverse().forEach(function (row) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="mono">${escapeHtml(formatTimestamp(row.createdAt || ''))}</td>
                <td class="mono">${escapeHtml(row.ruleId || '')}</td>
                <td>${escapeHtml(row.status || '')}</td>
                <td>${escapeHtml(row.eventType || '')}</td>
                <td>${escapeHtml(row.message || '')}</td>
                <td>${escapeHtml(row.createdBy || '')}</td>
            `;
            historyBody.appendChild(tr);
        });
    }

    async function loadAdminConfig() {
        const data = await fetchJson(API.adminConfig, { cache: 'no-store' });
        qs('adminConfigBox').textContent = JSON.stringify(data, null, 2);
    }

    async function exportRules() {
        const text = await fetchText(API.exportRules, { cache: 'no-store' });
        qs('rulesJsonBox').value = text;
    }

    async function importRules() {
        const body = qs('rulesJsonBox').value;
        if (!body.trim()) {
            alert('Ingen JSON att importera');
            return;
        }

        await fetchText(API.importRules, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json;charset=UTF-8' },
            body: body
        });

        await refreshAll();
        alert('Regler importerade');
    }

    async function saveRule(event) {
        event.preventDefault();
        const id = qs('ruleId').value.trim();
        const body = formDataFromForm(ruleForm);

        await fetchText(API.rules, {
            method: id ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: body.toString()
        });

        resetForm();
        await refreshAll();
    }

    async function deleteRule(ruleId) {
        await fetchText(API.rules + '?id=' + encodeURIComponent(ruleId), {
            method: 'DELETE'
        });
        await refreshAll();
    }

    async function acknowledge(ruleId, status) {
        const comment = window.prompt('Kommentar för kvittering:', '') || '';
        const acknowledgedBy = window.prompt('Ditt namn/signatur:', 'user') || 'user';

        const body = new URLSearchParams();
        body.set('ruleId', ruleId);
        body.set('status', status);
        body.set('comment', comment);
        body.set('acknowledgedBy', acknowledgedBy);

        await fetchText(API.acknowledge, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: body.toString()
        });

        await refreshAll();
    }

    async function refreshAll() {
        try {
            await loadDashboard();
            await loadHistory();
            await loadAdminConfig();
        } catch (e) {
            console.error(e);
            showBackendBanner(false, e.message || 'Kunde inte ladda data');
        }
    }

    ruleForm.addEventListener('submit', saveRule);

    qs('resetBtn').addEventListener('click', function () {
        resetForm();
    });

    qs('loadConfigBtn').addEventListener('click', loadAdminConfig);
    qs('exportRulesBtn').addEventListener('click', exportRules);
    qs('importRulesBtn').addEventListener('click', importRules);

    filterInput.addEventListener('input', function () {
        renderDashboardRows(currentRows);
    });

    document.addEventListener('click', async function (event) {
        const editId = event.target.getAttribute('data-edit');
        const deleteId = event.target.getAttribute('data-delete');
        const ackId = event.target.getAttribute('data-ack');
        const ackStatus = event.target.getAttribute('data-status');

        try {
            if (editId) {
                await loadRuleForEdit(editId);
            } else if (deleteId) {
                if (window.confirm('Vill du radera regeln?')) {
                    await deleteRule(deleteId);
                }
            } else if (ackId) {
                await acknowledge(ackId, ackStatus);
            }
        } catch (e) {
            console.error(e);
            alert('Något gick fel: ' + (e.message || 'okänt fel'));
        }
    });

    refreshAll();
    setInterval(loadDashboard, 5000);
    setInterval(loadHistory, 10000);
});
