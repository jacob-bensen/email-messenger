// Wires the /account/notifications "Browser & mobile push" toggle to the
// PushManager + the /account/notifications/push endpoints. When VAPID
// keys aren't configured, or the browser doesn't ship the APIs, the row
// stays in read-only "coming soon" mode instead of showing a switch
// that goes nowhere.
(function () {
  'use strict';

  const section = document.getElementById('push-section');
  if (!section) return;

  const badge = section.querySelector('[data-push-badge]');
  const desc = section.querySelector('[data-push-desc]');
  const toggleLabel = section.querySelector('[data-push-toggle-label]');
  const toggle = section.querySelector('[data-push-toggle]');
  const placeholder = section.querySelector('[data-push-switch-placeholder]');

  function showComingSoon(reason) {
    section.dataset.pushState = 'unavailable';
    if (badge) badge.hidden = false;
    if (desc && reason) desc.textContent = reason;
    if (toggleLabel) toggleLabel.hidden = true;
    if (placeholder) placeholder.hidden = false;
  }

  function showToggle(enabled) {
    section.dataset.pushState = enabled ? 'on' : 'off';
    if (badge) badge.hidden = true;
    if (toggleLabel) toggleLabel.hidden = false;
    if (placeholder) placeholder.hidden = true;
    if (toggle) toggle.checked = !!enabled;
  }

  function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    const headers = { 'Content-Type': 'application/json' };
    if (token && header && token.content && header.content) {
      headers[header.content] = token.content;
    }
    return headers;
  }

  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const raw = atob(base64);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
    return bytes;
  }

  function arrayBufferToBase64Url(buffer) {
    if (!buffer) return '';
    const bytes = new Uint8Array(buffer);
    let bin = '';
    for (let i = 0; i < bytes.byteLength; i++) bin += String.fromCharCode(bytes[i]);
    return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  function subscriptionKeys(sub) {
    const raw = sub.toJSON ? sub.toJSON() : null;
    if (raw && raw.keys) return { p256dh: raw.keys.p256dh, auth: raw.keys.auth };
    return {
      p256dh: arrayBufferToBase64Url(sub.getKey && sub.getKey('p256dh')),
      auth: arrayBufferToBase64Url(sub.getKey && sub.getKey('auth'))
    };
  }

  async function fetchVapidKey() {
    const res = await fetch('/account/notifications/push/vapid-public-key', {
      credentials: 'same-origin'
    });
    if (res.status === 404) return null;
    if (!res.ok) throw new Error('vapid-fetch-failed');
    const body = await res.json();
    return body.publicKey || null;
  }

  async function ensureRegistration() {
    if (!('serviceWorker' in navigator)) return null;
    const reg = await navigator.serviceWorker.getRegistration();
    if (reg) return reg;
    try {
      return await navigator.serviceWorker.register('/sw.js');
    } catch (_) {
      return null;
    }
  }

  async function currentSubscription() {
    const reg = await ensureRegistration();
    if (!reg || !reg.pushManager) return null;
    return reg.pushManager.getSubscription();
  }

  async function subscribe(publicKey) {
    if (Notification.permission === 'denied') {
      showToggle(false);
      alert('Notifications are blocked in your browser settings. Enable them there and try again.');
      return false;
    }
    if (Notification.permission !== 'granted') {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        showToggle(false);
        return false;
      }
    }
    const reg = await ensureRegistration();
    if (!reg || !reg.pushManager) {
      showComingSoon('Real-time alerts aren’t supported in this browser.');
      return false;
    }
    const sub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey)
    });
    const keys = subscriptionKeys(sub);
    const res = await fetch('/account/notifications/push/subscribe', {
      method: 'POST',
      credentials: 'same-origin',
      headers: csrfHeaders(),
      body: JSON.stringify({
        endpoint: sub.endpoint,
        p256dhKey: keys.p256dh,
        authKey: keys.auth
      })
    });
    if (!res.ok) {
      // Roll back the local subscription so a retry starts clean.
      try { await sub.unsubscribe(); } catch (_) { /* ignore */ }
      showToggle(false);
      return false;
    }
    showToggle(true);
    return true;
  }

  async function unsubscribe() {
    const sub = await currentSubscription();
    if (!sub) { showToggle(false); return true; }
    const endpoint = sub.endpoint;
    // Server-side removal first: if the client-side unsubscribe fails
    // (extension quirk, offline), we still want the server to stop
    // trying to reach a dead endpoint.
    try {
      await fetch('/account/notifications/push/subscribe', {
        method: 'DELETE',
        credentials: 'same-origin',
        headers: csrfHeaders(),
        body: JSON.stringify({ endpoint: endpoint })
      });
    } catch (_) { /* best-effort */ }
    try { await sub.unsubscribe(); } catch (_) { /* ignore */ }
    showToggle(false);
    return true;
  }

  async function init() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) {
      showComingSoon('Real-time alerts aren’t supported in this browser.');
      return;
    }
    let publicKey;
    try {
      publicKey = await fetchVapidKey();
    } catch (_) {
      showComingSoon();
      return;
    }
    if (!publicKey) {
      showComingSoon();
      return;
    }
    const existing = await currentSubscription();
    showToggle(!!existing && Notification.permission === 'granted');

    if (!toggle) return;
    toggle.addEventListener('change', async () => {
      toggle.disabled = true;
      try {
        if (toggle.checked) {
          await subscribe(publicKey);
        } else {
          await unsubscribe();
        }
      } catch (err) {
        console.error('push toggle failed', err);
        const sub = await currentSubscription();
        showToggle(!!sub);
      } finally {
        toggle.disabled = false;
      }
    });
  }

  init();
})();
