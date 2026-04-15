document.addEventListener('DOMContentLoaded', function () {
    const tbody = document.querySelector('#rulesTable tbody');
    const historyBody = document.querySelector('#historyTable tbody');
    const summary = document.getElementById('summary');
    const ruleForm = document.getElementById('ruleForm');

    function statusClass(status) {
        return 'status-' + String(status || '').toLowerCase();
    }

    function formDataFromForm(form) {
        return new URLSearchParams(new FormData(form));
    }

    function resetForm() {
        document.getElementById('ruleId').value = '';
        document.getElementById('sender').value = '';
        document.getElementById('receiver').value = '';
        document.getElementById('msgType').value = '';
        document.getElementById('description').value = '';
        document.getElementById('minExpected').value = '1';
        document.getElementById('maxExpected').value = '1';
        document.getElementById('deadline').value = '15:00';
        document.getElementById('warningMinutesBeforeDeadline').value = '60';
        document.getElementById('active').value = 'true';
    }

    async function loadDashboard() {
        const response = await fetch('api/demo-dashboard', { cache: 'no-store' });
        const data = await response.json();

        summary.textContent =
            'Totalt antal flöden idag: ' + data.totalFlowsToday +
            ' | OK: ' + data.ok +
            ' | Info: ' + data.info +
            ' | Warning: ' + data.warning +
            ' | Error: ' + data.error;

        tbody.innerHTML = '';

        data.rows.forEach(function (row) {
            const tr = document.createElement('tr');
            const ackButton = (row.status === 'WARNING' || row.status === 'ERROR')
                ? `<button class="warn" data-ack="${row.id}" data-status="${row.status}">Kvittera</button>`
                : '';

            tr.innerHTML = `
                <td>${row.sender}</td>
                <td>${row.receiver}</td>
                <td>${row.msgType}</td>
                <td class="${statusClass(row.status)}">${row.status}</td>
                <td>${row.countToday}</td>
                <td>${row.deadline}</td>
                <td>${row.description || ''}</td>
                <td>${row.message || ''}</td>
                <td>
                    <div class="actions">
                        <button class="secondary" data-edit="${row.id}">Editera</button>
                        <button class="danger" data-delete="${row.id}">Radera</button>
                        ${ackButton}
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    async function loadHistory() {
        const response = await fetch('api/history', { cache: 'no-store' });
        const rows = await response.json();

        historyBody.innerHTML = '';
        rows.slice().reverse().forEach(function (row) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${row.createdAt || ''}</td>
                <td>${row.ruleId || ''}</td>
                <td>${row.status || ''}</td>
                <td>${row.eventType || ''}</td>
                <td>${row.message || ''}</td>
                <td>${row.createdBy || ''}</td>
            `;
            historyBody.appendChild(tr);
        });
    }

    async function loadRulesForEdit(ruleId) {
        const response = await fetch('api/rules', { cache: 'no-store' });
        const rules = await response.json();
        const rule = rules.find(r => r.id === ruleId);
        if (!rule) return;

        document.getElementById('ruleId').value = rule.id || '';
        document.getElementById('sender').value = rule.sender || '';
        document.getElementById('receiver').value = rule.receiver || '';
        document.getElementById('msgType').value = rule.msgType || '';
        document.getElementById('description').value = rule.description || '';
        document.getElementById('minExpected').value = rule.minExpected || 0;
        document.getElementById('maxExpected').value = rule.maxExpected || 0;
        document.getElementById('deadline').value = rule.deadline || '15:00';
        document.getElementById('warningMinutesBeforeDeadline').value = rule.warningMinutesBeforeDeadline || 60;
        document.getElementById('active').value = String(rule.active);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    async function saveRule(event) {
        event.preventDefault();
        const id = document.getElementById('ruleId').value.trim();
        const body = formDataFromForm(ruleForm);

        const method = id ? 'PUT' : 'POST';
        const response = await fetch('api/rules', {
            method: method,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: body.toString()
        });

        if (!response.ok) {
            alert('Kunde inte spara regel');
            return;
        }

        resetForm();
        await refreshAll();
    }

    async function deleteRule(ruleId) {
        const response = await fetch('api/rules?id=' + encodeURIComponent(ruleId), {
            method: 'DELETE'
        });
        if (!response.ok) {
            alert('Kunde inte radera regel');
            return;
        }
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

        const response = await fetch('api/acknowledge', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: body.toString()
        });

        if (!response.ok) {
            alert('Kunde inte kvittera');
            return;
        }

        await refreshAll();
    }

    async function refreshAll() {
        await loadDashboard();
        await loadHistory();
    }

    ruleForm.addEventListener('submit', saveRule);

    document.getElementById('resetBtn').addEventListener('click', function () {
        resetForm();
    });

    document.addEventListener('click', async function (event) {
        const editId = event.target.getAttribute('data-edit');
        const deleteId = event.target.getAttribute('data-delete');
        const ackId = event.target.getAttribute('data-ack');
        const ackStatus = event.target.getAttribute('data-status');

        if (editId) {
            await loadRulesForEdit(editId);
        } else if (deleteId) {
            if (window.confirm('Vill du radera regeln?')) {
                await deleteRule(deleteId);
            }
        } else if (ackId) {
            await acknowledge(ackId, ackStatus);
        }
    });

    refreshAll();
    setInterval(loadDashboard, 5000);
    setInterval(loadHistory, 10000);
});
