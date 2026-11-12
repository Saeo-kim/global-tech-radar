/* Global Tech Updates: auto-submit filter selects. Everything else is server-rendered. */
(function () {
  'use strict';
  document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('news-filters');
    if (!form) return;
    form.querySelectorAll('select[data-autosubmit]').forEach((s) => s.addEventListener('change', () => form.submit()));
  });
})();
