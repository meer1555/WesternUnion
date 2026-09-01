/* Small fetch wrapper shared by every page: attaches the JWT,
   normalizes errors, and centralizes auth-storage helpers. */
const API_BASE = '/api';

const Auth = {
  getToken() { return localStorage.getItem('wu_token'); },
  setSession(data) {
    localStorage.setItem('wu_token', data.token);
    localStorage.setItem('wu_user', JSON.stringify({
      fullName: data.fullName,
      email: data.email,
      accountNumber: data.accountNumber
    }));
  },
  getUser() {
    try { return JSON.parse(localStorage.getItem('wu_user') || 'null'); }
    catch (e) { return null; }
  },
  clear() {
    localStorage.removeItem('wu_token');
    localStorage.removeItem('wu_user');
  },
  requireAuth() {
    if (!this.getToken()) window.location.href = 'login.html';
  },
  redirectIfLoggedIn() {
    if (this.getToken()) window.location.href = 'dashboard.html';
  }
};

async function api(path, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  const token = Auth.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(API_BASE + path, Object.assign({}, options, { headers }));

  let body = null;
  const text = await res.text();
  if (text) {
    try { body = JSON.parse(text); } catch (e) { body = text; }
  }

  if (!res.ok) {
    const message = (body && body.message) ? body.message : `Request failed (${res.status})`;
    if (res.status === 401 && path !== '/auth/login') {
      Auth.clear();
    }
    throw new Error(message);
  }

  return body;
}

function money(amount) {
  const n = Number(amount || 0);
  return n.toLocaleString('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });
}

function initials(name) {
  if (!name) return 'WU';
  return name.trim().split(/\s+/).map(p => p[0]).join('').slice(0, 2).toUpperCase();
}

function showToast(message, type = 'success') {
  let stack = document.querySelector('.toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.className = 'toast-stack';
    document.body.appendChild(stack);
  }
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  stack.appendChild(el);
  setTimeout(() => {
    el.style.transition = 'opacity 0.4s, transform 0.4s';
    el.style.opacity = '0';
    el.style.transform = 'translateX(40px)';
    setTimeout(() => el.remove(), 400);
  }, 3800);
}

function hidePageLoader() {
  const loader = document.querySelector('.page-loader');
  if (loader) setTimeout(() => loader.classList.add('hide'), 350);
}

document.addEventListener('DOMContentLoaded', () => {
  hidePageLoader();

  // scroll-reveal for elements marked .reveal
  const revealEls = document.querySelectorAll('.reveal');
  if (revealEls.length) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) entry.target.classList.add('in-view');
      });
    }, { threshold: 0.15 });
    revealEls.forEach(el => io.observe(el));
  }
});
