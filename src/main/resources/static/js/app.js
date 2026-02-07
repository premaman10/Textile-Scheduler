// State Management
let currentSchedule = null;
let allOrders = [];
let comparisonChart = null;
let priorityChart = null;
let familyChart = null;

const colorMap = {
    'WHITES_PASTELS': '#f8fafc',
    'LIGHT_COLORS': '#bae6fd',
    'MEDIUM_COLORS': '#7dd3fc',
    'DARK_COLORS': '#38bdf8',
    'BLACKS_DEEP_DARKS': '#1e293b'
};

document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    initCharts();
    refreshOrders();
});

function setupEventListeners() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    document.getElementById('populateBtn').addEventListener('click', populateDemoData);
    document.getElementById('simulateBtn').addEventListener('click', simulatePeakHours);
    document.getElementById('generateBtn').addEventListener('click', generateSchedule);
    document.getElementById('clearOrdersBtn').addEventListener('click', clearOrders);

    const modal = document.getElementById('orderModal');
    document.getElementById('addOrderBtn').addEventListener('click', () => modal.style.display = 'flex');
    document.getElementById('closeModal').addEventListener('click', () => modal.style.display = 'none');

    document.getElementById('orderForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await createOrder();
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');
    const tabEl = document.getElementById(`${tabId}Tab`);
    if (tabEl) tabEl.classList.add('active');

    document.getElementById('pageTitle').innerText = tabId.charAt(0).toUpperCase() + tabId.slice(1);
}

async function refreshOrders() {
    try {
        const response = await fetch('/api/orders');
        if (response.ok) {
            allOrders = await response.json();
            renderOrderTable();
            updateCapacityUI();
            updateAnalyticsCharts();
            updateActivityFeed();
        }
    } catch (err) { console.error(err); }
}

async function createOrder() {
    const orderData = {
        colorName: document.getElementById('colorNameField').value,
        colorFamily: document.getElementById('colorFamilyField').value,
        quantityMeters: parseInt(document.getElementById('quantityField').value),
        orderType: document.getElementById('orderTypeField').value,
        deadlineHours: parseInt(document.getElementById('deadlineField').value)
    };

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderData)
        });

        if (response.ok) {
            document.getElementById('orderModal').style.display = 'none';
            document.getElementById('orderForm').reset();
            refreshOrders();
            showPopup('Order created successfully!');
        } else {
            alert(await response.text());
        }
    } catch (err) { console.error(err); }
}

async function completeOrder(id) {
    try {
        const response = await fetch(`/api/orders/${id}/status?status=COMPLETED`, { method: 'PATCH' });
        if (response.ok) {
            refreshOrders();
            showPopup(`Order #${id} marked as COMPLETED.`);
            addActivityAlert(`Order #${id} production finished.`, 'eco');
        }
    } catch (err) { console.error(err); }
}

function renderOrderTable() {
    const tbody = document.getElementById('orderTableBody');
    tbody.innerHTML = '';

    allOrders.forEach(o => {
        const row = document.createElement('tr');
        const statusClass = `status-${o.status.toLowerCase()}`;

        row.innerHTML = `
            <td style="padding: 1rem;">#${o.id}</td>
            <td style="padding: 1rem;">${o.colorName}</td>
            <td style="padding: 1rem;"><div style="display:flex; align-items:center; gap:0.5rem;"><div style="width:12px; height:12px; border-radius:3px; background:${colorMap[o.colorFamily]}"></div> ${o.colorFamily}</div></td>
            <td style="padding: 1rem;">${o.quantityMeters}m</td>
            <td style="padding: 1rem;"><span class="badge badge-${o.orderType.toLowerCase()}">${o.orderType}</span></td>
            <td style="padding: 1rem;">${o.deadlineHours}h</td>
            <td style="padding: 1rem;"><span class="status-badge ${statusClass}">${o.status}</span></td>
            <td style="padding: 1rem;">
                ${o.status !== 'COMPLETED' ? `<button class="btn btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.7rem;" onclick="completeOrder(${o.id})">Complete</button>` : '--'}
            </td>
        `;
        tbody.appendChild(row);
    });
}

