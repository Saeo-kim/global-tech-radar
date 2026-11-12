/* GLOBAL TECH RADAR — shared front-end helpers */
(function () {
  'use strict';

  // Categorical palette validated for the dark navy surface (fixed order, never cycled).
  const PALETTE = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#008300', '#9085e9', '#e66767'];
  const css = getComputedStyle(document.documentElement);
  const token = (n) => css.getPropertyValue(n).trim();

  const GTR = {
    palette: PALETTE,
    color: (i) => PALETTE[i % PALETTE.length],
    accent: () => token('--accent') || '#22d3ee',

    async fetchJSON(url) {
      const res = await fetch(url, { headers: { Accept: 'application/json' } });
      if (!res.ok) throw new Error(url + ' -> HTTP ' + res.status);
      return res.json();
    },

    fmt(n, digits = 0) {
      if (n === null || n === undefined || isNaN(n)) return '–';
      return Number(n).toLocaleString('en-US', { maximumFractionDigits: digits, minimumFractionDigits: digits });
    },

    money(n) {
      if (n >= 1000) return '$' + GTR.fmt(n / 1000, 0) + 'k';
      return '$' + GTR.fmt(n, 0);
    },

    escape(s) {
      return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    },

    /** Small "Sample data · Source · date" line for chart cards that load data from the API. */
    provenanceLine(el, payload) {
      if (!el || !payload) return;
      const sample = (payload.dataStatus || 'SAMPLE') === 'SAMPLE';
      el.innerHTML =
        '<span class="badge ' + (sample ? 'sample' : 'live') + '">' + (sample ? 'Sample data' : 'Live data') + '</span>' +
        (payload.sourceName ? '<span>Source: <b>' + GTR.escape(payload.sourceName) + '</b></span>' : '') +
        (payload.publishedOn ? '<span>Published: <b class="mono">' + GTR.escape(payload.publishedOn) + '</b></span>' : '');
    },

    /**
     * Toggleable legend that also controls dataset visibility. Color follows the entity:
     * hiding a series never repaints the others.
     */
    legend(container, chart) {
      if (!container) return;
      container.innerHTML = '';
      chart.data.datasets.forEach((ds, i) => {
        const b = document.createElement('button');
        b.type = 'button';
        b.innerHTML = '<i style="background:' + ds.borderColor + '"></i>' + GTR.escape(ds.label);
        b.setAttribute('aria-pressed', 'true');
        b.addEventListener('click', () => {
          const visible = chart.isDatasetVisible(i);
          chart.setDatasetVisibility(i, !visible);
          b.classList.toggle('off', visible);
          b.setAttribute('aria-pressed', String(!visible));
          chart.update();
        });
        container.appendChild(b);
      });
    },

    lineChart(canvas, trend, opts = {}) {
      const keys = opts.keys || trend.series.map((s) => s.key);
      const colorOf = opts.colorOf || ((key, i) => GTR.color(i));
      const datasets = trend.series
        .filter((s) => keys.includes(s.key))
        .map((s) => {
          const c = colorOf(s.key, keys.indexOf(s.key));
          return {
            label: s.key, data: s.values, borderColor: c, backgroundColor: c + '22',
            borderWidth: 2, pointRadius: 0, pointHoverRadius: 5, pointHoverBorderWidth: 2,
            pointHoverBorderColor: token('--surface'), tension: .35, fill: !!opts.fill,
          };
        });
      return new Chart(canvas, {
        type: 'line',
        data: { labels: trend.periods, datasets },
        options: {
          interaction: { mode: 'index', intersect: false },
          plugins: {
            legend: { display: false },
            tooltip: { callbacks: { label: (ctx) => ' ' + ctx.dataset.label + ': ' + GTR.fmt(ctx.parsed.y, 1) + (trend.unit === '%' ? '%' : '') } },
          },
          scales: {
            y: { beginAtZero: true, suggestedMax: opts.max, title: { display: !!opts.yTitle, text: opts.yTitle } },
            x: { grid: { display: false } },
          },
        },
      });
    },

    sparkline(canvas, values, color) {
      return new Chart(canvas, {
        type: 'line',
        data: { labels: values.map((_, i) => i), datasets: [{ data: values, borderColor: color, borderWidth: 2, pointRadius: 0, tension: .4, fill: true, backgroundColor: color + '1f' }] },
        options: {
          animation: false, responsive: true, maintainAspectRatio: false, events: [],
          plugins: { legend: { display: false }, tooltip: { enabled: false } },
          scales: { x: { display: false }, y: { display: false, beginAtZero: false } },
        },
      });
    },

    sortableTables() {
      document.querySelectorAll('table[data-sortable]').forEach((table) => {
        table.querySelectorAll('th[data-sort]').forEach((th, idx) => {
          th.classList.add('sortable');
          th.addEventListener('click', () => {
            const col = Array.from(th.parentNode.children).indexOf(th);
            const type = th.dataset.sort;
            const asc = !th.classList.contains('sorted-asc');
            table.querySelectorAll('th').forEach((h) => h.classList.remove('sorted-asc', 'sorted-desc'));
            th.classList.add(asc ? 'sorted-asc' : 'sorted-desc');
            const tbody = table.tBodies[0];
            const rows = Array.from(tbody.rows);
            rows.sort((a, b) => {
              const va = a.cells[col].dataset.value ?? a.cells[col].textContent.trim();
              const vb = b.cells[col].dataset.value ?? b.cells[col].textContent.trim();
              const r = type === 'num' ? parseFloat(va) - parseFloat(vb) : va.localeCompare(vb);
              return asc ? r : -r;
            });
            rows.forEach((r) => tbody.appendChild(r));
          });
        });
      });
    },

    filterList(input, items, textOf) {
      const run = () => {
        const q = input.value.trim().toLowerCase();
        let shown = 0;
        items.forEach((el) => {
          const hit = !q || textOf(el).toLowerCase().includes(q);
          el.classList.toggle('hidden', !hit);
          if (hit) shown++;
        });
        return shown;
      };
      input.addEventListener('input', run);
      return run;
    },
  };

  // Chart.js defaults tuned for the dark theme: recessive grid, readable ticks.
  if (window.Chart) {
    Chart.defaults.color = token('--text-2') || '#a3b3cc';
    Chart.defaults.font.family = (token('--font') || 'Inter, system-ui, sans-serif') + ', "Noto Color Emoji"';
    Chart.defaults.font.size = 11.5;
    Chart.defaults.borderColor = 'rgba(30, 48, 80, .55)';
    Chart.defaults.maintainAspectRatio = false;
    Chart.defaults.plugins.tooltip.backgroundColor = '#0b1629';
    Chart.defaults.plugins.tooltip.borderColor = '#2a4069';
    Chart.defaults.plugins.tooltip.borderWidth = 1;
    Chart.defaults.plugins.tooltip.titleColor = '#e6edf7';
    Chart.defaults.plugins.tooltip.bodyColor = '#a3b3cc';
    Chart.defaults.plugins.tooltip.padding = 10;
    Chart.defaults.plugins.tooltip.cornerRadius = 8;
    Chart.defaults.plugins.tooltip.boxPadding = 4;
    Chart.defaults.elements.bar.borderRadius = 4;
    Chart.defaults.elements.bar.borderSkipped = 'start';
  }

  window.GTR = GTR;

  document.addEventListener('DOMContentLoaded', () => {
    // mobile sidebar
    const sidebar = document.getElementById('sidebar');
    const scrim = document.getElementById('scrim');
    const menu = document.getElementById('menu-btn');
    const close = () => { sidebar?.classList.remove('open'); scrim?.classList.remove('show'); };
    menu?.addEventListener('click', () => { sidebar.classList.add('open'); scrim.classList.add('show'); });
    scrim?.addEventListener('click', close);

    // UTC clock
    const clock = document.getElementById('utc-clock');
    const tick = () => { if (clock) clock.textContent = 'UTC ' + new Date().toISOString().slice(11, 16); };
    tick(); setInterval(tick, 30000);

    // "/" focuses global search
    const search = document.getElementById('global-search');
    document.addEventListener('keydown', (e) => {
      if (e.key === '/' && document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') {
        e.preventDefault(); search?.focus();
      }
    });

    // toasts
    document.querySelectorAll('[data-autohide]').forEach((t) => {
      setTimeout(() => t.classList.add('hide'), 3500);
      setTimeout(() => t.remove(), 4000);
    });

    // tabs: <div data-tabs> <button class="tab" data-tab="x"> ... <div class="tab-panel" data-panel="x">
    document.querySelectorAll('[data-tabs]').forEach((group) => {
      const tabs = group.querySelectorAll('.tab');
      const activate = (name, push) => {
        tabs.forEach((t) => t.classList.toggle('active', t.dataset.tab === name));
        group.querySelectorAll('.tab-panel').forEach((p) => p.classList.toggle('active', p.dataset.panel === name));
        if (push) history.replaceState(null, '', '#' + name);
        document.dispatchEvent(new CustomEvent('gtr:tab', { detail: name }));
      };
      tabs.forEach((t) => t.addEventListener('click', () => activate(t.dataset.tab, true)));
      const hash = location.hash.slice(1);
      if (hash && group.querySelector('[data-tab="' + hash + '"]')) activate(hash, false);
    });

    // confirm before destructive form posts
    document.querySelectorAll('form[data-confirm]').forEach((f) => {
      f.addEventListener('submit', (e) => { if (!confirm(f.dataset.confirm)) e.preventDefault(); });
    });

    // clickable rows
    document.querySelectorAll('tr[data-href]').forEach((tr) => {
      tr.classList.add('clickable');
      tr.addEventListener('click', (e) => { if (!e.target.closest('a,button,input')) location.href = tr.dataset.href; });
    });

    GTR.sortableTables();
  });
})();
