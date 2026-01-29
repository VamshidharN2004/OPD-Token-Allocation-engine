const API_BASE = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) ? CONFIG.API_BASE_URL : 'http://localhost:8080/api';

function log(msg) {
    const logs = document.getElementById('logs');
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.innerHTML = `> ${msg}`;
    logs.prepend(entry);
}

async function onboardDoctor() {
    const name = document.getElementById('docName').value;
    const specialization = document.getElementById('docSpec').value;
    try {
        const res = await fetch(`${API_BASE}/doctors?name=${name}&specialization=${specialization}`, { method: 'POST' });
        const data = await res.json();
        log(`Created Doctor: <b>${data.name}</b> <br><span style="color:#64748b; font-size:0.8em">ID: ${data.id}</span>`);

        document.getElementById('slotDocId').value = data.id;
        document.getElementById('bookDocId').value = data.id;
        document.getElementById('viewDocId').value = data.id;
        loadSlots();
    } catch (e) {
        log('Error creating doctor: ' + e);
        alert(e);
    }
}

async function createSlot() {
    const docId = document.getElementById('slotDocId').value;
    const date = document.getElementById('slotDate').value;
    const start = document.getElementById('startTime').value;
    const end = document.getElementById('endTime').value;
    const cap = document.getElementById('capacity').value;

    if (!date) { alert("Please select a date"); return; }

    try {
        const res = await fetch(`${API_BASE}/slots`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ doctorId: docId, date: date, startTime: start, endTime: end, capacity: parseInt(cap) })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || err.error || "Failed to create slot");
        }

        const data = await res.json();
        log(`Created Slot: ${data.date} ${formatTime(data.startTime)} to ${formatTime(data.endTime)}`);
        loadSlots();
    } catch (e) {
        log('Error: ' + e.message);
        alert('Error: ' + e.message);
    }
}

async function bookToken() {
    const docId = document.getElementById('bookDocId').value;
    const date = document.getElementById('bookDate').value;
    const name = document.getElementById('patName').value;
    const source = document.getElementById('source').value;

    if (!date) { alert("Please select a date"); return; }

    try {
        const res = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ doctorId: docId, date: date, patientName: name, source: source })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || err.error || "Booking Failed");
        }

        const data = await res.json();
        log(`✅ Booked: ${data.patientName} (${data.source})`);
        loadSlots();
    } catch (e) {
        log(`❌ Booking Failed: ${e.message}`);
        alert(`Booking Failed: ${e.message}`);
    }
}

function formatTime(timeStr) {
    if (!timeStr) return '';
    const [h, m] = timeStr.split(':');
    const hour = parseInt(h);
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const hour12 = hour % 12 || 12;
    return `${hour12}:${m} ${ampm}`;
}

async function loadSlots() {
    const docId = document.getElementById('viewDocId').value;
    if (!docId) return;

    try {
        const res = await fetch(`${API_BASE}/doctors/${docId}/slots`);

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || err.error || "Failed to load slots");
        }

        const slots = await res.json();

        const container = document.getElementById('slotsContainer');
        container.innerHTML = '';

        if (slots.length === 0) {
            container.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">No slots created for this Doctor yet.</div>`;
            return;
        }

        slots.forEach(slot => {
            const filled = slot.tokens.filter(t => t.status === 'ACTIVE' || !t.status).length;
            const total = slot.maxCapacity;
            const fillPercent = Math.min((filled / total) * 100, 100);
            const startFmt = formatTime(slot.startTime);
            const endFmt = formatTime(slot.endTime);

            let tokenHtml = '';
            slot.tokens.forEach(t => {
                const isNoShow = t.status === 'NO_SHOW';
                const opacity = isNoShow ? '0.4' : '1';
                const textDec = isNoShow ? 'line-through' : 'none';
                const filter = isNoShow ? 'grayscale(100%)' : 'none';
                const btnText = isNoShow ? 'Undo' : '🚫';
                const btnClass = isNoShow ? 'btn-warning' : 'btn-danger';

                tokenHtml += `
            <div class="token-item" style="opacity: ${opacity}; text-decoration: ${textDec}; filter: ${filter}">
                <div>
                    <span class="token-name">${t.patientName}</span>
                    <span class="badge status-${t.source}">${t.source}</span>
                </div>
                <div style="display:flex; gap: 5px;">
                    <button onclick="markNoShow('${t.id}')" class="btn-icon ${isNoShow ? 'btn-warning' : 'btn-danger'}" style="width: auto; padding: 4px 8px;" title="Toggle No-Show">${btnText}</button>
                    <button onclick="cancelToken('${t.id}')" class="btn-icon" style="width: auto; padding: 4px 8px; background: transparent; border: 1px solid #ef4444; color: #ef4444;" title="Cancel">✕</button>
                </div>
            </div>
           `;
            });

            const html = `
            <div class="slot-card">
                <div class="slot-header">
                    <div>
                        <div style="font-size: 0.8em; color: var(--accent); font-weight: bold; text-transform: uppercase;">${slot.date}</div>
                        <span class="slot-time">${startFmt} - ${endFmt}</span>
                    </div>
                    
                    <div style="display: items-center; gap: 5px;">
                        <span class="slot-capacity">${filled}/${total}</span>
                        <button onclick="deleteSlot('${slot.id}', '${startFmt} - ${endFmt}')" class="btn-icon btn-danger" style="padding: 4px 8px;" title="Remove Completed Slot">🗑️</button>
                    </div>
                </div>
                
                <div style="display:flex; justify-content:space-between; align-items:center; font-size: 0.85rem; color: var(--text-muted);">
                     <span>ID: ...${slot.id.substring(0, 8)}</span>
                     <button onclick="delaySlot('${slot.id}')" class="btn-icon btn-warning" style="font-size: 0.75rem;">+15 min</button>
                </div>
                
                <div class="progress-bar">
                    <div class="progress-fill" style="width: ${fillPercent}%;"></div>
                </div>
                
                <div class="token-list">
                    ${tokenHtml}
                </div>
            </div>
        `;
            container.innerHTML += html;
        });

    } catch (e) {
        console.error(e);
        alert(e.message);
        const container = document.getElementById('slotsContainer');
        container.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--danger);">${e.message}</div>`;
    }
}

async function delaySlot(slotId) {
    try {
        await fetch(`${API_BASE}/slots/${slotId}/delay?minutes=15`, { method: 'POST' });
        log(`⚠️ Slot Delayed by 15 mins`);
        loadSlots();
    } catch (e) {
        log('Error delaying slot');
    }
}

async function cancelToken(tokenId) {
    if (!confirm('Cancel this token?')) return;
    try {
        await fetch(`${API_BASE}/tokens/${tokenId}/cancel`, { method: 'POST' });
        log(`❌ Token Cancelled`);
        loadSlots();
    } catch (e) {
        log('Error cancelling token');
    }
}

async function markNoShow(tokenId) {
    try {
        await fetch(`${API_BASE}/tokens/${tokenId}/noshow`, { method: 'POST' });
        log(`🚫 Toggled No-Show`);
        loadSlots();
    } catch (e) {
        log('Error toggling');
    }
}

async function deleteSlot(slotId, timeRange) {
    if (!confirm('Delete this slot? (Do this only when time is completed)')) return;
    try {
        await fetch(`${API_BASE}/slots/${slotId}`, { method: 'DELETE' });
        log(`🗑️ Slot Removed: ${timeRange}`);
        loadSlots();
    } catch (e) {
        log('Error deleting slot');
    }
}
