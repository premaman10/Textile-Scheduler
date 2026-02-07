// State Management
let currentSchedule = null;
let allOrders = [];
let comparisonChart = null;

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
    document.querySelectorAll('.nav-link').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    // Core Actions
    document.getElementById('simulateBtn').addEventListener('click', simulatePeakHours);
    document.getElementById('generateBtn').addEventListener('click', generateSchedule);
    document.getElementById('clearOrdersBtn').addEventListener('click', clearOrders);

    // Modal
    document.getElementById('openAddOrderBtn').addEventListener('click', () => {
        switchTab('orders');
    });

    document.getElementById('orderForm').addEventListener('submit', handleManualOrder);

    // Simulation Modal
    if (document.getElementById('closeSimModal')) {
        document.getElementById('closeSimModal').addEventListener('click', () => {
            document.getElementById('simDetailModal').style.display = 'none';
        });
    }
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-link').forEach(i => i.classList.remove('active'));
    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');

    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById(tabId + 'Tab').classList.add('active');

    // Refresh specifics
    if (tabId === 'simulations') fetchSimulations();
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
            <td>${order.id}</td>
            <td>${order.colorName}</td>
            <td><div style="width:12px; height:12px; border-radius:3px; background:${colorMap[order.colorFamily] || '#ccc'}; display:inline-block; margin-right:5px;"></div> ${familyLabels[order.colorFamily]}</td>
            <td>${order.quantityMeters}</td>
            <td><span class="badge badge-${order.orderType.toLowerCase()}">${order.orderType}</span></td>
            <td>${order.deadlineHours}h</td>
            <td><span class="status-badge status-${order.status.toLowerCase()}">${order.status}</span></td>
            <td>
                ${order.status !== 'COMPLETED' ? `<button onclick="completeOrder(${order.id})" class="btn btn-secondary" style="padding:0.25rem 0.6rem; font-size:0.7rem;">Done</button>` : '✓'}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function completeOrder(id) {
    try {
        const response = await fetch(`/api/orders/${id}/status?status=COMPLETED`, { method: 'PATCH' });
        if (response.ok) {
            addActivityAlert(`Order #${id} marked as Completed.`, 'info');
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
        deadlineHours: 24 // Default fallback
    };

    // Parse deadline
    const dateInput = document.getElementById('deadlineDate').value;
    if (dateInput) {
        const diff = new Date(dateInput) - new Date();
        const hours = Math.floor(diff / (1000 * 60 * 60));
        order.deadlineHours = Math.max(1, hours);
    }

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(order)
        });
        if (response.ok) {
            document.getElementById('orderForm').reset();
            refreshOrders();
            showPopup('New Production Order Added.');
            addActivityAlert(`Manual Intake: ${order.colorName}`, 'info');
        }
    } catch (err) { console.error(err); }
}



async function simulatePeakHours() {
    const btn = document.getElementById('simulateBtn');
    const oldText = btn.innerHTML;
    try {
        btn.innerHTML = '<i data-lucide="loader-2" class="spin" style="width:16px; margin-right:5px;"></i> Simulating...';
        btn.disabled = true;

        const response = await fetch('/api/simulations/run', { method: 'POST' });
        if (response.ok) {
            await refreshOrders();
            showPopup('100 Peak-Hour test cases generated! Click "Generate Optimized" to process.');
            addActivityAlert('SIMULATION started: 1000 orders.', 'warn');

            fetchSimulations();
        } else {
            showPopup('Simulation failed to start');
        }
    } catch (err) {
        console.error(err);
        showPopup('Error: ' + err.message);
    } finally {
        btn.innerHTML = oldText;
        btn.disabled = false;
        lucide.createIcons();
    }
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
            addActivityAlert(`Optimization Finished. Savings: ${currentSchedule.timeSavedMinutes}m`, 'info');
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
        document.getElementById('timelineBody').innerHTML = '';
        updateDashboard();
    } catch (err) { console.error(err); }
}

function updateDashboard() {
    if (!currentSchedule) {
        document.getElementById('cleaningSavedValue').innerText = '0m';
        document.getElementById('complianceValue').innerText = '0%';
        document.getElementById('efficiencyValue').innerText = '0%';
        if (comparisonChart) {
            comparisonChart.data.datasets[0].data = [0, 0];
            comparisonChart.update();
        }
        return;
    }
    document.getElementById('cleaningSavedValue').innerText = currentSchedule.timeSavedMinutes + 'm';
    document.getElementById('complianceValue').innerText = currentSchedule.deadlineCompliance;
    document.getElementById('efficiencyValue').innerText = currentSchedule.machineEfficiency;

    // Update Schedule Stats
    if (document.getElementById('optCleaningVal')) {
        document.getElementById('optCleaningVal').innerText = currentSchedule.optimizedCleaningTimeMinutes + 'm';
        document.getElementById('fifoCleaningVal').innerText = currentSchedule.fifoCleaningTimeMinutes + 'm';
        document.getElementById('cleaningSavedVal2').innerText = currentSchedule.timeSavedMinutes + 'm';
        document.getElementById('effVal2').innerText = currentSchedule.machineEfficiency;
        document.getElementById('compVal2').innerText = currentSchedule.deadlineCompliance;
    }

    updateCharts();
}

