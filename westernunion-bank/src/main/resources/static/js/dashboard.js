Auth.requireAuth();

let currentAccount = null;
let currentBalance = 0;

const el = (id) => document.getElementById(id);

function openModal(id) { el(id).classList.add('show'); }
function closeModal(id) { el(id).classList.remove('show'); }

document.querySelectorAll('[data-close]').forEach(node => {
  node.addEventListener('click', () => {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('show'));
  });
});
document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) overlay.classList.remove('show');
  });
});

function setBtnLoading(form, loading) {
  const btn = form.querySelector('button[type="submit"]');
  btn.classList.toggle('loading', loading);
  btn.disabled = loading;
}

// ---------------------------------------------------------------
// Initial load
// ---------------------------------------------------------------

async function loadAccount() {
  const user = Auth.getUser();
  if (user) {
    el('user-first-name').textContent = user.fullName.split(' ')[0];
    el('user-avatar').textContent = initials(user.fullName);
    el('account-holder').textContent = user.fullName;
  }

  try {
    const account = await api('/account/me');
    currentAccount = account;
    currentBalance = Number(account.balance);

    el('account-number').innerHTML = account.accountNumber +
      ' <button class="copy-btn" id="copy-acct-btn">Copy</button>';
    document.getElementById('copy-acct-btn').addEventListener('click', copyAccountNumber);

    animateNumber(el('balance-amount'), currentBalance, { prefix: '$' });
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function copyAccountNumber() {
  if (!currentAccount) return;
  navigator.clipboard.writeText(currentAccount.accountNumber)
    .then(() => showToast('Account number copied', 'success'))
    .catch(() => showToast('Could not copy — please copy manually', 'error'));
}

async function loadTransactions() {
  const list = el('txn-list');
  try {
    const txns = await api('/account/transactions');
    if (!txns.length) {
      list.innerHTML = '<div class="empty-state">No transactions yet. Make your first deposit to get started.</div>';
      return;
    }

    list.innerHTML = txns.slice(0, 25).map(renderTxnRow).join('');
  } catch (err) {
    list.innerHTML = `<div class="empty-state">Failed to load transactions: ${err.message}</div>`;
  }
}

function renderTxnRow(txn) {
  const isCredit = txn.type === 'DEPOSIT' || txn.type === 'TRANSFER_IN';
  const sign = isCredit ? '+' : '-';
  const icon = txn.type === 'DEPOSIT' ? '⬇️'
             : txn.type === 'WITHDRAWAL' ? '⬆️'
             : txn.type === 'TRANSFER_OUT' ? '↗️'
             : '↘️';
  const title = txn.type === 'DEPOSIT' ? 'Deposit'
              : txn.type === 'WITHDRAWAL' ? 'Withdrawal'
              : txn.type === 'TRANSFER_OUT' ? `Sent to ${txn.toAccountNumber || ''}`
              : `Received from ${txn.fromAccountNumber || ''}`;
  const date = new Date(txn.timestamp).toLocaleString('en-US', {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
  });

  return `
    <div class="txn-row">
      <div class="txn-left">
        <div class="txn-icon ${isCredit ? 'in' : 'out'}">${icon}</div>
        <div>
          <div class="txn-title">${escapeHtml(title)}</div>
          <div class="txn-sub">${date} · ${escapeHtml(txn.description || '')}</div>
        </div>
      </div>
      <div class="txn-amt ${isCredit ? 'in' : 'out'}">${sign}${money(txn.amount).replace('$','$')}</div>
    </div>
  `;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str || '';
  return div.innerHTML;
}

// ---------------------------------------------------------------
// Navigation wiring
// ---------------------------------------------------------------

el('card-deposit').addEventListener('click', () => openModal('deposit-modal'));
el('link-deposit').addEventListener('click', () => openModal('deposit-modal'));

el('card-withdraw').addEventListener('click', () => openModal('withdraw-modal'));
el('link-withdraw').addEventListener('click', () => openModal('withdraw-modal'));

el('card-transfer').addEventListener('click', () => openModal('transfer-modal'));
el('link-transfer').addEventListener('click', () => openModal('transfer-modal'));

el('link-history').addEventListener('click', () => {
  document.getElementById('history-panel').scrollIntoView({ behavior: 'smooth' });
});

el('refresh-btn').addEventListener('click', () => {
  loadAccount();
  loadTransactions();
});

el('logout-btn').addEventListener('click', () => {
  Auth.clear();
  window.location.href = 'index.html';
});

function showSuccess(title, message) {
  el('success-title').textContent = title;
  el('success-message').textContent = message;
  openModal('success-modal');
}

// ---------------------------------------------------------------
// Deposit
// ---------------------------------------------------------------

el('deposit-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  setBtnLoading(form, true);
  try {
    const amount = parseFloat(el('deposit-amount').value);
    const description = el('deposit-note').value.trim();
    const txn = await api('/account/deposit', {
      method: 'POST',
      body: JSON.stringify({ amount, description: description || undefined })
    });
    closeModal('deposit-modal');
    form.reset();
    currentBalance = Number(txn.balanceAfter);
    animateNumber(el('balance-amount'), currentBalance, { prefix: '$', from: currentBalance - amount });
    showSuccess('Deposit Successful', `${money(amount)} was added to your account.`);
    loadTransactions();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setBtnLoading(form, false);
  }
});

// ---------------------------------------------------------------
// Withdraw
// ---------------------------------------------------------------

el('withdraw-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  setBtnLoading(form, true);
  try {
    const amount = parseFloat(el('withdraw-amount').value);
    const description = el('withdraw-note').value.trim();
    const txn = await api('/account/withdraw', {
      method: 'POST',
      body: JSON.stringify({ amount, description: description || undefined })
    });
    closeModal('withdraw-modal');
    form.reset();
    currentBalance = Number(txn.balanceAfter);
    animateNumber(el('balance-amount'), currentBalance, { prefix: '$', from: currentBalance + amount });
    showSuccess('Withdrawal Successful', `${money(amount)} was withdrawn from your account.`);
    loadTransactions();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setBtnLoading(form, false);
  }
});

// ---------------------------------------------------------------
// Transfer
// ---------------------------------------------------------------

let lookupTimer = null;
el('transfer-to').addEventListener('input', (e) => {
  clearTimeout(lookupTimer);
  const value = e.target.value.trim();
  const hint = el('recipient-hint');
  if (!value) { hint.textContent = ''; return; }

  lookupTimer = setTimeout(async () => {
    try {
      const info = await api('/account/lookup/' + encodeURIComponent(value));
      hint.textContent = `Recipient: ${info.accountHolder}`;
      hint.style.color = '#23d18b';
    } catch (err) {
      hint.textContent = 'No account found with that number';
      hint.style.color = '#ff5c72';
    }
  }, 450);
});

el('transfer-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = e.target;
  setBtnLoading(form, true);
  try {
    const toAccountNumber = el('transfer-to').value.trim();
    const amount = parseFloat(el('transfer-amount').value);
    const description = el('transfer-note').value.trim();
    const txn = await api('/account/transfer', {
      method: 'POST',
      body: JSON.stringify({ toAccountNumber, amount, description: description || undefined })
    });
    closeModal('transfer-modal');
    form.reset();
    el('recipient-hint').textContent = '';
    currentBalance = Number(txn.balanceAfter);
    animateNumber(el('balance-amount'), currentBalance, { prefix: '$', from: currentBalance + amount });
    showSuccess('Transfer Sent', `${money(amount)} was sent to ${toAccountNumber}.`);
    loadTransactions();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    setBtnLoading(form, false);
  }
});

loadAccount();
loadTransactions();
