/* Reusable interactive world map (jsVectorMap). Shading = sequential blue ramp by metric tier. */
(function () {
  'use strict';

  const TIERS = [
    { key: 't4', min: 90, color: '#9ec5f4', label: '90+' },
    { key: 't3', min: 80, color: '#5598e7', label: '80–89' },
    { key: 't2', min: 70, color: '#2a78d6', label: '70–79' },
    { key: 't1', min: 0, color: '#1c5cab', label: '<70' },
  ];
  const METRIC_LABELS = {
    techIndex: 'Tech index', aiReadiness: 'AI readiness', developerTalent: 'Developer talent',
    rndIntensity: 'R&D intensity', startupEcosystem: 'Startup ecosystem', digitalInfrastructure: 'Digital infrastructure',
  };

  const tierOf = (v) => TIERS.find((t) => v >= t.min);

  function create({ el, countries, metric = 'techIndex', selected, onSelect }) {
    const container = typeof el === 'string' ? document.querySelector(el) : el;
    if (!container) return null;
    if (typeof window.jsVectorMap === 'undefined') {
      const fb = document.getElementById('map-fallback');
      if (fb) { fb.style.display = 'block'; container.parentElement.style.display = 'none'; }
      return null;
    }
    const byCode = Object.fromEntries(countries.map((c) => [c.code, c]));
    let map = null;
    let current = metric;

    const render = () => {
      container.innerHTML = '';
      const values = {};
      countries.forEach((c) => { values[c.code] = tierOf(c[current]).key; });
      const scale = Object.fromEntries(TIERS.map((t) => [t.key, t.color]));
      map = new jsVectorMap({
        selector: container,
        map: 'world',
        backgroundColor: 'transparent',
        zoomButtons: true,
        zoomOnScroll: false,
        regionsSelectable: false,
        regionStyle: {
          initial: { fill: '#1a2b48', stroke: '#0a1426', strokeWidth: 0.4, fillOpacity: 1 },
          hover: { fillOpacity: 0.85, cursor: 'pointer' },
        },
        series: { regions: [{ attribute: 'fill', scale, values }] },
        markers: countries.map((c) => ({ name: c.name, coords: [c.latitude, c.longitude], code: c.code })),
        markerStyle: {
          initial: { fill: '#22d3ee', stroke: '#04121f', strokeWidth: 2, r: 6 },
          hover: { fill: '#ffffff', cursor: 'pointer' },
        },
        onRegionTooltipShow(event, tooltip, code) {
          const c = byCode[code];
          if (!c) { tooltip.text(tooltip.text() + ' · not tracked'); return; }
          tooltip.text(tooltipHtml(c, current), true);
        },
        onMarkerTooltipShow(event, tooltip, index) {
          tooltip.text(tooltipHtml(countries[index], current), true);
        },
        onRegionClick(event, code) { if (byCode[code] && onSelect) onSelect(byCode[code]); },
        onMarkerClick(event, index) { if (onSelect) onSelect(countries[index]); },
      });
    };

    render();
    if (selected && onSelect) onSelect(byCode[selected] || countries[0]);
    window.addEventListener('resize', debounce(() => map && map.updateSize && map.updateSize(), 200));
    return {
      setMetric(m) { current = m; render(); },
      label: (m) => METRIC_LABELS[m] || m,
    };
  }

  function tooltipHtml(c, metric) {
    const esc = window.GTR.escape;
    return '<div style="font-weight:700;margin-bottom:2px">' + esc(c.flag) + ' ' + esc(c.name) + '</div>' +
      '<div>' + esc(METRIC_LABELS[metric] || metric) + ': <b>' + c[metric] + '</b>/100</div>' +
      '<div style="color:#fbbf24;font-size:11px">Sample data</div>';
  }

  function debounce(fn, ms) {
    let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
  }

  window.WorldMap = { create, TIERS, METRIC_LABELS };
})();
