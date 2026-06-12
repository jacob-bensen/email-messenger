(function () {
  'use strict';
  document.addEventListener('click', function (event) {
    var btn = event.target.closest('[data-copy-target]');
    if (!btn) return;
    var targetId = btn.getAttribute('data-copy-target');
    var input = document.getElementById(targetId);
    if (!input) return;
    var defaultLabel = btn.getAttribute('data-copy-default') || btn.textContent;
    var successLabel = btn.getAttribute('data-copy-success') || 'Copied!';
    var done = function () {
      btn.textContent = successLabel;
      btn.classList.add('is-copied');
      setTimeout(function () {
        btn.textContent = defaultLabel;
        btn.classList.remove('is-copied');
      }, 1800);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(input.value).then(done).catch(function () {
        input.select();
        document.execCommand && document.execCommand('copy');
        done();
      });
    } else {
      input.select();
      document.execCommand && document.execCommand('copy');
      done();
    }
  });
})();
