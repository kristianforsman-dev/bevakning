document.addEventListener('DOMContentLoaded', function () {
    const contextPath = window.location.pathname.startsWith('/bevakning/') ? '/bevakning' : '';

    const API = {
        dashboard: contextPath + '/api/dashboard',
        rules: contextPath + '/api/rules',
        acknowledge: contextPath + '/api/acknowledge',
        history: contextPath + '/api/history',
        adminConfig: contextPath + '/api/admin/config',
        exportRules: contextPath + '/api/admin/rules/export',
        importRules: contextPath + '/api/admin/rules/import'
    };

    const MODE_HELP = {
        daily: 'Bevakningen gäller varje dag.',
        weekly: 'Bevakningen gäller valda veckodagar.',
        monthly: 'Bevakningen gäller valda dagar i månaden.',
        dates: 'Bevakningen gäller specifika datum.',
        multiwindow: 'Bevakningen använder flera tidsfönster.'
    };

    let currentRows = [];
    const incomingAgeEl = document.getElementById('statIncomingAge');
    const incomingTimeEl = document.getElementById('statIncomingTime');
    const outgoingAgeEl = document.getElementById('statOutgoingAge');
    const outgoingTimeEl = document.getElementById('statOutgoingTime');
    const incomingCardEl = document.getElementById('statIncomingCard');
    const outgoingCardEl = document.getElementById('statOutgoingCard');

    function setStaleCardState(card, minutes) {
        if (!card) return;
        card.classList.remove('ok', 'warn', 'error', 'stale-warn', 'stale-error');
        if (minutes == null) {
            card.classList.add('error', 'stale-error');
            return;
        }
        if (minutes > 60) {
            card.classList.add('error', 'stale-error');
            return;
        }
        if (minutes >= uiConfig.flowStaleWarningMinutes) {
            card.classList.add('warn', 'stale-warn');
            return;
        }
        card.classList.add('ok');
    }

    function setFlowSummaryPlaceholder() {
        if (incomingAgeEl) incomingAgeEl.textContent = '--';
        if (incomingTimeEl) incomingTimeEl.textContent = 'Ingen data';
        if (outgoingAgeEl) outgoingAgeEl.textContent = '--';
        if (outgoingTimeEl) outgoingTimeEl.textContent = 'Ingen data';
        setStaleCardState(incomingCardEl, null);
        setStaleCardState(outgoingCardEl, null);
    }

    setFlowSummaryPlaceholder();
    let currentRules = [];
    let currentHistory = [];
    let selectedRuleId = null;
    let editorMode = 'new'; // new | view | edit
    let selectedRuleMode = 'daily';
    let selectedSpecificDates = [];
    let currentSort = { key: 'status', direction: 'desc' };
    let acknowledgeInFlight = false;
    let formDirty = false;
        function markFormDirty(){ formDirty = true; }
    let localAckOverrides = {}; let uiConfig = { dashboardRefreshSeconds: 5, historyRefreshSeconds: 10, flowStaleWarningMinutes: 10, flowStaleErrorMinutes: 60 }; let dashboardRefreshTimer = null; let historyRefreshTimer = null;

    function qs(id) {
        return document.getElementById(id);
    }

    function qsa(selector) {
        return Array.from(document.querySelectorAll(selector));
    }

    function on(id, eventName, handler) {
        const el = qs(id);
        if (el) el.addEventListener(eventName, handler);
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

    function minutesSince(value) {
        if (!value) return null;
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return null;
        return Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000));
    }

    function formatMinutesAge(minutes) {
        if (minutes == null) return '--';
        if (minutes < 60) return minutes + ' min';
        if (minutes < 1440) {
            const hours = Math.floor(minutes / 60);
            const rest = minutes % 60;
            return rest === 0 ? hours + ' h' : hours + ' h ' + rest + ' min';
        }
        const days = Math.floor(minutes / 1440);
        const remAfterDays = minutes % 1440;
        const hours = Math.floor(remAfterDays / 60);
        return hours === 0 ? days + ' d' : days + ' d ' + hours + ' h';
    }

    function applyFlowCardState(card, minutes) {
        if (!card) return;
        card.classList.remove('ok', 'warn', 'error', 'stale-warn', 'stale-error');
        if (minutes == null || minutes > uiConfig.flowStaleErrorMinutes) {
            card.classList.add('error');
            return;
        }
        if (minutes >= uiConfig.flowStaleWarningMinutes) {
            card.classList.add('warn');
            return;
        }
        card.classList.add('ok');
    }

    function updateFlowSummary(data) {
        const incomingMinutes = minutesSince(data && data.incomingStartedAt);
        const outgoingMinutes = minutesSince(data && data.outgoingStartedAt);

        if (incomingAgeEl) incomingAgeEl.textContent = formatMinutesAge(incomingMinutes);
        if (incomingTimeEl) incomingTimeEl.textContent = data && data.incomingStartedAt ? formatTimestamp(data.incomingStartedAt) : 'Ingen data';
        if (outgoingAgeEl) outgoingAgeEl.textContent = formatMinutesAge(outgoingMinutes);
        if (outgoingTimeEl) outgoingTimeEl.textContent = data && data.outgoingStartedAt ? formatTimestamp(data.outgoingStartedAt) : 'Ingen data';

        applyFlowCardState(incomingCardEl, incomingMinutes);
        applyFlowCardState(outgoingCardEl, outgoingMinutes);
    }

    function swedishStatus(status) {
        const map = {
            OK: 'OK',
            INFO: 'Info',
            WARNING: 'Varning',
            ERROR: 'Fel'
        };
        return map[status] || status || '';
    }

    function friendlyHistoryEvent(eventType) {
        const map = {
            STATUS_DETECTED: 'Första läge',
            STATUS_CHANGED: 'Läget ändrades',
            OCCURRENCE_STARTED: 'Ny bevakning',
            ACKNOWLEDGED: 'Kvitterad'
        };
        return map[eventType] || eventType || '';
    }

    function friendlyOccurrenceKey(value) {
        if (!value) return '';
        const parts = String(value).split('|');
        if (parts.length >= 3) {
            return parts[1] + ' · ' + parts[2];
        }
        if (parts.length === 2) {
            return parts[1];
        }
        return value;
    }

    function friendlyHistoryActor(row) {
        const by = String((row && row.createdBy) || '').trim();
        if (!by) return 'Okänd';
        return by.toLowerCase() === 'system' ? 'Systemet' : by;
    }

    function extractStatusChange(row) {
        const msg = String((row && row.message) || '');
        const m = msg.match(/Status ändrad:?\s*([A-Z]+)\s*(?:->|till)\s*([A-Z]+)/i);
        if (!m) return '';
        return 'Från ' + swedishStatus(m[1].toUpperCase()) + ' till ' + swedishStatus(m[2].toUpperCase());
    }

    function friendlyHistoryMessage(row) {
        if (!row) return '-';

        const eventType = row.eventType || '';
        const status = swedishStatus(row.status || '');
        const occ = friendlyOccurrenceKey(row.occurrenceKey || '');
        const actor = friendlyHistoryActor(row);

        if (eventType === 'OCCURRENCE_STARTED') {
            if (occ) return 'Bevakningen startade för ' + occ + (status ? '. Aktuellt läge: ' + status : '');
            return status ? 'Bevakningen startade. Aktuellt läge: ' + status : 'Bevakningen startade';
        }

        if (eventType === 'STATUS_DETECTED') {
            return status ? 'Första registrerade läge var ' + status : 'Första registrerade läge';
        }

        if (eventType === 'STATUS_CHANGED') {
            const change = extractStatusChange(row);
            if (change) return change;
            return status ? 'Nytt läge: ' + status : 'Läget ändrades';
        }

        if (eventType === 'ACKNOWLEDGED') {
            const comment = String(row.message || '').trim();
            return comment
                ? 'Kvitterad av ' + actor + '. Kommentar: ' + comment
                : 'Kvitterad av ' + actor;
        }

        return row.message || '-';
    }

    function formatAckTimestamp(value) {
        if (!value) return '';
        return formatTimestamp(value);
    }

    function isAckRelevant(row) {
        return !!(row && (row.status === 'WARNING' || row.status === 'ERROR'));
    }

    function ackInfoText(row) {
        if (!isAckRelevant(row)) return '';
        if (!row.acknowledged) return 'Ej kvitterad';

        const by = row.acknowledgedBy || 'okänd';
        const at = formatAckTimestamp(row.acknowledgedAt || '');
        if (at) {
            return 'Kvitterad av ' + by + ' · ' + at;
        }
        return 'Kvitterad av ' + by;
    }


    function statusClass(status) {
        return 'status-' + String(status || '').toLowerCase();
    }

    function updateAcknowledgeButtonState() {
        const btn = qs('editorAckBtn');
        const row = selectedRuleId ? findRowById(selectedRuleId) : null;
        if (!btn) return;

        const canAcknowledge = !!(
            row &&
            (row.status === 'WARNING' || row.status === 'ERROR') &&
            !row.acknowledged &&
            !acknowledgeInFlight
        );

        btn.classList.toggle('hidden', !canAcknowledge);
        btn.disabled = !canAcknowledge;
        btn.textContent = acknowledgeInFlight ? 'Kvitterar...' : 'Kvittera';
    }

    function applyLocalAcknowledgement(ruleId, status, acknowledgedBy, acknowledgedAt) {
        currentRows = currentRows.map(function (row) {
            if (!row || row.id !== ruleId) return row;
            if (String(row.status || '').toUpperCase() !== String(status || '').toUpperCase()) return row;
            if (!(row.status === 'WARNING' || row.status === 'ERROR')) return row;

            const updated = Object.assign({}, row, {
                acknowledged: true,
                acknowledgedBy: acknowledgedBy || '',
                acknowledgedAt: acknowledgedAt || new Date().toISOString()
            });

            localAckOverrides[ackKeyForRow(updated)] = {
                acknowledged: true,
                acknowledgedBy: updated.acknowledgedBy,
                acknowledgedAt: updated.acknowledgedAt
            };

            return updated;
        });

        renderDashboardRows(currentRows);
        updateTableScroll();
        const wrap=document.getElementById('rulesTableWrap');if(wrap)wrap.scrollTop=0;

        if (selectedRuleId === ruleId) {
            const row = findRowById(ruleId);
            fillStatusBar(row);
            updateAcknowledgeButtonState();
        }
    }

    function statusWeight(status) {
        const map = {
            ERROR: 4,
            WARNING: 3,
            INFO: 2,
            OK: 1
        };
        return map[status] || 0;
    }

    function buildRuleBadgeText(rule) {
        if (!rule) return '';
        return (rule.sender || '-') + ' → ' + (rule.receiver || '-') + ' · ' + (rule.msgType || '-');
    }

    function inferModeFromRule(rule) {
        if ((rule.scheduleType || '') === 'MULTI_WINDOW') return 'multiwindow';
        if ((rule.specificDates || '').trim()) return 'dates';
        if ((rule.monthDays || '').trim()) return 'monthly';
        if ((rule.weekdays || '').trim()) return 'weekly';
        return 'daily';
    }

    function describeApplicability(rule) {
        if (!rule) return '';
        if ((rule.specificDates || '').trim()) return 'Datum: ' + rule.specificDates;
        if ((rule.monthDays || '').trim()) return 'Månadsdagar: ' + rule.monthDays;
        if ((rule.weekdays || '').trim()) return 'Veckodagar: ' + rule.weekdays;
        return 'Daglig';
    }

    function findRuleById(ruleId) {
        return currentRules.find(function (r) { return r.id === ruleId; }) || null;
    }

    function findRowById(ruleId) {
        return currentRows.find(function (r) { return r.id === ruleId; }) || null;
    }

    function findHistoryByRuleId(ruleId) {
        return currentHistory
            .filter(function (row) { return row.ruleId === ruleId; })
            .slice()
            .reverse();
    }

    function ackKeyForRow(row) {
        if (!row) return '';
        return [
            row.id || '',
            row.occurrenceKey || '',
            String(row.status || '').toUpperCase()
        ].join('|');
    }

    function mergeLocalAcknowledgements(rows) {
        return (rows || []).map(function (row) {
            const key = ackKeyForRow(row);
            const localAck = localAckOverrides[key];
            if (!localAck) return row;

            if (row.acknowledged) {
                delete localAckOverrides[key];
                return row;
            }

            return Object.assign({}, row, {
                acknowledged: true,
                acknowledgedBy: localAck.acknowledgedBy || '',
                acknowledgedAt: localAck.acknowledgedAt || ''
            });
        });
    }

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

    function showBackendBanner(ok, message) {
        const banner = qs('backendBanner');
        if (!banner) return;

        if (ok) {
            banner.classList.add('hidden');
            banner.textContent = '';
            return;
        }

        banner.classList.remove('hidden');
        banner.textContent = 'Tekniskt fel: ' + (message || 'okänt fel');
    }

    function openEditorPanel() {
        const panel = qs('rulePanel');
        if (!panel) return;

        if (panel.classList.contains('hidden')) {
            panel.classList.remove('hidden');
            const icon = document.querySelector('[data-accordion-target="rulePanel"] .accordion-icon');
            if (icon) icon.textContent = '–';
        }
    }

    function initAccordions() {
        qsa('[data-accordion-target]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                const targetId = btn.getAttribute('data-accordion-target');
                const panel = qs(targetId);
                const icon = btn.querySelector('.accordion-icon');
                if (!panel) return;

                const hidden = panel.classList.contains('hidden');
                panel.classList.toggle('hidden', !hidden);
                if (icon) icon.textContent = hidden ? '–' : '+';
            });
        });
    }

    function initMonthDayPicker(containerId, attrName, onChange) {
        const container = qs(containerId);
        if (!container) return;

        container.innerHTML = '';
        for (let i = 1; i <= 31; i++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'chip';
            btn.textContent = String(i);
            btn.setAttribute(attrName, String(i));
            btn.addEventListener('click', function () {
                if (editorMode === 'view') return;
                btn.classList.toggle('active');
                onChange();
            });
            container.appendChild(btn);
        }
    }

    function initWeekdayPicker(selector, onChange) {
        qsa(selector).forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (editorMode === 'view') return;
                btn.classList.toggle('active');
                onChange();
            });
        });
    }

    function clearChips(selector) {
        qsa(selector).forEach(function (btn) {
            btn.classList.remove('active');
        });
    }

    function setChipsFromCsv(selector, attr, csv) {
        const values = (csv || '')
            .split(',')
            .map(function (x) { return x.trim(); })
            .filter(Boolean);

        qsa(selector).forEach(function (btn) {
            btn.classList.toggle('active', values.includes(btn.getAttribute(attr)));
        });
    }

    function syncHiddenFields() {
        const weekdays = qsa('#weekdayPicker .chip.active').map(function (btn) {
            return btn.getAttribute('data-weekday');
        });
        const monthDays = qsa('#monthDayPicker .chip.active').map(function (btn) {
            return btn.getAttribute('data-monthday');
        });

        if (qs('weekdays')) qs('weekdays').value = weekdays.join(',');
        if (qs('monthDays')) qs('monthDays').value = monthDays.join(',');
        if (qs('specificDates')) qs('specificDates').value = selectedSpecificDates.join(',');
    }

    function renderSpecificDates() {
        const list = qs('specificDatesList');
        if (!list) return;

        list.innerHTML = '';
        list.scrollTop = 0;
        selectedSpecificDates.forEach(function (dateValue) {
            const tag = document.createElement('div');
            tag.className = 'selected-tag';
            tag.innerHTML = `
                <span>${escapeHtml(dateValue)}</span>
                <button type="button" data-remove-date="${escapeHtml(dateValue)}">×</button>
            `;
            list.appendChild(tag);
        });

        syncHiddenFields();
    }

    function updateEditorRuleBadge(rule) {
        const badge = qs('editorRuleBadge');
        if (!badge) return;

        if (!rule) {
            badge.textContent = '';
            badge.classList.add('hidden');
            return;
        }

        badge.textContent = buildRuleBadgeText(rule);
        badge.classList.remove('hidden');
    }

    function renderRuleMode() {
        qsa('[data-rule-mode]').forEach(function (btn) {
            const isActive = btn.getAttribute('data-rule-mode') === selectedRuleMode;
            btn.classList.toggle('active', isActive);
            btn.disabled = editorMode === 'view';
        });

        if (qs('ruleModeHelp')) {
            qs('ruleModeHelp').textContent = MODE_HELP[selectedRuleMode] || '';
        }

        if (qs('weekdaySection')) {
            qs('weekdaySection').classList.toggle('hidden', selectedRuleMode !== 'weekly');
        }
        if (qs('monthDaySection')) {
            qs('monthDaySection').classList.toggle('hidden', selectedRuleMode !== 'monthly');
        }
        if (qs('specificDateSection')) {
            qs('specificDateSection').classList.toggle('hidden', selectedRuleMode !== 'dates');
        }
        if (qs('windowsSpecWrap')) {
            qs('windowsSpecWrap').classList.toggle('hidden', selectedRuleMode !== 'multiwindow');
        }
        if (qs('scheduleType')) {
            qs('scheduleType').value = selectedRuleMode === 'multiwindow' ? 'MULTI_WINDOW' : 'DAILY';
        }

        syncHiddenFields();
    }

    function setFormEditable(editable) {
        qsa('#ruleForm input, #ruleForm select, #ruleForm textarea').forEach(function (el) {
            if (el.type === 'hidden') return;
            el.disabled = !editable;
        });

        qsa('#weekdayPicker .chip, #monthDayPicker .chip').forEach(function (chip) {
            chip.classList.toggle('chip-disabled', !editable);
        });

        if (qs('addSpecificDateBtn')) qs('addSpecificDateBtn').disabled = !editable;
        if (qs('clearSpecificDatesBtn')) qs('clearSpecificDatesBtn').disabled = !editable;
        if (qs('specificDateInput')) qs('specificDateInput').disabled = !editable;
    }

    function setEditorMode(mode) {
        editorMode = mode;
        const editable = mode === 'new' || mode === 'edit';
        const rule = selectedRuleId ? findRuleById(selectedRuleId) : null;
        const modeBadge = qs('editorModeBadge');

        setFormEditable(editable);
        updateEditorRuleBadge(rule);

        if (modeBadge) {
            if (mode === 'new') modeBadge.textContent = 'Ny';
            else if (mode === 'view') modeBadge.textContent = 'Visa';
            else modeBadge.textContent = 'Redigera';
        }

        if (mode === 'new') {
            if (qs('editorNewBtn')) qs('editorNewBtn').classList.remove('hidden');
            if (qs('editorEditBtn')) qs('editorEditBtn').classList.add('hidden');
            if (qs('editorAckBtn')) qs('editorAckBtn').classList.add('hidden');
            if (qs('editorDeleteBtn')) qs('editorDeleteBtn').classList.add('hidden');
            if (qs('editorSaveBtn')) qs('editorSaveBtn').classList.remove('hidden');
            if (qs('editorCancelBtn')) qs('editorCancelBtn').classList.remove('hidden');
            updateAcknowledgeButtonState();
            return;
        }

        if (mode === 'view') {
            if (qs('editorNewBtn')) qs('editorNewBtn').classList.remove('hidden');
            if (qs('editorEditBtn')) qs('editorEditBtn').classList.remove('hidden');
            if (qs('editorDeleteBtn')) qs('editorDeleteBtn').classList.remove('hidden');
            if (qs('editorSaveBtn')) qs('editorSaveBtn').classList.add('hidden');
            if (qs('editorCancelBtn')) qs('editorCancelBtn').classList.add('hidden');

            updateAcknowledgeButtonState();
            return;
        }

        if (qs('editorNewBtn')) qs('editorNewBtn').classList.add('hidden');
        if (qs('editorEditBtn')) qs('editorEditBtn').classList.add('hidden');
        if (qs('editorAckBtn')) qs('editorAckBtn').classList.add('hidden');
        if (qs('editorDeleteBtn')) qs('editorDeleteBtn').classList.add('hidden');
        if (qs('editorSaveBtn')) qs('editorSaveBtn').classList.remove('hidden');
        if (qs('editorCancelBtn')) qs('editorCancelBtn').classList.remove('hidden');
        updateAcknowledgeButtonState();
    }

    function fillStatusBar(row) {
        const statusEl = qs('editorCurrentStatus');
        const countEl = qs('editorCurrentCount');
        const ackEl = qs('editorAckInfo');
        const ackWrap = ackEl ? ackEl.closest('.editor-status-item') : null;

        if (!statusEl || !countEl) return;

        if (!row) {
            statusEl.textContent = '-';
            countEl.textContent = '-';
            if (ackEl) ackEl.textContent = '';
            if (ackWrap) ackWrap.classList.add('hidden');
            return;
        }

        statusEl.innerHTML = `<span class="status-chip ${statusClass(row.status)}">${escapeHtml(swedishStatus(row.status))}</span>`;
        countEl.textContent = 'Idag: ' + String(row.countToday || 0);

        if (ackEl) {
            if (!isAckRelevant(row)) {
                ackEl.textContent = '';
                if (ackWrap) ackWrap.classList.add('hidden');
            } else if (row.acknowledged) {
                const by = row.acknowledgedBy || 'okänd';
                const at = row.acknowledgedAt ? formatTimestamp(row.acknowledgedAt) : '';
                ackEl.innerHTML = `<span class="ack-meta">Kvitterad av ${escapeHtml(by)}${at ? ' · ' + escapeHtml(at) : ''}</span>`;
                if (ackWrap) ackWrap.classList.remove('hidden');
            } else {
                ackEl.textContent = 'Ej kvitterad';
                if (ackWrap) ackWrap.classList.remove('hidden');
            }
        }
    }

    function renderEditorHistory(ruleId) {
        const panel = qs('editorHistoryPanel');
        const empty = qs('editorHistoryEmpty');
        const list = qs('editorHistoryList');

        if (!panel || !empty || !list) return;

        list.innerHTML = '';
        list.scrollTop = 0;

        if (!ruleId) {
            panel.classList.add('hidden');
            return;
        }

        panel.classList.remove('hidden');

        const rows = findHistoryByRuleId(ruleId);
        if (!rows.length) {
            empty.classList.remove('hidden');
            return;
        }

        empty.classList.add('hidden');

        rows.forEach(function (row) {
            const item = document.createElement('div');
            item.className = 'timeline-item ' + statusClass(row.status || '');
            const isAckEvent = (row.eventType || '') === 'ACKNOWLEDGED';
            item.innerHTML = `
                <div class="timeline-head">
                    <div class="timeline-event">${escapeHtml(friendlyHistoryEvent(row.eventType || ''))}</div>
                    <div class="timeline-time">${escapeHtml(formatTimestamp(row.createdAt || ''))}</div>
                </div>
                <div class="timeline-sub">
                    ${isAckEvent ? '' : `<span class="timeline-status-badge ${statusClass(row.status || '')}">${escapeHtml(swedishStatus(row.status || ''))}</span>`}
                    <span class="timeline-meta">${escapeHtml(friendlyHistoryActor(row))}</span>
                    ${row.occurrenceKey ? `<span class="timeline-occurrence">${escapeHtml(friendlyOccurrenceKey(row.occurrenceKey || ''))}</span>` : ''}
                </div>
                <div class="timeline-message">${escapeHtml(friendlyHistoryMessage(row))}</div>
            `;
            list.appendChild(item);
        });
    }

    function populateFormFromRule(ruleId) {
        const rule = findRuleById(ruleId);
        const row = findRowById(ruleId);
        if (!rule) return;

        selectedRuleId = ruleId;
        formDirty = false;

        if (qs('ruleId')) qs('ruleId').value = rule.id || '';
        if (qs('sender')) qs('sender').value = rule.sender || '';
        if (qs('receiver')) qs('receiver').value = rule.receiver || '';
        if (qs('msgType')) qs('msgType').value = rule.msgType || '';
        if (qs('description')) qs('description').value = rule.description || '';
        if (qs('minExpected')) qs('minExpected').value = rule.minExpected || 0;
        if (qs('maxExpected')) qs('maxExpected').value = rule.maxExpected || 0;
        if (qs('deadline')) qs('deadline').value = rule.deadline || '15:00';
        if (qs('warningMinutesBeforeDeadline')) qs('warningMinutesBeforeDeadline').value = rule.warningMinutesBeforeDeadline || 60;
        if (qs('active')) qs('active').value = String(rule.active);
        if (qs('useHistoricalBaseline')) qs('useHistoricalBaseline').value = String(rule.useHistoricalBaseline);
        if (qs('historicalDays')) qs('historicalDays').value = rule.historicalDays || 10;
        if (qs('minPercentOfAverage')) qs('minPercentOfAverage').value = rule.minPercentOfAverage || 85;
        if (qs('windowsSpec')) qs('windowsSpec').value = (rule.windowsSpec || '').replaceAll('\\n', '\n');

        clearChips('#weekdayPicker .chip');
        clearChips('#monthDayPicker .chip');
        setChipsFromCsv('#weekdayPicker .chip', 'data-weekday', rule.weekdays || '');
        setChipsFromCsv('#monthDayPicker .chip', 'data-monthday', rule.monthDays || '');

        selectedSpecificDates = (rule.specificDates || '')
            .split(',')
            .map(function (x) { return x.trim(); })
            .filter(Boolean);
        renderSpecificDates();

        selectedRuleMode = inferModeFromRule(rule);
        renderRuleMode();
        fillStatusBar(row);
        renderEditorHistory(ruleId);
        updateEditorRuleBadge(rule);
    }

    function resetCreateForm() {
        selectedRuleId = null;
        formDirty = false;

        if (qs('ruleId')) qs('ruleId').value = '';
        if (qs('sender')) qs('sender').value = '';
        if (qs('receiver')) qs('receiver').value = '';
        if (qs('msgType')) qs('msgType').value = '';
        if (qs('description')) qs('description').value = '';
        if (qs('minExpected')) qs('minExpected').value = '1';
        if (qs('maxExpected')) qs('maxExpected').value = '1';
        if (qs('deadline')) qs('deadline').value = '15:00';
        if (qs('warningMinutesBeforeDeadline')) qs('warningMinutesBeforeDeadline').value = '60';
        if (qs('active')) qs('active').value = 'true';
        if (qs('useHistoricalBaseline')) qs('useHistoricalBaseline').value = 'false';
        if (qs('historicalDays')) qs('historicalDays').value = '10';
        if (qs('minPercentOfAverage')) qs('minPercentOfAverage').value = '85';
        if (qs('windowsSpec')) qs('windowsSpec').value = '';

        clearChips('#weekdayPicker .chip');
        clearChips('#monthDayPicker .chip');
        selectedSpecificDates = [];
        selectedRuleMode = 'daily';

        renderSpecificDates();
        renderRuleMode();
        fillStatusBar(null);
        renderEditorHistory(null);
        updateEditorRuleBadge(null);
        setEditorMode('new');
    }

    function toggleRulePanel(ruleId) {
        if (selectedRuleId === ruleId && !qs('rulePanel').classList.contains('hidden')) {
            // panelen är redan öppen för samma regel → stäng den
            qs('rulePanel').classList.add('hidden');
            selectedRuleId = null;
            renderDashboardRows(currentRows);
        updateTableScroll();
        const wrap=document.getElementById('rulesTableWrap');if(wrap)wrap.scrollTop=0; // ta bort highlight
            return;
        }
        openRule(ruleId);
    }

    function openRule(ruleId) {
        openEditorPanel();
        populateFormFromRule(ruleId);
        setEditorMode('view');
        const row = findRowById(ruleId);
        fillStatusBar(row);
        updateAcknowledgeButtonState();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function sortRows(rows) {
        const sorted = rows.slice();

        sorted.sort(function (a, b) {
            const ruleA = findRuleById(a.id);
            const ruleB = findRuleById(b.id);

            let valueA = '';
            let valueB = '';

            if (currentSort.key === 'status') {
                valueA = statusWeight(a.status);
                valueB = statusWeight(b.status);
            } else if (currentSort.key === 'countToday') {
                valueA = Number(a.countToday || 0);
                valueB = Number(b.countToday || 0);
            } else if (currentSort.key === 'deadline') {
                valueA = a.deadline || '';
                valueB = b.deadline || '';
            } else if (currentSort.key === 'sender') {
                valueA = a.sender || '';
                valueB = b.sender || '';
            } else {
                valueA = describeApplicability(ruleA);
                valueB = describeApplicability(ruleB);
            }

            if (typeof valueA === 'number') {
                return currentSort.direction === 'asc' ? valueA - valueB : valueB - valueA;
            }

            const cmp = String(valueA).localeCompare(String(valueB), 'sv');
            return currentSort.direction === 'asc' ? cmp : -cmp;
        });

        return sorted;
    }

    
function updateTableScroll() {
    const wrap = document.getElementById('rulesTableWrap');
    if (!wrap) return;
    const rows = wrap.querySelectorAll('tbody tr');
    // Lägg till class "scroll" om det finns fler än 5 rader
    wrap.classList.toggle('scroll', rows.length > 5);
}


    function renderDashboardRows(rows) {
        const tbody = document.querySelector('#rulesTable tbody');
        if (!tbody) return;

        const filter = (qs('filterInput') ? qs('filterInput').value : '').trim().toLowerCase();

        const filtered = rows.filter(function (row) {
            const rule = findRuleById(row.id);
            const haystack = [
                row.sender,
                row.receiver,
                row.msgType,
                row.status,
                row.message,
                describeApplicability(rule)
            ].join(' ').toLowerCase();
            return !filter || haystack.includes(filter);
        });

        const sorted = sortRows(filtered);
        tbody.innerHTML = '';

        sorted.forEach(function (row) {
            const rule = findRuleById(row.id);
            const tr = document.createElement('tr');
            tr.className = 'fade-up clickable-row';
            tr.setAttribute('data-open', row.id);
            tr.setAttribute('tabindex', '0');
            tr.setAttribute('role', 'button');

            if (selectedRuleId === row.id) {
                tr.classList.add('selected-row');
            }

            tr.innerHTML = `
                <td class="mono">${escapeHtml(row.sender)}</td>
                <td class="mono">${escapeHtml(row.receiver)}</td>
                <td class="mono">${escapeHtml(row.msgType)}</td>
                <td>
                    <span class="ack-inline">
                        <span class="status-chip ${statusClass(row.status)}">${escapeHtml(swedishStatus(row.status))}</span>
                        ${row.acknowledged ? '<span class="ack-badge">Kvitterad</span>' : ''}
                    </span>
                </td>
                <td>${escapeHtml(row.countToday)}</td>
                <td class="mono">${escapeHtml(row.deadline)}</td>
                <td>${escapeHtml(describeApplicability(rule))}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    function initTableSorting() {
        const headerMap = [
            { index: 0, key: 'sender' },
            { index: 3, key: 'status' },
            { index: 4, key: 'countToday' },
            { index: 5, key: 'deadline' },
            { index: 6, key: 'applies' }
        ];

        const headers = qsa('#rulesTable thead th');
        headerMap.forEach(function (item) {
            const th = headers[item.index];
            if (!th) return;

            th.classList.add('sortable');
            th.addEventListener('click', function () {
                if (currentSort.key === item.key) {
                    currentSort.direction = currentSort.direction === 'asc' ? 'desc' : 'asc';
                } else {
                    currentSort.key = item.key;
                    currentSort.direction = item.key === 'status' ? 'desc' : 'asc';
                }
                renderDashboardRows(currentRows);
        updateTableScroll();
        const wrap=document.getElementById('rulesTableWrap');if(wrap)wrap.scrollTop=0;
            });
        });
    }

    async function loadDashboard() {
        const data = await fetchJson(API.dashboard, { cache: 'no-store' });
        uiConfig = Object.assign(uiConfig, data);

        if (qs('statTotal')) qs('statTotal').textContent = data.totalFlowsToday;
        if (qs('statOk')) qs('statOk').textContent = data.ok;
        if (qs('statInfo')) qs('statInfo').textContent = data.info;
        if (qs('statWarning')) qs('statWarning').textContent = data.warning;
        if (qs('statError')) qs('statError').textContent = data.error;
        updateFlowSummary(data);
        if (qs('refreshedAt')) qs('refreshedAt').textContent = formatTimestamp(data.refreshedAt);

        showBackendBanner(data.backendOk, data.backendMessage);

        currentRows = mergeLocalAcknowledgements(data.rows || []);
        renderDashboardRows(currentRows);
        updateTableScroll();
        const wrap=document.getElementById('rulesTableWrap');if(wrap)wrap.scrollTop=0;
        updateAcknowledgeButtonState();

        if (selectedRuleId && editorMode === 'view') {
            const exists = findRuleById(selectedRuleId) && findRowById(selectedRuleId);
            if (exists) {
                populateFormFromRule(selectedRuleId);
                setEditorMode('view');
            }
        }
    }

    async function loadRules() {
        currentRules = await fetchJson(API.rules, { cache: 'no-store' });
    }

    async function loadHistory() {
        currentHistory = await fetchJson(API.history, { cache: 'no-store' });
    }

    function startRefreshTimers() {
        if (dashboardRefreshTimer) clearInterval(dashboardRefreshTimer);
        if (historyRefreshTimer) clearInterval(historyRefreshTimer);

        dashboardRefreshTimer = setInterval(function () {
            loadDashboard().catch(function (e) {
                console.error(e);
                showBackendBanner(false, e.message || 'Kunde inte ladda dashboard');
            });
        }, Math.max(1, uiConfig.dashboardRefreshSeconds) * 1000);

        historyRefreshTimer = setInterval(function () {
            loadHistory().catch(function (e) {
                console.error(e);
            });
        }, Math.max(1, uiConfig.historyRefreshSeconds) * 1000);
    }

    async function loadAdminConfig() {
        const box = qs('adminConfigBox');
        if (!box) return;
        const data = await fetchJson(API.adminConfig, { cache: 'no-store' });
        uiConfig = Object.assign(uiConfig, data);
        box.textContent = JSON.stringify(data, null, 2);
    }

    async function refreshAll() {
        try {
            await loadRules();
            await loadDashboard();
            await loadHistory();
            await loadAdminConfig();
        } catch (e) {
            console.error(e);
            showBackendBanner(false, e.message || 'Kunde inte ladda data');
        }
    }

    async function saveRule() {
        syncHiddenFields();

        const form = qs('ruleForm');
        if (!form) return;

        
const body  = new URLSearchParams(new FormData(form));
const ruleId = (qs("ruleId") ? qs("ruleId").value.trim() : "");
const isNew  = !ruleId;
const method = isNew ? "POST" : "PUT";
const url    = isNew ? API.rules : `${API.rules}/${encodeURIComponent(ruleId)}`;
        await fetchText(url, {
            method: method,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: body.toString()
        });

        await refreshAll();
        formDirty = false;

        if (method === 'PUT' && selectedRuleId) {
            openRule(selectedRuleId);
        } else {
            resetCreateForm();
            openEditorPanel();
        }
    }

    async function deleteRule(ruleId) {
        await fetchText(API.rules + '?id=' + encodeURIComponent(ruleId), {
            method: 'DELETE'
        });
        resetCreateForm();
        await refreshAll();
    }

    async function acknowledge(ruleId, status) {
        const row = findRowById(ruleId);
        if (!ruleId || !row) return;
        if (!(row.status === 'WARNING' || row.status === 'ERROR')) return;
        if (row.acknowledged || acknowledgeInFlight) return;

        const acknowledgedBy = window.prompt('Vem kvitterar?', 'user');
        if (acknowledgedBy == null) return;

        const comment = window.prompt('Kommentar (valfritt):', '') || '';

        acknowledgeInFlight = true;
        updateAcknowledgeButtonState();

        try {
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

            const acknowledgedAt = new Date().toISOString();

            // Direkt i GUI, utan att vänta på nästa refresh
            applyLocalAcknowledgement(ruleId, status, acknowledgedBy, acknowledgedAt);
            acknowledgeInFlight = false;
            updateAcknowledgeButtonState();

            // Ladda historik direkt, men behåll lokal kvittering synlig även om dashboard-snapshot släpar
            await loadHistory();

            if (selectedRuleId === ruleId) {
                const refreshedRow = findRowById(ruleId);
                fillStatusBar(refreshedRow);
                updateAcknowledgeButtonState();
            } else {
                renderDashboardRows(currentRows);
        updateTableScroll();
        const wrap=document.getElementById('rulesTableWrap');if(wrap)wrap.scrollTop=0;
            }

            // Försök synka dashboard i bakgrunden utan att tappa lokal kvittering
            setTimeout(function () {
                loadDashboard().then(_=>dashboardRefresh()).catch(function () {});
            }, 250);
        } catch (error) {
            acknowledgeInFlight = false;
            updateAcknowledgeButtonState();
            window.alert('Kvittering misslyckades: ' + (error && error.message ? error.message : error));
        }
    }

    function setupCreateModePicker() {
        qsa('[data-rule-mode]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (editorMode === 'view') return;
                selectedRuleMode = btn.getAttribute('data-rule-mode');
                renderRuleMode();
            });
        });
    }

    function setupSpecificDateButtons() {
        on('addSpecificDateBtn', 'click', function () {
            if (editorMode === 'view') return;

            const input = qs('specificDateInput');
            const value = input ? input.value : '';
            if (!value) return;

            if (!selectedSpecificDates.includes(value)) {
                selectedSpecificDates.push(value);
                selectedSpecificDates.sort();
            }

            if (input) input.value = '';
            renderSpecificDates();
        });

        on('clearSpecificDatesBtn', 'click', function () {
            if (editorMode === 'view') return;
            selectedSpecificDates = [];
            renderSpecificDates();
        });

        document.addEventListener('click', function (event) {
            const value = event.target.getAttribute('data-remove-date');
            if (!value || editorMode === 'view') return;
            selectedSpecificDates = selectedSpecificDates.filter(function (x) { return x !== value; });
            renderSpecificDates();
        });
    }

    function setupEditorButtons() {
        on('editorNewBtn', 'click', function () {
            openEditorPanel();
            resetCreateForm();
        });

        on('editorEditBtn', 'click', function () {
            if (!selectedRuleId) return;
            setEditorMode('edit');
        });

        on('editorSaveBtn', 'click', async function () {
            await saveRule();
        });

        on('editorCancelBtn', 'click', function () {
            if (selectedRuleId) openRule(selectedRuleId);
            else resetCreateForm();
        });

        on('editorDeleteBtn', 'click', async function () {
            if (!selectedRuleId) return;
            if (window.confirm('Radera regeln?')) {
                await deleteRule(selectedRuleId);
            }
        });

        on('editorAckBtn', 'click', async function () {
            const row = findRowById(selectedRuleId);
            if (!selectedRuleId || !row || row.acknowledged || acknowledgeInFlight) return;
            await acknowledge(selectedRuleId, row.status);
        });
    }

    function setupGlobalClicks() {
        document.addEventListener('click', function (event) {
            const openEl = event.target.closest('[data-open]');
            if (!openEl) return;

            const openId = openEl.getAttribute('data-open');
            if (openId) toggleRulePanel(openId);
        });

        document.addEventListener('keydown', function (event) {
            const openEl = event.target.closest('[data-open]');
            if (!openEl) return;

            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                const openId = openEl.getAttribute('data-open');
                if (openId) openRule(openId);
            }
        });
    }

    on('ruleForm', 'input', function () {
        markFormDirty();
    });

    on('ruleForm', 'change', function () {
        markFormDirty();
    });

    on('ruleForm', 'submit', function (event) {
        event.preventDefault();
        saveRule();
    });

    on('resetBtn', 'click', function () {
        resetCreateForm();
    });

    on('loadConfigBtn', 'click', loadAdminConfig);

    on('exportRulesBtn', 'click', async function () {
        const box = qs('rulesJsonBox');
        if (!box) return;
        const text = await fetchText(API.exportRules, { cache: 'no-store' });
        box.value = text;
    });

    on('importRulesBtn', 'click', async function () {
        const box = qs('rulesJsonBox');
        const body = box ? box.value : '';
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
    });

    on('filterInput', 'input', function () {
        renderDashboardRows(currentRows);
        updateTableScroll();
        const wrap=document.getElementById('rulesTableWrap');if(wrap)wrap.scrollTop=0;
    });

    try {
        initAccordions();
        initMonthDayPicker('monthDayPicker', 'data-monthday', syncHiddenFields);
        initWeekdayPicker('#weekdayPicker .chip', syncHiddenFields);
        initTableSorting();
        setupCreateModePicker();
        setupSpecificDateButtons();
        setupEditorButtons();
        setupGlobalClicks();

        resetCreateForm();
        refreshAll();

        
        
    } catch (e) {
        console.error('Frontend crash:', e);
        showBackendBanner(false, 'Frontend-fel: ' + (e && e.message ? e.message : e));
    }
});


