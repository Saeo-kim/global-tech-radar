/* Dashboard page */
(function () {
  'use strict';
  const { fetchJSON, escape, fmt } = window.GTR;

  document.addEventListener('DOMContentLoaded', async () => {
    try {
      const [countries, techTrend, aiTrend] = await Promise.all([
        fetchJSON('/api/countries'),
        fetchJSON('/api/trends/technology'),
        fetchJSON('/api/trends/ai_industry'),
      ]);
      initMap(countries);
      initGrowth(techTrend);
      initAi(aiTrend);
      initDevelopers(countries);
    } catch (e) {
      console.error(e);
      document.getElementById('country-panel').innerHTML = '<div class="muted">Could not load data: ' + escape(e.message) + '</div>';
    }
  });

  // ------------------------------------------------------------------ map + panel
  function initMap(countries) {
    const panel = document.getElementById('country-panel');
    const metrics = [
      ['techIndex', 'Technology index'], ['aiReadiness', 'AI readiness'], ['developerTalent', 'Developer talent'],
      ['rndIntensity', 'R&D intensity'], ['startupEcosystem', 'Startup ecosystem'], ['digitalInfrastructure', 'Digital infrastructure'],
      ['remoteFriendliness', 'Remote-work friendliness'],
    ];
    const rankOf = (c) => countries.slice().sort((a, b) => b.techIndex - a.techIndex).findIndex((x) => x.code === c.code) + 1;

    const show = (c) => {
      const p = c.provenance || {};
      panel.innerHTML =
        '<div class="country-title"><span class="flag">' + escape(c.flag) + '</span><div>' +
        '<h2>' + escape(c.name) + '</h2><div class="muted" style="font-size:12px">' + escape(c.region) +
        ' · rank #' + rankOf(c) + ' of ' + countries.length + '</div></div>' +
        '<span class="badge accent" style="margin-left:auto">Index ' + c.techIndex + '</span></div>' +
        '<p class="text-2" style="font-size:13px">' + escape(c.summary) + '</p>' +
        '<div class="metric-list">' + metrics.map(([k, label]) =>
          '<div class="metric"><span>' + label + '</span><b class="num">' + c[k] + '</b>' +
          '<div class="bar"><span style="width:' + c[k] + '%"></span></div></div>').join('') + '</div>' +
        '<div class="chips">' + (c.strengthList || []).map((s) => '<span class="tag">' + escape(s) + '</span>').join('') + '</div>' +
        '<div class="row between"><span class="badge sample">Sample data · ' + escape(p.publishedOn || '') + '</span>' +
        '<a class="btn sm" href="/countries/' + c.code + '">Full profile →</a></div>';
    };

    const map = window.WorldMap.create({ el: '#world-map', countries, selected: countries[0]?.code, onSelect: show });
    if (!map) show(countries[0]);

    document.querySelectorAll('#map-metric .chip').forEach((chip) => {
      chip.addEventListener('click', () => {
        document.querySelectorAll('#map-metric .chip').forEach((c) => c.classList.remove('active'));
        chip.classList.add('active');
        map && map.setMetric(chip.dataset.metric);
      });
    });
    window.GTR.provenanceLine(document.getElementById('map-provenance'), {
      dataStatus: countries[0]?.provenance?.dataStatus, sourceName: countries[0]?.provenance?.sourceName,
      publishedOn: countries[0]?.provenance?.publishedOn,
    });
  }

  // ------------------------------------------------------------------ growth chart (max 4 series)
  function initGrowth(trend) {
    const MAX = 4;
    const picker = document.getElementById('domain-picker');
    const chips = Array.from(picker.querySelectorAll('.chip'));
    // slot -> domain. A domain keeps its colour while selected; removing one never repaints the others.
    const slots = new Array(MAX).fill(null);
    ['AI', 'AI Agents', 'Cloud', 'Semiconductor'].forEach((d, i) => { slots[i] = d; });

    const canvas = document.getElementById('growth-chart');
    let chart = null;

    const draw = () => {
      const keys = slots.filter(Boolean);
      chips.forEach((c) => {
        const slot = slots.indexOf(c.dataset.domain);
        c.classList.toggle('active', slot >= 0);
        c.style.background = slot >= 0 ? window.GTR.color(slot) : '';
        c.style.borderColor = slot >= 0 ? window.GTR.color(slot) : '';
        c.style.color = slot >= 0 ? '#fff' : '';
      });
      if (chart) chart.destroy();
      chart = window.GTR.lineChart(canvas, trend, { keys, colorOf: (k) => window.GTR.color(slots.indexOf(k)), max: 100, yTitle: 'Activity index' });
      window.GTR.legend(document.getElementById('growth-legend'), chart);
    };

    chips.forEach((chip) => chip.addEventListener('click', () => {
      const d = chip.dataset.domain;
      const at = slots.indexOf(d);
      if (at >= 0) {
        if (slots.filter(Boolean).length === 1) return; // keep at least one
        slots[at] = null;
      } else {
        const free = slots.indexOf(null);
        if (free < 0) { chip.animate([{ transform: 'translateX(-3px)' }, { transform: 'translateX(3px)' }, { transform: 'none' }], 200); return; }
        slots[free] = d;
      }
      draw();
    }));
    draw();
    window.GTR.provenanceLine(document.getElementById('growth-provenance'), trend);
  }

  function initAi(trend) {
    const chart = window.GTR.lineChart(document.getElementById('ai-chart'), trend, { max: 100 });
    window.GTR.legend(document.getElementById('ai-legend'), chart);
    window.GTR.provenanceLine(document.getElementById('ai-provenance'), trend);
  }

  function initDevelopers(countries) {
    const sorted = countries.slice().sort((a, b) => b.developersThousands - a.developersThousands);
    new Chart(document.getElementById('dev-chart'), {
      type: 'bar',
      data: {
        labels: sorted.map((c) => c.flag + ' ' + c.code),
        datasets: [{ label: 'Developers (thousands)', data: sorted.map((c) => c.developersThousands), backgroundColor: window.GTR.accent(), barPercentage: .7, categoryPercentage: .85 }],
      },
      options: {
        indexAxis: 'y',
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { title: (i) => sorted[i[0].dataIndex].name, label: (ctx) => ' ' + fmt(ctx.parsed.x) + 'k developers (sample)' } },
        },
        scales: { x: { beginAtZero: true, ticks: { callback: (v) => fmt(v / 1000, 0) + 'M' } }, y: { grid: { display: false } } },
      },
    });
    const p = countries[0]?.provenance || {};
    window.GTR.provenanceLine(document.getElementById('dev-provenance'), p);
  }
})();