function updateCapacityUI() {
    const count = allOrders.length;
    const label = document.getElementById('capacityLabel');
    label.innerText = `Capacity: ${count}/100`;

    if (count >= 100) {
        label.style.color = '#ef4444';
    } else if (count >= 90) {
        label.style.color = '#f59e0b';
        addActivityAlert('FACTORY ALERT: Capacity at 90%.', 'warn');
    }
}

function updateActivityFeed() {
    const feed = document.getElementById('activityFeed');
    const completed = allOrders.filter(o => o.status === 'COMPLETED').length;
    if (completed > 0 && completed % 10 === 0) {
        addActivityAlert(`${completed} orders completed today.`, 'eco');
    }
}

function addActivityAlert(msg, type) {
    const feed = document.getElementById('activityFeed');
    if (feed.innerText === 'No recent alerts.') feed.innerHTML = '';

    const alert = document.createElement('div');
    alert.style.cssText = `
        padding: 0.75rem 1rem; border-radius: 1rem; font-size: 0.8rem;
        background: rgba(255,255,255,0.02); border-left: 3px solid ${type === 'warn' ? '#ef4444' : '#34d399'};
    `;
    alert.innerHTML = `<span style="color: var(--text-dim); margin-right: 0.5rem;">[${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}]</span> ${msg}`;
    feed.prepend(alert);
}

function showPopup(msg) {
    const p = document.createElement('div');
    p.className = 'glass';
    p.style.cssText = `position:fixed; top:2rem; right:2rem; padding:1.5rem; z-index:2000; border-radius:1.5rem; border-color:#34d399; color:#34d399; font-weight:700;`;
    p.innerText = msg;
    document.body.appendChild(p);
    setTimeout(() => p.remove(), 4000);
}

async function populateDemoData() {
    try {
        const btn = document.getElementById('populateBtn');
        btn.innerText = 'Populating...';
        btn.disabled = true;
        const response = await fetch('/api/demo/populate', { method: 'POST' });
        if (response.ok) {
            await refreshOrders();
            showPopup('100 production orders ready.');
            addActivityAlert('Factory intake complete: 100 orders.', 'eco');
        }
        btn.innerText = 'Populate Demo';
        btn.disabled = false;
    } catch (err) { console.error(err); }
}

async function simulatePeakHours() {
    try {
        const btn = document.getElementById('simulateBtn');
        const oldText = btn.innerHTML;
        btn.innerText = 'Simulating...';
        btn.disabled = true;

        const response = await fetch('/api/demo/simulate', { method: 'POST' });
        if (response.ok) {
            await refreshOrders();
            showPopup('100 Peak-Hour test cases generated!');
            addActivityAlert('SIMULATION: 100 high-urgency orders added.', 'warn');

            // Auto trigger optimization for simulation
            await generateSchedule();
        }
        btn.innerHTML = oldText;
        btn.disabled = false;
    } catch (err) { console.error(err); }
}

async function clearOrders() {
    if (!confirm('Clear all?')) return;
    try {
        const response = await fetch('/api/orders/clear', { method: 'DELETE' });
        if (response.ok) {
            refreshOrders();
            showPopup('Factory cleared.');
        }
    } catch (err) { console.error(err); }
}

async function generateSchedule() {
    try {
        const btn = document.getElementById('generateBtn');
        btn.innerText = 'Optimizing...';
        btn.disabled = true;
        const response = await fetch('/schedule/generate', { method: 'POST' });
        if (response.ok) {
            currentSchedule = await response.json();
            updateDashboard();
            renderTimeline();
            fetchComparison();
            refreshOrders();
            switchTab('dashboard');
            addActivityAlert('Optimization engine: Schedule updated.', 'eco');
        }
        btn.innerText = 'Generate Optimized';
        btn.disabled = false;
    } catch (err) { console.error(err); }
}

