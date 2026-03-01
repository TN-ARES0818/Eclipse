// ════════════════════════════════════════════════════════
// ECLIPSE RESULTS — results.js  Dawn 0.2
// ════════════════════════════════════════════════════════

const SEARX_BASE = 'https://search.mdosch.de/search';
const SEARX_FALLBACKS = [
  'https://search.mdosch.de/search',
  'https://searx.tiekoetter.com/search',
  'https://searx.be/search',
];
let currentFallback = 0;

const State = {
  query:   '',
  tab:     'all',
  results: [],
  loading: false,
  error:   null,
  totalResults: 0,
  timeMs: 0,
};

// ── URL PARAMS ────────────────────────────────────────────────────────
function getParams() {
  const p = new URLSearchParams(window.location.search);
  State.query = p.get('q') || '';
  State.tab   = p.get('tab') || 'all';
}

// ── INIT ──────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  getParams();
  if (!State.query) { showEmpty('Type something to search'); return; }

  // Fill search bar
  const inp = document.getElementById('resSearchInput');
  if (inp) inp.value = State.query;

  // Mark active tab
  document.querySelectorAll('.res-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.tab === State.tab);
  });

  // Load accent from storage
  try {
    const ac = localStorage.getItem('eclipse_accent') || '#E8C840';
    document.documentElement.style.setProperty('--ac', ac);
  } catch(e) {}

  fetchResults();
});

