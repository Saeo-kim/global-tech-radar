/* Company Intelligence: ranking bar, comparison radar (max 3), card filter, detail radar. */
(function () {
  'use strict';
  const G = window.GTR;
  const AXES = [['aiInvestmentIndex', 'AI investment'], ['talentDemandIndex', 'Talent demand'], ['rndIndex', 'R&D'],
    ['ecosystemIndex', 'Ecosystem'], ['hiringMomentum', 'Hiring']];
  const composite = (c) => Math.round(AXES.reduce((s, a) => s + c[a[0]], 0) / AXES.length);

  const radarOpts = () => ({
    plugins: { legend: { display: false } },
    scales: { r: { min: 40, max: 100, ticks: { stepSize: 20, backdropColor: 'transparent', color: '#6f84a5' },
      grid: { color: 'rgba(42,64,105,.6)' }, angleLines: { color: 'rgba(42,64,105,.6)' },
      pointLabels: { color: '#a3b3cc', font: { size: 11.5, weight: '600' } } } },
  });
  const ds = (label, data, color, dashed) => ({ label, data, borderColor: color, backgroundColor: color + '22', borderWidth: 2,
    pointRadius: 3, pointBackgroundColor: color, borderDash: dashed ? [5, 4] : [] });

  document.addEventListener('DOMContentLoaded', async () => {
    const filter = document.getElementById('company-filter');
    if (filter) G.filterList(filter, document.querySelectorAll('#company-cards [data-search]'), (el) => el.dataset.search);
    const companies = await G.fetchJSON('/api/companies');
    if (document.getElementById('rank-chart')) initList(companies);
    if (document.getElementById('company-radar')) initDetail(companies);
  });

  function initList(companies) {
    const ranked = companies.slice().sort((a, b) => composite(b) - composite(a));
    new Chart(document.getElementById('rank-chart'), {
      type: 'bar',
      data: { labels: ranked.map((c) => c.name), datasets: [{ label: 'Composite score', data: ranked.map(composite), backgroundColor: G.accent(), barPercentage: .7 }] },
      options: {
        indexAxis: 'y',
        plugins: { legend: { display: false }, tooltip: { callbacks: { label: (ctx) => ' Composite ' + ctx.parsed.x + '/100 (sample)' } } },
        scales: { x: { min: 50, max: 100 }, y: { grid: { display: false } } },
        onClick: (e, els) => { if (els.length) location.href = '/companies/' + ranked[els[0].index].slug; },
      },
    });

    const bySlug = Object.fromEntries(companies.map((c) => [c.slug, c]));
    const boxes = Array.from(document.querySelectorAll('.compare-box'));
    const slots = ['nvidia', 'microsoft', 'samsung'].filter((s) => bySlug[s]);
    while (slots.length < 3) slots.push(null);
    let chart;

    const draw = () => {
      boxes.forEach((b) => {
        const s = slots.indexOf(b.value);
        b.checked = s >= 0;
        b.disabled = s < 0 && !slots.includes(null);
        b.closest('.card').style.borderColor = s >= 0 ? G.color(s) : '';
      });
      if (chart) chart.destroy();
      chart = new Chart(document.getElementById('compare-chart'), {
        type: 'radar',
        data: { labels: AXES.map((a) => a[1]),
          datasets: slots.map((slug, i) => slug && ds(bySlug[slug].name, AXES.map((a) => bySlug[slug][a[0]]), G.color(i))).filter(Boolean) },
        options: radarOpts(),
      });
      G.legend(document.getElementById('compare-legend'), chart);
    };
    boxes.forEach((b) => b.addEventListener('change', () => {
      const at = slots.indexOf(b.value);
      if (b.checked && at < 0) { const free = slots.indexOf(null); if (free >= 0) slots[free] = b.value; }
      if (!b.checked && at >= 0) slots[at] = null;
      draw();
    }));
    document.getElementById('compare-reset').addEventListener('click', () => { slots.fill(null); draw(); });
    draw();
  }

  function initDetail(companies) {
    const el = document.getElementById('company-radar');
    const c = companies.find((x) => x.slug === el.dataset.slug);
    const avg = AXES.map((a) => Math.round(companies.reduce((s, x) => s + x[a[0]], 0) / companies.length));
    const chart = new Chart(el, {
      type: 'radar',
      data: { labels: AXES.map((a) => a[1]), datasets: [ds(c.name, AXES.map((a) => c[a[0]]), G.color(0)), ds('Peer average', avg, G.color(1), true)] },
      options: radarOpts(),
    });
    G.legend(document.getElementById('company-radar-legend'), chart);
  }
})();