async function fetchComparison() {
    try {
        const response = await fetch('/schedule/compare');
        if (response.ok) {
            const data = await response.json();
            comparisonChart.data.datasets[0].data = [data.fifoCleaningTimeMinutes, data.optimizedCleaningTimeMinutes];
            comparisonChart.update();
        }
    } catch (err) { console.error(err); }
}

function initCharts() {
    const cfg = { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } }, x: { grid: { display: false }, ticks: { color: '#94a3b8' } } } };
    comparisonChart = new Chart(document.getElementById('comparisonChart').getContext('2d'), { type: 'bar', data: { labels: ['Baseline', 'Optimized'], datasets: [{ data: [0, 0], backgroundColor: ['rgba(248, 113, 113, 0.2)', 'rgba(52, 211, 153, 0.5)'], borderColor: ['#f87171', '#34d399'], borderWidth: 2, borderRadius: 10 }] }, options: cfg });
    priorityChart = new Chart(document.getElementById('priorityChart').getContext('2d'), { type: 'doughnut', data: { labels: ['Rush', 'Standard', 'Bulk'], datasets: [{ data: [0, 0, 0], backgroundColor: ['#fca5a5', '#fef9c3', '#dcfce7'], borderColor: 'transparent' }] }, options: { ...cfg, cutout: '70%', plugins: { legend: { display: true, position: 'bottom', labels: { color: '#94a3b8' } } } } });
    familyChart = new Chart(document.getElementById('familyChart').getContext('2d'), { type: 'bar', data: { labels: Object.keys(colorMap), datasets: [{ data: [0, 0, 0, 0, 0], backgroundColor: Object.values(colorMap), borderRadius: 5 }] }, options: { ...cfg, indexAxis: 'y' } });
}

function updateAnalyticsCharts() {
    if (!allOrders.length) return;
    priorityChart.data.datasets[0].data = ['RUSH', 'STANDARD', 'BULK'].map(t => allOrders.filter(o => o.orderType === t).length);
    priorityChart.update();
    familyChart.data.datasets[0].data = Object.keys(colorMap).map(f => allOrders.filter(o => o.colorFamily === f).length);
    familyChart.update();
}

function updateDashboard() {
    if (!currentSchedule) return;
    document.getElementById('ecoGradeValue').innerText = currentSchedule.ecoGrade;
    document.getElementById('cleaningSavedValue').innerText = currentSchedule.timeSavedMinutes + 'm';
    document.getElementById('complianceValue').innerText = currentSchedule.deadlineCompliance;
    document.getElementById('efficiencyValue').innerText = currentSchedule.machineEfficiency;

    // Add simulation results alert to activity feed if it looks like a simulation
    if (currentSchedule.schedule.length >= 100) {
        addActivityAlert(`Stats: Compliance ${currentSchedule.deadlineCompliance} | Efficiency ${currentSchedule.machineEfficiency}`, 'eco');
    }
}

function renderTimeline() {
    const container = document.getElementById('timelineBody');
    container.innerHTML = '';
    if (!currentSchedule || !currentSchedule.schedule) return;
    currentSchedule.schedule.forEach(slot => {
        const row = document.createElement('div');
        row.className = 'timeline-row';
        const cleaningWidth = (slot.cleaningBeforeMinutes / 120) * 100;
        row.innerHTML = `<div class="row-label"><span>#${slot.orderId}</span><span class="slot-range">${slot.startTime} - ${slot.endTime}</span></div><div class="production-bar-container"><div class="production-bar" style="background-color: ${colorMap[slot.colorFamily]}; width: 100%;">${slot.cleaningBeforeMinutes > 0 ? `<div class="cleaning-indicator" style="width: ${cleaningWidth}%">CLEANING</div>` : ''}<span style="margin-left: ${slot.cleaningBeforeMinutes > 0 ? cleaningWidth + 2 : 0}%">${slot.colorFamily}</span></div></div>`;
        container.appendChild(row);
    });
}

// Global for inline onclick
window.completeOrder = completeOrder;