// ── TAB SWITCH ────────────────────────────────────────────────────────
function switchTab(el, tab) {
  document.querySelectorAll('.res-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  State.tab = tab;
  fetchResults();
}

// ── RE-SEARCH ─────────────────────────────────────────────────────────
function reSearch() {
  const inp = document.getElementById('resSearchInput');
  const q   = inp?.value.trim();
  if (!q) return;
  State.query = q;

  // Check if it's a URL
  const isURL = /^(https?:\/\/|www\.)/i.test(q) || (q.includes('.') && !q.includes(' ') && q.length < 80);
  if (isURL) {
    const url = q.startsWith('http') ? q : 'https://' + q;
    try { Android.openUrl(url); } catch(e) { window.location.href = url; }
    return;
  }

  fetchResults();
}

function clearSearch() {
  const inp = document.getElementById('resSearchInput');
  if (inp) { inp.value = ''; inp.focus(); }
}

// ── NAVIGATE BACK ──────────────────────────────────────────────────────
function goBackToHome() {
  try { Android.goHome(); } catch(e) {
    try { Android.goBack(); } catch(e2) { history.back(); }
  }
}

// ── FETCH RESULTS ─────────────────────────────────────────────────────
async function fetchResults() {
  if (!State.query) return;
  State.loading = true;
  State.error   = null;

  showSkeletons();
  document.getElementById('resMeta').textContent = '';
  document.getElementById('resFooter').style.display = 'none';

  const categories = tabToCategories(State.tab);
  const url = `${SEARX_FALLBACKS[currentFallback]}?q=${encodeURIComponent(State.query)}&format=json&categories=${categories}&language=en`;

  const t0 = Date.now();

  try {
    const raw = await eclFetch(url);
    const data = JSON.parse(raw);
    State.timeMs = Date.now() - t0;
    State.totalResults = data.number_of_results || 0;
    State.results = data.results || [];

    if (State.tab === 'images') {
      renderImages(State.results);
    } else {
      renderResults(State.results);
    }

    const meta = document.getElementById('resMeta');
    if (meta) {
      meta.textContent = `${formatNum(State.totalResults)} results · ${State.timeMs}ms`;
    }

    const footer = document.getElementById('resFooter');
    if (footer) footer.style.display = 'block';

  } catch(e) {
    // Try next fallback
    if (currentFallback < SEARX_FALLBACKS.length - 1) {
      currentFallback++;
      fetchResults();
    } else {
      currentFallback = 0;
      showError(e.message || 'Search failed');
    }
  }

  State.loading = false;
}

function tabToCategories(tab) {
  switch(tab) {
    case 'news':   return 'news';
    case 'images': return 'images';
    case 'videos': return 'videos';
    default:       return 'general';
  }
}

// ── ANDROID FETCH BRIDGE ──────────────────────────────────────────────
window._eclFetchCbs = {};
window.EclFetchCallback = function(id, data, err) {
  const cb = window._eclFetchCbs[id];
  if (!cb) return;
  delete window._eclFetchCbs[id];
  cb(data, err);
};

function eclFetch(url) {
  return new Promise((resolve, reject) => {
    const id = 'r_' + Date.now() + '_' + Math.random().toString(36).slice(2);
    const timer = setTimeout(() => {
      delete window._eclFetchCbs[id];
      reject(new Error('Request timed out'));
    }, 18000);

    window._eclFetchCbs[id] = (data, err) => {
      clearTimeout(timer);
      if (err) { reject(new Error(err)); return; }
      try { resolve(atob(data)); }
      catch(e) { resolve(data); }
    };

    try {
      Android.fetchUrl(url, id);
    } catch(e) {
      clearTimeout(timer);
      delete window._eclFetchCbs[id];
      fetch(url, { signal: AbortSignal.timeout(16000) })
        .then(r => r.text()).then(resolve).catch(reject);
    }
  });
}

// ── RENDER RESULTS ────────────────────────────────────────────────────
function renderResults(results) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (!list) return;

  list.style.display = 'block';
  if (grid) grid.style.display = 'none';
  list.innerHTML = '';

  if (!results.length) {
    showEmpty(`No results for "${State.query}"`);
    return;
  }

  results.forEach((item, i) => {
    const card = document.createElement('div');
    card.className = 'res-card' + (State.tab === 'news' ? ' news-card-type' : '');
    card.style.animationDelay = Math.min(i * 30, 300) + 'ms';

    const domain  = getDomain(item.url);
    const favicon = `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
    const engines = (item.engines || [item.engine || '']).join(', ');
    const pub     = item.publishedDate ? formatDate(item.publishedDate) : '';
    const desc    = (item.content || '').replace(/\s+/g,' ').trim();

    card.innerHTML = `
      <div class="res-card-top">
        <div class="res-favicon-wrap">
          <img class="res-favicon" src="${favicon}"
            onerror="this.style.display='none';this.nextElementSibling.style.display='flex'" loading="lazy">
          <span class="res-favicon-fallback" style="display:none">🌐</span>
        </div>
        <span class="res-domain">${escHtml(domain)}</span>
        ${engines ? `<span class="res-engine-tag">${escHtml(engines.split(',')[0].trim())}</span>` : ''}
      </div>
      <div class="res-title">${escHtml(item.title || 'No title')}</div>
      ${desc ? `<div class="res-desc">${escHtml(desc)}</div>` : ''}
      ${pub  ? `<div class="res-pub-date">📅 ${pub}</div>` : ''}
      <div class="res-url">${escHtml(item.url || '')}</div>`;

    card.addEventListener('click', () => openResult(item.url));
    list.appendChild(card);
  });
}

// ── RENDER IMAGES ─────────────────────────────────────────────────────
function renderImages(results) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (!grid) return;

  if (list) list.style.display = 'none';
  grid.style.display = 'grid';
  grid.innerHTML = '';

  const imgResults = results.filter(r => r.img_src || r.thumbnail);
  if (!imgResults.length) {
    grid.style.display = 'none';
    if (list) list.style.display = 'block';
    showEmpty('No images found');
    return;
  }

  imgResults.forEach(item => {
    const card  = document.createElement('div');
    card.className = 'res-img-card';
    const thumb = item.thumbnail || item.img_src;
    const title = item.title || getDomain(item.url);

    card.innerHTML = `
      <img src="${escHtml(thumb)}" loading="lazy"
        onerror="this.parentElement.style.display='none'"
        alt="${escHtml(title)}">
      <div class="res-img-card-label">${escHtml(title)}</div>`;

    card.addEventListener('click', () => openResult(item.url));
    grid.appendChild(card);
  });
}

// ── SKELETON ──────────────────────────────────────────────────────────
function showSkeletons(n = 6) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (list) { list.style.display = 'block'; list.innerHTML = ''; }
  if (grid) grid.style.display = 'none';

  for (let i = 0; i < n; i++) {
    const s = document.createElement('div');
    s.className = 'res-skeleton';
    s.innerHTML = `
      <div class="skel-line skel-top"></div>
      <div class="skel-line skel-title"></div>
      <div class="skel-line skel-title2"></div>
      <div class="skel-line skel-desc"></div>
      <div class="skel-line skel-desc2"></div>`;
    list.appendChild(s);
  }
}

// ── ERROR / EMPTY STATES ───────────────────────────────────────────────
function showError(msg) {
  const list = document.getElementById('resList');
  if (!list) return;
  list.innerHTML = `
    <div class="res-error">
      <div class="res-error-icon">⚠️</div>
      <div class="res-error-title">Search failed</div>
      <div class="res-error-sub">${escHtml(msg)}</div>
      <button class="res-retry-btn" onclick="fetchResults()">Try Again</button>
    </div>`;
}

function showEmpty(msg) {
  const list = document.getElementById('resList');
  if (!list) return;
  list.innerHTML = `
    <div class="res-empty">
      <div class="res-empty-icon">🔭</div>
      <div class="res-empty-title">${escHtml(msg)}</div>
      <div class="res-empty-sub">Try different keywords</div>
    </div>`;
}

// ── OPEN RESULT ────────────────────────────────────────────────────────
function openResult(url) {
  if (!url) return;
  try { Android.openUrl(url); }
  catch(e) { window.location.href = url; }
}

// ── UTILS ─────────────────────────────────────────────────────────────
function getDomain(url) {
  try { return new URL(url).hostname.replace('www.',''); }
  catch(e) { return url.slice(0,30); }
}

function escHtml(str) {
  return String(str)
    .replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>')
    .replace(/&quot;/g,'"').replace(/&#39;/g,"'")
    .replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

function formatNum(n) {
  if (!n) return '?';
  if (n >= 1000000) return (n/1000000).toFixed(1) + 'M';
  if (n >= 1000) return (n/1000).toFixed(0) + 'K';
  return String(n);
}

function formatDate(dateStr) {
  try {
    const d = new Date(dateStr);
    const diff = Math.floor((Date.now() - d.getTime()) / 1000);
    if (diff < 3600) return `${Math.floor(diff/60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff/3600)}h ago`;
    if (diff < 604800) return `${Math.floor(diff/86400)}d ago`;
    return d.toLocaleDateString('en', { month:'short', day:'numeric' });
  } catch(e) { return ''; }
}

// Toast helper
function showToast(msg) {
  const t = document.getElementById('restoast');
  if (!t) return;
  t.textContent = msg; t.classList.add('show');
  clearTimeout(t._t);
  t._t = setTimeout(() => t.classList.remove('show'), 2200);
}
