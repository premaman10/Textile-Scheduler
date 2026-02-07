// State Management
let currentSchedule = null;
let allOrders = [];
let comparisonChart = null;
let priorityChart = null;
let familyChart = null;

const colorMap = {
    'WHITES_PASTELS': '#f8fafc',
    'LIGHT_COLORS': '#bae6fd',
    'MEDIUM_COLORS': '#bae6fd',
    'DARK_COLORS': '#1e293b',
    'BLACKS_DEEP_DARKS': '#020617'
};

const familyLabels = {
    'WHITES_PASTELS': 'Whites & Pastels',
    'LIGHT_COLORS': 'Light Colors',
    'MEDIUM_COLORS': 'Medium Colors',
    'DARK_COLORS': 'Dark Colors',
    'BLACKS_DEEP_DARKS': 'Blacks & Deep Darks'
};

document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();
    initCharts();
    setupEventListeners();
    refreshOrders();
    fetchSimulations();
});

function setupEventListeners() {
    // Tab Switching
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    // Core Actions
    document.getElementById('populateBtn').addEventListener('click', populateDemoData);
    document.getElementById('simulateBtn').addEventListener('click', simulatePeakHours);
    document.getElementById('generateBtn').addEventListener('click', generateSchedule);
    document.getElementById('clearOrdersBtn').addEventListener('click', clearOrders);

    // Modal
    document.getElementById('addOrderBtn').addEventListener('click', () => {
        document.getElementById('orderModal').style.display = 'flex';
    });
    document.getElementById('closeModal').addEventListener('click', () => {
        document.getElementById('orderModal').style.display = 'none';
    });
    document.getElementById('orderForm').addEventListener('submit', handleManualOrder);

    // Simulation Modal
    document.getElementById('closeSimModal').addEventListener('click', () => {
        document.getElementById('simDetailModal').style.display = 'none';
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');

    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById(tabId + 'Tab').classList.add('active');

    document.getElementById('pageTitle').innerText = tabId.charAt(0).toUpperCase() + tabId.slice(1);

    // Refresh specifics
    if (tabId === 'simulations') fetchSimulations();
    if (tabId === 'analytics') updateAnalytics();
}

async function refreshOrders() {
    try {
        const response = await fetch('/api/orders');
        allOrders = await response.json();
        renderOrderTable();
        updateCapacity();
    } catch (err) { console.error(err); }
}

function renderOrderTable() {
    const tbody = document.getElementById('orderTableBody');
    tbody.innerHTML = '';

    allOrders.forEach(order => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="padding: 1rem;">${order.id}</td>
            <td style="padding: 1rem;">${order.colorName}</td>
            <td style="padding: 1rem;"><div style="width:12px; height:12px; border-radius:3px; background:${colorMap[order.colorFamily] || '#ccc'}; display:inline-block; margin-right:5px;"></div> ${familyLabels[order.colorFamily]}</td>
            <td style="padding: 1rem;">${order.quantityMeters}</td>
            <td style="padding: 1rem;"><span class="badge badge-${order.orderType.toLowerCase()}">${order.orderType}</span></td>
            <td style="padding: 1rem;">${order.deadlineHours}h</td>
            <td style="padding: 1rem;"><span class="status-badge status-${order.status.toLowerCase()}">${order.status}</span></td>
            <td style="padding: 1rem;">
                ${order.status !== 'COMPLETED' ? `<button onclick="completeOrder(${order.id})" class="btn" style="padding:0.25rem 0.6rem; font-size:0.7rem; border:1px solid var(--glass-border);">Done</button>` : '✓'}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function completeOrder(id) {
    try {
        const response = await fetch(`/api/orders/${id}/status?status=COMPLETED`, { method: 'PATCH' });
        if (response.ok) {
            addActivityAlert(`Order #${id} marked as Completed.`, 'eco');
            refreshOrders();
        }
    } catch (err) { console.error(err); }
}

async function handleManualOrder(e) {
    e.preventDefault();
    if (allOrders.length >= 100) {
        showPopup('Factory Capacity Reached (100/100)!');
        return;
    }

    const order = {
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
            body: JSON.stringify(order)
        });
        if (response.ok) {
            document.getElementById('orderModal').style.display = 'none';
            document.getElementById('orderForm').reset();
            refreshOrders();
            showPopup('New Production Order Added.');
            addActivityAlert(`Manual Intake: ${order.colorName}`, 'eco');
        }
    } catch (err) { console.error(err); }
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
            addActivityAlert('SIMULATION started: 100 orders.', 'warn');

            // Auto optimize
            await generateSchedule();
            fetchSimulations();
        }
        btn.innerHTML = oldText;
        btn.disabled = false;
    } catch (err) { console.error(err); }
}

