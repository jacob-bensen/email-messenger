(function () {
  var STORAGE_KEY = 'mailim.cookieConsent.v1';

  function ready(fn) {
    if (document.readyState !== 'loading') {
      fn();
    } else {
      document.addEventListener('DOMContentLoaded', fn);
    }
  }

  ready(function () {
    var banner = document.getElementById('cookie-banner');
    var acceptBtn = document.getElementById('cookie-banner-accept');
    if (!banner || !acceptBtn) {
      return;
    }

    var alreadyAccepted = false;
    try {
      alreadyAccepted = localStorage.getItem(STORAGE_KEY) === 'accepted';
    } catch (e) {
      // Private mode / disabled storage — show the banner each session anyway,
      // and silently accept if storage write also fails.
    }

    if (alreadyAccepted) {
      banner.parentNode && banner.parentNode.removeChild(banner);
      return;
    }

    banner.hidden = false;

    acceptBtn.addEventListener('click', function () {
      try {
        localStorage.setItem(STORAGE_KEY, 'accepted');
      } catch (e) {
        // ignore — banner still dismisses for this session
      }
      banner.hidden = true;
      if (banner.parentNode) {
        banner.parentNode.removeChild(banner);
      }
    });
  });
})();