function initCharts() {
    const ctxComp = document.getElementById('comparisonChart');
    if (!ctxComp) return;

    comparisonChart = new Chart(ctxComp.getContext('2d'), {
        type: 'bar',
        data: {
            labels: ['Baseline (FIFO)', 'RainBow Optimizer'],
            datasets: [{
                label: 'Cleaning Downtime (min)',
                data: [0, 0],
                backgroundColor: ['#e2e8f0', '#7c3aed'],
                borderRadius: 4
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, grid: { color: '#f1f5f9' } }, x: { grid: { display: false } } },
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

function updateCharts() {
    if (!currentSchedule || !comparisonChart) return;
    comparisonChart.data.datasets[0].data = [currentSchedule.fifoCleaningTimeMinutes, currentSchedule.optimizedCleaningTimeMinutes];
    comparisonChart.update();
}

function renderTimeline() {
    const body = document.getElementById('timelineBody');
    body.innerHTML = '';

    currentSchedule.schedule.forEach((slot, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${index + 1}</td>
            <td>Order #${slot.orderId}</td>
            <td><div style="width:12px; height:12px; border-radius:3px; background:${colorMap[slot.colorFamily] || '#ccc'}; display:inline-block; margin-right:5px;"></div> ${slot.colorFamily}</td>
            <td>${slot.colorFamily}</td>
            <td>${new Date(slot.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
            <td><span class="badge" style="background:#f1f5f9;">${slot.cleaningBeforeMinutes}m</span></td>
        `;
        body.appendChild(tr);
    });
}

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
        container.innerHTML = '<div style="color:var(--text-secondary); width: 100%;">No simulations run yet. Click "Peak Simulation" to start.</div>';
        return;
    }

    sims.forEach(sim => {
        const card = document.createElement('div');
        card.className = 'stat-card';
        card.style.cursor = 'pointer';
        card.onclick = () => showSimulationDetails(sim);

        card.innerHTML = `
            <div class="stat-header" style="display:flex; justify-content:space-between; align-items:center;">
                <span style="font-weight:600;">${sim.name}</span>
                <i data-lucide="chevron-right" style="width:16px;"></i>
            </div>
            <div style="font-size: 0.75rem; color: var(--text-secondary); margin-bottom: 0.5rem;">${new Date(sim.timestamp).toLocaleString()}</div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem;">
                <div>
                    <div class="stat-label">COMPLIANCE</div>
                    <div style="font-weight: 700; color: var(--success);">${sim.deadlineCompliance || 'N/A'}</div>
                </div>
                <div>
                    <div class="stat-label">EFFICIENCY</div>
                    <div style="font-weight: 700;">${sim.machineEfficiency || 'N/A'}</div>
                </div>
            </div>
        `;
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
                <td>${o.id}</td>
                <td>${o.colorFamily}</td>
                <td>${o.quantityMeters}m</td>
                <td>${o.orderType}</td>
                <td>${o.deadlineHours}h</td>
                <td style="color: var(--text-secondary);">${new Date(o.createdAt).toLocaleTimeString()}</td>
                <td style="font-weight: 600; color: var(--primary);">${o.scheduledStartTime ? new Date(o.scheduledStartTime).toLocaleTimeString() : 'Pending'}</td>
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
    if (count >= 100) label.style.color = 'var(--danger)';
    else if (count >= 90) label.style.color = 'var(--warning)';
    else label.style.color = 'var(--text-secondary)';
}

function showPopup(msg) {
    const p = document.createElement('div');
    p.innerText = msg;
    p.style.position = 'fixed';
    p.style.bottom = '2rem';
    p.style.right = '2rem';
    p.style.background = '#1e293b';
    p.style.color = 'white';
    p.style.padding = '1rem 2rem';
    p.style.borderRadius = '0.5rem';
    p.style.fontWeight = '500';
    p.style.boxShadow = '0 10px 15px -3px rgba(0,0,0,0.1)';
    p.style.zIndex = '9999';
    p.style.animation = 'fadeIn 0.3s ease';
    document.body.appendChild(p);
    setTimeout(() => p.remove(), 3000);
}

function addActivityAlert(msg, type) {
    const feed = document.getElementById('activityFeed');
    const alert = document.createElement('div');
    alert.style.padding = '0.75rem';
    alert.style.borderRadius = '0.5rem';
    alert.style.fontSize = '0.875rem';
    alert.style.background = 'white';
    alert.style.border = '1px solid var(--border)';
    alert.style.borderLeft = `3px solid ${type === 'info' ? 'var(--primary)' : type === 'warn' ? 'var(--warning)' : 'var(--primary)'}`;
    alert.innerHTML = `<span style="color:var(--text-secondary); margin-right:5px; font-size:0.75rem;">${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span> ${msg}`;
    feed.prepend(alert);
    if (feed.children.length > 5) feed.lastElementChild.remove();
}