async function generateSchedule() {
    try {
        const btn = document.getElementById('generateBtn');
        btn.innerText = 'Optimizing...';
        const response = await fetch('/api/schedule/generate', { method: 'POST' });
        if (response.ok) {
            currentSchedule = await response.json();
            updateDashboard();
            renderTimeline();
            showPopup('Dynamic Schedule Generated.');
            addActivityAlert(`Optimization Finised. Savings: ${currentSchedule.timeSavedMinutes}m`, 'eco');
            refreshOrders();
        }
        btn.innerText = 'Generate Optimized';
    } catch (err) { console.error(err); }
}

async function clearOrders() {
    if (!confirm('Clear all?')) return;
    try {
        await fetch('/api/orders/clear', { method: 'DELETE' });
        refreshOrders();
        currentSchedule = null;
        document.getElementById('timelineBody').innerHTML = 'Cleared.';
    } catch (err) { console.error(err); }
}

function updateDashboard() {
    if (!currentSchedule) return;
    document.getElementById('ecoGradeValue').innerText = currentSchedule.ecoGrade;
    document.getElementById('cleaningSavedValue').innerText = currentSchedule.timeSavedMinutes + 'm';
    document.getElementById('complianceValue').innerText = currentSchedule.deadlineCompliance;
    document.getElementById('efficiencyValue').innerText = currentSchedule.machineEfficiency;

    updateCharts();
}

