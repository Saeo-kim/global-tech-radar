/* Country Intelligence: map, comparison radar (max 3), trend, card filter, and detail-page charts. */
(function () {
  'use strict';
  const G = window.GTR;
  const AXES = [
    ['aiReadiness', 'AI'], ['developerTalent', 'Talent'], ['rndIntensity', 'R&D'], ['startupEcosystem', 'Startups'],
    ['digitalInfrastructure', 'Infra'], ['remoteFriendliness', 'Remote'],
  ];

  function radarOptions() {
    return {
      plugins: { legend: { display: false } },
      scales: {
        r: {
          min: 0, max: 100, ticks: { stepSize: 25, backdropColor: 'transparent', color: '#6f84a5', font: { size: 10 } },
          grid: { color: 'rgba(42,64,105,.6)' }, angleLines: { color: 'rgba(42,64,105,.6)' },
          pointLabels: { color: '#a3b3cc', font: { size: 11.5, weight: '600' } },
        },
      },
    };
  }

  function radarDataset(label, values, color, dashed) {
    return {
      label, data: values, borderColor: color, backgroundColor: color + '26', borderWidth: 2,
      pointRadius: 3, pointBackgroundColor: color, borderDash: dashed ? [5, 4] : [],
    };
  }

  document.addEventListener('DOMContentLoaded', async () => {
    const filter = document.getElementById('country-filter');
    if (filter) G.filterList(filter, document.querySelectorAll('#country-cards [data-search]'), (el) => el.dataset.search);

    if (document.getElementById('compare-chart')) await initList();
    if (document.getElementById('country-radar')) await initDetail();
  });

  async function initList() {
    const [countries, trend] = await Promise.all([G.fetchJSON('/api/countries'), G.fetchJSON('/api/trends/country')]);
    const byCode = Object.fromEntries(countries.map((c) => [c.code, c]));
    const MAX = 3;
    const slots = [countries[0]?.code, countries[1]?.code, null];
    const chips = Array.from(document.querySelectorAll('#compare-picker .chip'));
    let radar, line;

    const toggle = (code) => {
      const at = slots.indexOf(code);
      if (at >= 0) { if (slots.filter(Boolean).length > 1) slots[at] = null; }
      else { const free = slots.indexOf(null); if (free >= 0) slots[free] = code; }
      draw();
    };

    const draw = () => {
      chips.forEach((c) => {
        const s = slots.indexOf(c.dataset.code);
        c.classList.toggle('active', s >= 0);
        c.style.background = s >= 0 ? G.color(s) : '';
        c.style.borderColor = s >= 0 ? G.color(s) : '';
        c.style.color = s >= 0 ? '#fff' : '';
      });
      const chosen = slots.map((code, i) => ({ code, i })).filter((x) => x.code);
      if (radar) radar.destroy();
      radar = new Chart(document.getElementById('compare-chart'), {
        type: 'radar',
        data: {
          labels: AXES.map((a) => a[1]),
          datasets: chosen.map(({ code, i }) => radarDataset(byCode[code].name, AXES.map((a) => byCode[code][a[0]]), G.color(i))),
        },
        options: radarOptions(),
      });
      G.legend(document.getElementById('compare-legend'), radar);

      if (line) line.destroy();
      line = G.lineChart(document.getElementById('trend-chart'), trend, {
        keys: chosen.map((x) => x.code), colorOf: (k) => G.color(slots.indexOf(k)),
      });
      line.data.datasets.forEach((ds) => { ds.label = byCode[ds.label]?.name || ds.label; });
      line.options.scales.y.beginAtZero = false;
      line.update();
      G.legend(document.getElementById('trend-legend'), line);
    };

    chips.forEach((c) => c.addEventListener('click', () => toggle(c.dataset.code)));
    draw();
    G.provenanceLine(document.getElementById('trend-provenance'), trend);

    const map = window.WorldMap.create({ el: '#world-map', countries, onSelect: (c) => toggle(c.code) });
    document.querySelectorAll('#map-metric .chip').forEach((chip) => chip.addEventListener('click', () => {
      document.querySelectorAll('#map-metric .chip').forEach((c) => c.classList.remove('active'));
      chip.classList.add('active');
      map && map.setMetric(chip.dataset.metric);
    }));
  }

  async function initDetail() {
    const el = document.getElementById('country-radar');
    const code = el.dataset.code;
    const [countries, trend] = await Promise.all([G.fetchJSON('/api/countries'), G.fetchJSON('/api/trends/country?keys=' + code)]);
    const c = countries.find((x) => x.code === code);
    const avg = AXES.map((a) => Math.round(countries.reduce((s, x) => s + x[a[0]], 0) / countries.length));
    const radar = new Chart(el, {
      type: 'radar',
      data: {
        labels: AXES.map((a) => a[1]),
        datasets: [radarDataset(c.name, AXES.map((a) => c[a[0]]), G.color(0)), radarDataset('Average of tracked countries', avg, G.color(1), true)],
      },
      options: radarOptions(),
    });
    G.legend(document.getElementById('country-radar-legend'), radar);

    const line = G.lineChart(document.getElementById('country-trend'), trend, { fill: true, colorOf: () => G.accent() });
    line.data.datasets[0].label = c.name + ' tech index';
    line.options.scales.y.beginAtZero = false;
    line.update();
    G.provenanceLine(document.getElementById('country-trend-provenance'), trend);
  }
})();
