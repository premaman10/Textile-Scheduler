document.addEventListener('DOMContentLoaded', () => {
    // State management
    const state = {
        schedule: null,
        activeTab: 'dashboard'
    };

    // DOM Elements
    const elements = {
        generateBtn: document.getElementById('generateSchedule'),
        populateBtn: document.getElementById('populateData'),
        waterSaved: document.getElementById('waterSaved'),
        wasteSaved: document.getElementById('wasteSaved'),
        timeSaved: document.getElementById('timeSaved'),
        ecoGrade: document.getElementById('ecoGrade'),
        timelineContainer: document.getElementById('timelineContainer'),
        tabs: document.querySelectorAll('.nav-item'),
        tabContents: document.querySelectorAll('.tab-content')
    };

    // Tab Interface
    elements.tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            e.preventDefault();
            const target = tab.dataset.tab;

            elements.tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            elements.tabContents.forEach(content => {
                content.classList.remove('active');
                if (content.id === target) content.classList.add('active');
            });

            document.querySelector('h1').innerText = target.charAt(0).toUpperCase() + target.slice(1);
        });
    });

    // API Handlers
    async function populateDemo() {
        try {
            const resp = await fetch('/api/demo/populate', { method: 'POST' });
            const msg = await resp.text();
            alert(msg);
        } catch (err) {
            console.error('Error populating data:', err);
        }
    }

    async function generateSchedule() {
        elements.generateBtn.disabled = true;
        elements.generateBtn.innerHTML = '<i data-lucide="loader-2" class="animate-spin"></i> Processing...';
        lucide.createIcons();

        try {
            const resp = await fetch('/schedule/generate', { method: 'POST' });
            const data = await resp.json();
            state.schedule = data;
            updateUI(data);
        } catch (err) {
            console.error('Error generating schedule:', err);
        } finally {
            elements.generateBtn.disabled = false;
            elements.generateBtn.innerHTML = '<i data-lucide="zap"></i> Generate Optimized';
            lucide.createIcons();
        }
    }

    function updateUI(data) {
        // Update Stats
        elements.waterSaved.innerText = data.waterSavedLiters.toLocaleString();
        elements.wasteSaved.innerText = data.chemicalWasteSavedKg.toFixed(1);
        elements.timeSaved.innerText = data.timeSavedMinutes;

        // Update Grade
        elements.ecoGrade.className = `stat-card glass grade-${data.ecoGrade.toLowerCase()}`;
        elements.ecoGrade.querySelector('.stat-value').innerText = data.ecoGrade;

        // Render Timeline
        renderTimeline(data.schedule);

        // Render Chart
        renderChart(data);
    }

    function renderTimeline(slots) {
        elements.timelineContainer.innerHTML = '';
        slots.forEach(slot => {
            const div = document.createElement('div');
            div.className = 'timeline-slot';
            div.innerHTML = `
                <div class="slot-time">${slot.startTime} - ${slot.endTime}</div>
                <div class="slot-color" style="background: ${getColorForFamily(slot.colorFamily)}"></div>
                <div class="slot-info">
                    <div class="slot-title">Order #${slot.orderId} - ${slot.colorFamily}</div>
                </div>
                ${slot.cleaningBeforeMinutes > 0 ? `<div class="cleaning-badge">Cleaning: ${slot.cleaningBeforeMinutes}m</div>` : ''}
            `;
            elements.timelineContainer.appendChild(div);
        });
    }

    function getColorForFamily(family) {
        const colors = {
            'WHITES_PASTELS': '#f8fafc',
            'LIGHT_COLORS': '#bae6fd',
            'MEDIUM_COLORS': '#7dd3fc',
            'DARK_COLORS': '#0369a1',
            'BLACKS_DEEP_DARKS': '#020617'
        };
        return colors[family] || '#fff';
    }

    let comparisonChart = null;
    function renderChart(data) {
        const ctx = document.getElementById('comparisonChart').getContext('2d');

        if (comparisonChart) comparisonChart.destroy();

        comparisonChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['FIFO Logic (Standard)', 'Optimized (Eco-Smart)'],
                datasets: [{
                    label: 'Total Cleaning Time (Minutes)',
                    data: [data.fifoCleaningTimeMinutes, data.optimizedCleaningTimeMinutes],
                    backgroundColor: ['rgba(255, 255, 255, 0.1)', '#10b981'],
                    borderColor: ['rgba(255, 255, 255, 0.2)', '#059669'],
                    borderWidth: 1,
                    borderRadius: 10
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } },
                    x: { ticks: { color: '#94a3b8' } }
                }
            }
        });
    }

    // Event Listeners
    elements.generateBtn.addEventListener('click', generateSchedule);
    elements.populateBtn.addEventListener('click', populateDemo);
});
