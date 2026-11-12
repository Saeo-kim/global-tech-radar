/* My Technology Position: proficiency slider label + "you vs market" chart. */
(function () {
  'use strict';
  const G = window.GTR;
  const LABELS = ['', 'Beginner', 'Elementary', 'Intermediate', 'Advanced', 'Expert'];

  document.addEventListener('DOMContentLoaded', async () => {
    const range = document.getElementById('prof-range');
    const label = document.getElementById('prof-label');
    if (range && label) {
      const run = () => { label.textContent = range.value + ' · ' + LABELS[range.value]; };
      range.addEventListener('input', run); run();
    }

    const a = await G.fetchJSON('/api/position/analysis');
    const top = a.comparisons.slice(0, 12);
    const chart = new Chart(document.getElementById('gap-chart'), {
      type: 'bar',
      data: {
        labels: top.map((c) => c.skill),
        datasets: [
          { label: 'Market demand', data: top.map((c) => c.marketDemand), backgroundColor: G.color(0), borderColor: G.color(0), barPercentage: .8, categoryPercentage: .72 },
          { label: 'Your level', data: top.map((c) => c.userLevel), backgroundColor: G.color(1), borderColor: G.color(1), barPercentage: .8, categoryPercentage: .72 },
        ],
      },
      options: {
        indexAxis: 'y',
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { afterBody: (items) => { const c = top[items[0].dataIndex]; return 'Gap ' + c.gap + ' · ' + c.status + (c.goalSkill ? ' · goal skill' : ''); } } },
        },
        scales: { x: { min: 0, max: 100 }, y: { grid: { display: false } } },
      },
    });
    G.legend(document.getElementById('gap-legend'), chart);
  });
})();
