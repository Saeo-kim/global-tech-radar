/* Global Work Market: lazy per-tab charts, rate calculator, project filter. */
(function () {
  'use strict';
  const G = window.GTR;
  const built = {};

  document.addEventListener('DOMContentLoaded', async () => {
    const trend = await G.fetchJSON('/api/trends/developer_market');
    const chart = G.lineChart(document.getElementById('market-trend'), trend, { max: 100 });
    G.legend(document.getElementById('market-legend'), chart);
    G.provenanceLine(document.getElementById('market-provenance'), trend);

    const active = document.querySelector('.tab.active')?.dataset.tab || 'remote';
    build(active);
    document.addEventListener('gtr:tab', (e) => build(e.detail));
    initCalculator();
    initProjects();
  });

  async function build(tab) {
    if (built[tab]) return;
    built[tab] = true;
    if (tab === 'remote') {
      const rows = (await G.fetchJSON('/api/market/remote_jobs')).filter((r) => r.region === 'Global');
      hbar('remote-role', rows.map((r) => r.title), rows.map((r) => r.metricValue), '%', 50);
    }
    if (tab === 'freelance') {
      const rows = await G.fetchJSON('/api/market/freelance');
      new Chart(document.getElementById('freelance-chart'), {
        type: 'bar',
        data: { labels: rows.map((r) => r.title), datasets: [{ label: 'Hourly band (USD)', data: rows.map((r) => [r.metricValue, r.secondaryValue]), backgroundColor: G.accent(), barPercentage: .6, borderSkipped: false }] },
        options: {
          indexAxis: 'y',
          plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => ' $' + ctx.raw[0] + '–$' + ctx.raw[1] + ' per hour (sample)' } } },
          scales: { x: { min: 0, ticks: { callback: (v) => '$' + v } }, y: { grid: { display: false } } },
        },
      });
    }
    if (tab === 'salaries') {
      const rows = await G.fetchJSON('/api/market/salary');
      const countries = Object.fromEntries((await G.fetchJSON('/api/countries')).map((c) => [c.code, c]));
      const chart = new Chart(document.getElementById('salary-chart'), {
        type: 'bar',
        data: {
          labels: rows.map((r) => (countries[r.region]?.flag || '') + ' ' + r.region),
          datasets: [
            { label: 'Software engineer', data: rows.map((r) => r.metricValue), backgroundColor: G.color(0), borderColor: G.color(0), barPercentage: .8, categoryPercentage: .7 },
            { label: 'AI/ML engineer', data: rows.map((r) => r.secondaryValue), backgroundColor: G.color(1), borderColor: G.color(1), barPercentage: .8, categoryPercentage: .7 },
          ],
        },
        options: {
          plugins: { legend: { display: false }, tooltip: { callbacks: {
            title: (i) => countries[rows[i[0].dataIndex].region]?.name || rows[i[0].dataIndex].region,
            label: (ctx) => ' ' + ctx.dataset.label + ': $' + G.fmt(ctx.parsed.y) } } },
          scales: { y: { beginAtZero: true, ticks: { callback: (v) => G.money(v) } }, x: { grid: { display: false } } },
        },
      });
      G.legend(document.getElementById('salary-legend'), chart);
    }
    if (tab === 'skills') {
      const rows = await G.fetchJSON('/api/market/skill_demand');
      let chart;
      const draw = (by) => {
        const sorted = rows.slice().sort((a, b) => by === 'growth' ? b.secondaryValue - a.secondaryValue : b.metricValue - a.metricValue);
        const vals = sorted.map((r) => by === 'growth' ? r.secondaryValue : r.metricValue);
        if (chart) chart.destroy();
        chart = hbar('skills-chart', sorted.map((r) => r.title), vals, by === 'growth' ? '% YoY' : '', by === 'growth' ? 60 : 100);
      };
      document.querySelectorAll('#skill-sort .chip').forEach((c) => c.addEventListener('click', () => {
        document.querySelectorAll('#skill-sort .chip').forEach((x) => x.classList.toggle('active', x === c));
        draw(c.dataset.sort);
      }));
      draw('demand');
    }
  }

  function hbar(id, labels, values, unit, max) {
    return new Chart(document.getElementById(id), {
      type: 'bar',
      data: { labels, datasets: [{ data: values, backgroundColor: G.accent(), barPercentage: .7, categoryPercentage: .85 }] },
      options: {
        indexAxis: 'y',
        plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => ' ' + G.fmt(ctx.parsed.x, 1) + (unit ? ' ' + unit : '') + ' (sample)' } } },
        scales: { x: { beginAtZero: true, max }, y: { grid: { display: false } } },
      },
    });
  }

  function initCalculator() {
    const role = document.getElementById('calc-role');
    if (!role) return;
    const pos = document.getElementById('calc-pos');
    const hours = document.getElementById('calc-hours');
    const run = () => {
      const [lo, hi] = role.value.split('-').map(Number);
      const p = Number(pos.value) / 100;
      const rate = lo + (hi - lo) * p;
      const h = Math.max(0, Math.min(60, Number(hours.value) || 0));
      document.getElementById('calc-pos-label').textContent = pos.value + '%';
      document.getElementById('calc-out').textContent = '$' + G.fmt(rate * h * 4.33);
      document.getElementById('calc-rate').textContent = 'at $' + G.fmt(rate) + '/hr · ' + h + ' h/week · 4.33 weeks/month';
    };
    [role, pos, hours].forEach((e) => e.addEventListener('input', run));
    run();
  }

  function initProjects() {
    const input = document.getElementById('project-filter');
    if (!input) return;
    const cards = document.querySelectorAll('#project-cards [data-search]');
    const count = document.getElementById('project-count');
    const run = G.filterList(input, cards, (el) => el.dataset.search);
    const update = () => { count.textContent = run() + ' of ' + cards.length + ' archetypes'; };
    input.addEventListener('input', update);
    update();
  }
})();
