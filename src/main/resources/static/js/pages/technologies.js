/* Technology Radar: SVG radar, filters, detail panel, scatter, sparklines. */
(function () {
  'use strict';
  const G = window.GTR;
  const NS = 'http://www.w3.org/2000/svg';
  const RING_ORDER = ['ADOPT', 'TRIAL', 'ASSESS', 'HOLD'];
  const RING_LABEL = { ADOPT: 'Adopt', TRIAL: 'Trial', ASSESS: 'Assess', HOLD: 'Hold' };
  // validated trio + neutral for Hold (ring is also encoded by position, so colour is secondary)
  const RING_COLOR = { ADOPT: '#199e70', TRIAL: '#3987e5', ASSESS: '#c98500', HOLD: '#6f84a5' };
  const RING_R = [[24, 118], [118, 190], [190, 250], [250, 296]];
  // quadrant -> start angle (degrees, SVG coords, 0 = east, clockwise)
  const QUADS = { AI_DATA: 270, PLATFORMS: 180, ENGINEERING: 90, FRONTIER: 0 };
  const QUAD_LABEL = { AI_DATA: 'AI & Data', PLATFORMS: 'Platforms & Infra', ENGINEERING: 'Engineering & Security', FRONTIER: 'Hardware & Frontier' };
  const C = 320;

  const state = { domain: window.GTR_SELECTED_DOMAIN || '', ring: '', q: '', selected: null };
  let techs = [];
  let order = {};

  document.addEventListener('DOMContentLoaded', async () => {
    injectRingDots();
    const [list, trend] = await Promise.all([G.fetchJSON('/api/technologies'), G.fetchJSON('/api/trends/technology')]);
    techs = list;
    list.forEach((t, i) => { order[t.slug] = i + 1; });
    drawRadar();
    drawScatter();
    drawSparks(trend);
    bindFilters();
    applyFilters();
  });

  function injectRingDots() {
    const s = document.createElement('style');
    s.textContent = RING_ORDER.map((r) => '.ring-dot-' + r + '{background:' + RING_COLOR[r] + ';border-radius:50%!important}').join('');
    document.head.appendChild(s);
  }

  function el(name, attrs, parent) {
    const e = document.createElementNS(NS, name);
    Object.entries(attrs || {}).forEach(([k, v]) => e.setAttribute(k, v));
    if (parent) parent.appendChild(e);
    return e;
  }

  function polar(angleDeg, r) {
    const a = angleDeg * Math.PI / 180;
    return [C + r * Math.cos(a), C + r * Math.sin(a)];
  }

  function drawRadar() {
    const svg = document.getElementById('radar');
    svg.innerHTML = '';
    const defs = el('defs', {}, svg);
    const grad = el('radialGradient', { id: 'rg' }, defs);
    el('stop', { offset: '0%', 'stop-color': 'rgba(34,211,238,.10)' }, grad);
    el('stop', { offset: '100%', 'stop-color': 'rgba(34,211,238,0)' }, grad);
    el('circle', { cx: C, cy: C, r: RING_R[3][1], fill: 'url(#rg)' }, svg);

    RING_R.forEach(([, outer], i) => {
      el('circle', { cx: C, cy: C, r: outer, class: 'ring-circle' }, svg);
      const t = el('text', { x: C + 4, y: C - outer + 14, class: 'ring-label' }, svg);
      t.textContent = RING_LABEL[RING_ORDER[i]];
    });
    el('line', { x1: C - 300, y1: C, x2: C + 300, y2: C, class: 'axis' }, svg);
    el('line', { x1: C, y1: C - 300, x2: C, y2: C + 300, class: 'axis' }, svg);

    const qpos = { AI_DATA: [630, 20, 'end'], PLATFORMS: [10, 20, 'start'], ENGINEERING: [10, 630, 'start'], FRONTIER: [630, 630, 'end'] };
    Object.entries(qpos).forEach(([q, [x, y, anchor]]) => {
      const t = el('text', { x, y, 'text-anchor': anchor, class: 'q-label' }, svg);
      t.textContent = QUAD_LABEL[q];
    });

    // group by quadrant+ring, spread evenly inside the segment
    const groups = {};
    techs.forEach((t) => { (groups[t.quadrant + '|' + t.ring] ||= []).push(t); });
    Object.entries(groups).forEach(([key, items]) => {
      const [q, ring] = key.split('|');
      const [r0, r1] = RING_R[RING_ORDER.indexOf(ring)];
      const start = QUADS[q] + 10;
      const span = 70;
      items.forEach((t, i) => {
        const angle = start + (span * (i + 0.5)) / items.length;
        const radius = r0 + (r1 - r0) * (items.length > 2 ? (i % 2 ? 0.68 : 0.34) : 0.5);
        const [x, y] = polar(angle, radius);
        const g = el('g', { 'data-slug': t.slug, tabindex: 0, role: 'button', 'aria-label': t.name + ', ' + RING_LABEL[ring] }, svg);
        const circle = el('circle', { cx: x, cy: y, r: 10, fill: RING_COLOR[ring], class: 'blip' }, g);
        const num = el('text', { x, y: y + 3.2, 'text-anchor': 'middle', class: 'blip-num' }, g);
        num.textContent = order[t.slug];
        const title = el('title', {}, g);
        title.textContent = t.name + ' — ' + RING_LABEL[ring] + ' · momentum ' + t.momentum + ' (sample)';
        const select = () => selectTech(t.slug);
        g.addEventListener('click', select);
        g.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); select(); } });
        g.style.cursor = 'pointer';
        circle.dataset.slug = t.slug;
      });
    });
  }

  function selectTech(slug) {
    state.selected = slug;
    document.querySelectorAll('#radar .blip').forEach((b) => b.classList.toggle('selected', b.dataset.slug === slug));
    document.querySelectorAll('#tech-table tbody tr').forEach((r) => { r.style.background = r.dataset.slug === slug ? 'rgba(34,211,238,.07)' : ''; });
    const t = techs.find((x) => x.slug === slug);
    if (!t) return;
    const e = G.escape;
    const p = t.provenance || {};
    document.getElementById('tech-detail').innerHTML =
      '<div class="row between"><div><div class="eyebrow" style="margin:0">' + e(t.domain) + ' · ' + e(QUAD_LABEL[t.quadrant]) + '</div>' +
      '<h2 style="font-size:19px">' + e(t.name) + '</h2></div>' +
      '<span class="badge ring-' + t.ring + '">' + RING_LABEL[t.ring] + '</span></div>' +
      '<p class="text-2" style="margin-top:10px">' + e(t.description) + '</p>' +
      '<div class="score-grid"><div class="score"><span>Maturity</span><b>' + t.maturity + '</b></div>' +
      '<div class="score"><span>Momentum</span><b>' + t.momentum + '</b></div>' +
      '<div class="score"><span>Demand</span><b>' + t.marketDemand + '</b></div></div>' +
      '<div class="row" style="margin-bottom:10px"><span class="muted">Growth YoY</span><span class="delta ' + (t.growthRate >= 0 ? 'up' : 'down') + '">' +
      (t.growthRate >= 0 ? '+' : '') + G.fmt(t.growthRate) + '%</span>' +
      '<span class="muted" style="margin-left:12px">Leaders</span>' + (t.leadingCountryList || []).map((c) => '<span class="tag">' + e(c) + '</span>').join('') + '</div>' +
      '<h3 style="margin-bottom:6px">Key signals</h3><ul class="text-2" style="margin:0 0 10px;padding-left:18px;font-size:13px">' +
      (t.keySignalList || []).map((s) => '<li>' + e(s) + '</li>').join('') + '</ul>' +
      '<div class="chips" style="margin-bottom:12px">' + (t.relatedSkillList || []).map((s) => '<span class="tag">' + e(s) + '</span>').join('') + '</div>' +
      '<div class="row between"><span class="badge sample">Sample data · ' + e(p.publishedOn || '') + '</span>' +
      '<a class="btn sm" href="/technologies/' + e(t.slug) + '">Full profile →</a></div>';
  }

  function drawScatter() {
    new Chart(document.getElementById('scatter'), {
      type: 'scatter',
      data: {
        datasets: RING_ORDER.map((ring) => ({
          label: RING_LABEL[ring],
          data: techs.filter((t) => t.ring === ring).map((t) => ({ x: t.maturity, y: t.momentum, slug: t.slug, name: t.name })),
          backgroundColor: RING_COLOR[ring], borderColor: '#101b30', borderWidth: 2, pointRadius: 6, pointHoverRadius: 8,
        })),
      },
      options: {
        plugins: {
          legend: { display: true, position: 'bottom', labels: { usePointStyle: true, boxWidth: 8, color: '#a3b3cc' } },
          tooltip: { callbacks: { label: (ctx) => ' ' + ctx.raw.name + ' — maturity ' + ctx.raw.x + ', momentum ' + ctx.raw.y } },
        },
        scales: {
          x: { min: 0, max: 100, title: { display: true, text: 'Maturity' } },
          y: { min: 0, max: 100, title: { display: true, text: 'Momentum' } },
        },
        onClick: (evt, els, chart) => {
          if (!els.length) return;
          const raw = chart.data.datasets[els[0].datasetIndex].data[els[0].index];
          selectTech(raw.slug);
        },
      },
    });
  }

  function drawSparks(trend) {
    document.querySelectorAll('canvas[data-spark]').forEach((c) => {
      const s = trend.series.find((x) => x.key === c.dataset.spark);
      if (s) G.sparkline(c, s.values, G.accent());
    });
  }

  function bindFilters() {
    document.querySelectorAll('#domain-cards .domain-card').forEach((card) => card.addEventListener('click', () => {
      state.domain = state.domain === card.dataset.domain ? '' : card.dataset.domain;
      const url = new URL(location.href);
      if (state.domain) url.searchParams.set('domain', state.domain); else url.searchParams.delete('domain');
      history.replaceState(null, '', url);
      applyFilters();
    }));
    document.getElementById('clear-domain').addEventListener('click', () => { state.domain = ''; history.replaceState(null, '', location.pathname); applyFilters(); });
    document.querySelectorAll('[data-ring]').forEach((chip) => {
      if (chip.tagName !== 'BUTTON') return;
      chip.addEventListener('click', () => {
        state.ring = chip.dataset.ring;
        document.querySelectorAll('button[data-ring]').forEach((c) => c.classList.toggle('active', c === chip));
        applyFilters();
      });
    });
    document.getElementById('radar-search').addEventListener('input', (e) => { state.q = e.target.value.trim().toLowerCase(); applyFilters(); });
  }

  function matches(t) {
    return (!state.domain || t.domain === state.domain) &&
      (!state.ring || t.ring === state.ring) &&
      (!state.q || (t.name + ' ' + t.description + ' ' + t.domain).toLowerCase().includes(state.q));
  }

  function applyFilters() {
    let n = 0;
    const visible = new Set();
    techs.forEach((t) => { if (matches(t)) { visible.add(t.slug); n++; } });
    document.querySelectorAll('#radar g[data-slug]').forEach((g) => {
      const on = visible.has(g.dataset.slug);
      g.querySelector('.blip').classList.toggle('dim', !on);
      g.querySelector('.blip-num').classList.toggle('dim', !on);
    });
    document.querySelectorAll('#tech-table tbody tr').forEach((r) => r.classList.toggle('hidden', !visible.has(r.dataset.slug)));
    document.querySelectorAll('#domain-cards .domain-card').forEach((c) => c.classList.toggle('active', c.dataset.domain === state.domain));
    const parts = [];
    if (state.domain) parts.push(state.domain);
    if (state.ring) parts.push(RING_LABEL[state.ring]);
    if (state.q) parts.push('"' + state.q + '"');
    document.getElementById('radar-count').textContent = 'Showing ' + n + ' of ' + techs.length + ' technologies' + (parts.length ? ' · ' + parts.join(' · ') : '');
    document.getElementById('clear-domain').classList.toggle('hidden', !state.domain);
    if (state.domain && !state.selected) {
      const first = techs.find((t) => visible.has(t.slug));
      if (first) selectTech(first.slug);
    }
  }
})();