function initCharts() {
    const ctxComp = document.getElementById('comparisonChart').getContext('2d');
    comparisonChart = new Chart(ctxComp, {
        type: 'bar',
        data: {
            labels: ['Baseline (FIFO)', 'RainBow Optimizer'],
            datasets: [{
                label: 'Cleaning Downtime (min)',
                data: [0, 0],
                backgroundColor: ['rgba(255, 255, 255, 0.05)', '#f59e0b'],
                borderRadius: 10
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' } } }
        }
    });

    const ctxPrio = document.getElementById('priorityChart').getContext('2d');
    priorityChart = new Chart(ctxPrio, {
        type: 'doughnut',
        data: {
            labels: ['Rush', 'Standard', 'Bulk'],
            datasets: [{
                data: [0, 0, 0],
                backgroundColor: ['#f87171', '#fbbf24', '#34d399'],
                borderWidth: 0
            }]
        }
    });

    const ctxFam = document.getElementById('familyChart').getContext('2d');
    familyChart = new Chart(ctxFam, {
        type: 'bar',
        data: {
            labels: Object.values(familyLabels),
            datasets: [{
                label: 'Orders',
                data: [0, 0, 0, 0, 0],
                backgroundColor: '#34d399'
            }]
        }
    });
}

function updateCharts() {
    if (!currentSchedule) return;
    comparisonChart.data.datasets[0].data = [currentSchedule.fifoCleaningTimeMinutes, currentSchedule.optimizedCleaningTimeMinutes];
    comparisonChart.update();
}

function updateAnalytics() {
    if (allOrders.length === 0) return;

    // Priority Distribution
    const counts = { RUSH: 0, STANDARD: 0, BULK: 0 };
    allOrders.forEach(o => counts[o.orderType]++);
    priorityChart.data.datasets[0].data = [counts.RUSH, counts.STANDARD, counts.BULK];
    priorityChart.update();

    // Family Load
    const famCounts = {};
    allOrders.forEach(o => famCounts[o.colorFamily] = (famCounts[o.colorFamily] || 0) + 1);
    familyChart.data.datasets[0].data = Object.keys(familyLabels).map(f => famCounts[f] || 0);
    familyChart.update();
}

function renderTimeline() {
    const body = document.getElementById('timelineBody');
    body.innerHTML = '';

    currentSchedule.schedule.forEach((slot, index) => {
        const row = document.createElement('div');
        row.className = 'timeline-row';
        row.innerHTML = `
            <div class="row-label">
                <span>Batch #${index + 1}</span>
                <span class="slot-range">${new Date(slot.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${new Date(slot.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
            </div>
            <div class="production-bar-container">
                ${slot.cleaningBeforeMinutes > 0 ? `<div class="cleaning-indicator" style="width: ${Math.min(100, (slot.cleaningBeforeMinutes / 120) * 100)}%">${slot.cleaningBeforeMinutes}m</div>` : ''}
                <div class="production-bar" style="background: ${colorMap[slot.colorFamily]}; width: 100%;">
                    Order #${slot.orderId} (${slot.colorFamily})
                </div>
            </div>
        `;
        body.appendChild(row);
    });
}

// SIMULATION HISTORY LOGIC
async function fetchSimulations() {
    try {
        const response = await fetch('/api/simulations');
        const sims = await response.json();
        renderSimulations(sims);
    } catch (err) { console.error(err); }
}

function renderSimulations(sims) {
    const container = document.getElementById('simHistoryList');
    container.innerHTML = '';

    if (sims.length === 0) {
        container.innerHTML = '<div style="color:var(--text-dim);">No simulations run yet. Click "Peak Simulation" to start.</div>';
        return;
    }

    sims.forEach(sim => {
        const card = document.createElement('div');
        card.className = 'glass stat-card';
        card.style.cursor = 'pointer';
        card.style.transition = 'transform 0.2s';
        card.onmouseover = () => card.style.transform = 'translateY(-5px)';
        card.onmouseout = () => card.style.transform = 'translateY(0)';

        card.innerHTML = `
            <div class="stat-header">
                <i data-lucide="database"></i>
                <span>${sim.name}</span>
            </div>
            <div style="font-size: 0.7rem; color: var(--text-dim); margin-top: -0.5rem;">${new Date(sim.timestamp).toLocaleString()}</div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem;">
                <div>
                    <div style="font-size: 0.6rem; color: var(--text-dim);">COMPLIANCE</div>
                    <div style="font-weight: 700; color: var(--eco-green);">${sim.deadlineCompliance || 'N/A'}</div>
                </div>
                <div>
                    <div style="font-size: 0.6rem; color: var(--text-dim);">EFFICIENCY</div>
                    <div style="font-weight: 700;">${sim.machineEfficiency || 'N/A'}</div>
                </div>
            </div>
            <div style="font-size: 0.6rem; color: var(--text-dim); margin-top: 1rem; text-align: right;">Click to view 100 orders Array →</div>
        `;
        card.addEventListener('click', () => showSimulationDetails(sim));
        container.appendChild(card);
    });
    lucide.createIcons();
}

async function showSimulationDetails(sim) {
    try {
        const response = await fetch(`/api/simulations/${sim.id}/orders`);
        const orders = await response.json();

        document.getElementById('simDetailTitle').innerText = `${sim.name} Order Array (${orders.length} items)`;
        const tbody = document.getElementById('simDetailBody');
        tbody.innerHTML = '';

        orders.forEach(o => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02);">${o.id}</td>
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02);">${o.colorFamily}</td>
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02);">${o.quantityMeters}m</td>
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02);">${o.orderType}</td>
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02);">${o.deadlineHours}h</td>
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02);; font-size: 0.7rem; color: var(--text-dim);">${new Date(o.createdAt).toLocaleTimeString()}</td>
                <td style="padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.02); font-weight: 600; color: var(--primary);">${o.scheduledStartTime ? new Date(o.scheduledStartTime).toLocaleTimeString() : 'Pending'}</td>
            `;
            tbody.appendChild(tr);
        });

        document.getElementById('simDetailModal').style.display = 'flex';
    } catch (err) { console.error(err); }
}

function updateCapacity() {
    const label = document.getElementById('capacityLabel');
    const count = allOrders.length;
    label.innerText = `Capacity: ${count}/100`;
    if (count >= 100) label.style.color = '#f87171';
    else if (count >= 90) label.style.color = '#fbbf24';
    else label.style.color = 'var(--text-dim)';
}

function showPopup(msg) {
    const p = document.createElement('div');
    p.innerText = msg;
    p.style.position = 'fixed';
    p.style.bottom = '2rem';
    p.style.right = '2rem';
    p.style.background = 'var(--primary)';
    p.style.color = '#000';
    p.style.padding = '1rem 2rem';
    p.style.borderRadius = '1rem';
    p.style.fontWeight = '700';
    p.style.boxShadow = '0 10px 40px rgba(0,0,0,0.5)';
    p.style.zIndex = '9999';
    p.style.animation = 'fadeIn 0.3s ease';
    document.body.appendChild(p);
    setTimeout(() => p.remove(), 3000);
}

function addActivityAlert(msg, type) {
    const feed = document.getElementById('activityFeed');
    const alert = document.createElement('div');
    alert.style.padding = '0.5rem 0.8rem';
    alert.style.borderRadius = '0.75rem';
    alert.style.fontSize = '0.75rem';
    alert.style.background = 'rgba(255,255,255,0.03)';
    alert.style.borderLeft = `3px solid ${type === 'eco' ? 'var(--eco-green)' : type === 'warn' ? '#f87171' : 'var(--primary)'}`;
    alert.innerHTML = `<span style="opacity:0.6; margin-right:5px;">${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span> ${msg}`;
    feed.prepend(alert);
    if (feed.children.length > 5) feed.lastElementChild.remove();
}
