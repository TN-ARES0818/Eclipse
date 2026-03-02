// ════════════════════════════════════════════════════════════════════════════
// ECLIPSE RESULTS — results.js  Dawn 0.2
// Smart SearXNG search with HTML detection and smart instance fallback
// ════════════════════════════════════════════════════════════════════════════

// ── SEARXNG INSTANCES ────────────────────────────────────────────────────
const INSTANCES = [
  'https://search.ononoki.org',
  'https://searx.tiekoetter.com',
  'https://searxng.world',
  'https://sx.catgirl.cloud',
  'https://searx.prvcy.eu',
  'https://priv.au',
  'https://searx.work',
  'https://searx.be',
];

const failedInstances = new Set();
let currentIdx = 0;

const S = {
  query: '', tab: 'all', results: [],
  loading: false, total: 0, ms: 0,
};

// ── INIT ──────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  loadAccentColor();
  parseUrlParams();
  if (!S.query) { showEmpty('Type something to search'); return; }
  const inp = document.getElementById('resSearchInput');
  if (inp) inp.value = S.query;
  document.querySelectorAll('.res-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.tab === S.tab);
  });
  fetchResults();
});

function loadAccentColor() {
  try {
    const ac = localStorage.getItem('eclipse_accent') || '#E8C840';
    document.documentElement.style.setProperty('--ac', ac);
  } catch(e) {}
}

function parseUrlParams() {
  try {
    const p = new URLSearchParams(window.location.search);
    S.query = decodeURIComponent(p.get('q') || '').trim();
    S.tab   = p.get('tab') || 'all';
  } catch(e) { S.query = ''; S.tab = 'all'; }
}

function switchTab(el, tab) {
  if (S.loading) return;
  document.querySelectorAll('.res-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  S.tab = tab;
  fetchResults();
}

function reSearch() {
  const inp = document.getElementById('resSearchInput');
  const q   = (inp?.value || '').trim();
  if (!q) return;
  const isURL = /^(https?:\/\/|www\.)/i.test(q) || (q.includes('.') && !q.includes(' ') && q.length < 80);
  if (isURL) { openResult(q.startsWith('http') ? q : 'https://' + q); return; }
  S.query = q;
  try {
    const url = new URL(window.location.href);
    url.searchParams.set('q', q);
    window.history.replaceState({}, '', url.toString());
  } catch(e) {}
  fetchResults();
}

function clearSearch() {
  const inp = document.getElementById('resSearchInput');
  if (inp) { inp.value = ''; inp.focus(); }
}

function goBackToHome() {
  try { Android.goHome(); }
  catch(e) { try { Android.goBack(); } catch(e2) { history.back(); } }
}

// ── CORE FETCH WITH HTML DETECTION ───────────────────────────────────────
async function fetchResults() {
  if (!S.query || S.loading) return;
  S.loading = true;
  showSkeletons();
  setMeta('');
  hideFooter();

  const t0  = Date.now();
  let data   = null;
  let lastErr = 'All search instances are currently unavailable';

  for (let attempt = 0; attempt < INSTANCES.length; attempt++) {
    const idx = getNextGoodIdx();
    if (idx === -1) break;

    const base = INSTANCES[idx];
    const cat  = tabToCategory(S.tab);
    const url  = `${base}/search?q=${encodeURIComponent(S.query)}&format=json&categories=${cat}&language=en-US`;

    try {
      const raw = await eclFetch(url, 13000);

      // CRITICAL FIX: detect HTML response — means instance is blocking us
      const trimmed = raw.trimStart();
      if (trimmed.startsWith('<') || trimmed.startsWith('<!') || trimmed.toLowerCase().includes('<!doctype')) {
        failedInstances.add(base);
        currentIdx = (idx + 1) % INSTANCES.length;
        continue;
      }

      // Try parse JSON
      try {
        data = JSON.parse(raw);
        // Make sure it has results structure
        if (typeof data !== 'object' || data === null) throw new Error('Invalid response structure');
        currentIdx = idx;
        break;
      } catch(pe) {
        failedInstances.add(base);
        currentIdx = (idx + 1) % INSTANCES.length;
        lastErr = 'Invalid response from ' + base;
        continue;
      }

    } catch(fe) {
      failedInstances.add(base);
      currentIdx = (idx + 1) % INSTANCES.length;
      lastErr = fe.message || 'Network error';
      continue;
    }
  }

  S.ms      = Date.now() - t0;
  S.loading = false;

  if (!data) {
    failedInstances.clear(); // reset for next attempt
    showError(lastErr);
    return;
  }

  S.total   = data.number_of_results || 0;
  S.results = data.results || [];

  if (S.tab === 'images') renderImages(S.results);
  else renderResults(S.results);

  setMeta(`${fmtNum(S.total)} results · ${S.ms}ms`);
  showFooter();
}

function getNextGoodIdx() {
  for (let i = 0; i < INSTANCES.length; i++) {
    const idx = (currentIdx + i) % INSTANCES.length;
    if (!failedInstances.has(INSTANCES[idx])) { currentIdx = idx; return idx; }
  }
  return -1;
}

function tabToCategory(tab) {
  if (tab === 'news')   return 'news';
  if (tab === 'images') return 'images';
  if (tab === 'videos') return 'videos';
  return 'general';
}

// ── RENDER RESULTS ────────────────────────────────────────────────────────
function renderResults(results) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (!list) return;
  list.style.display = 'block';
  if (grid) grid.style.display = 'none';
  list.innerHTML = '';
  if (!results || !results.length) { showEmpty('No results found for "' + S.query + '"'); return; }

  results.forEach((item, i) => {
    const card    = document.createElement('div');
    card.className = 'res-card' + (S.tab === 'news' ? ' news-card-type' : '');
    card.style.animationDelay = Math.min(i * 25, 300) + 'ms';

    const domain  = getDomain(item.url);
    const favicon = 'https://www.google.com/s2/favicons?domain=' + domain + '&sz=32';
    const engines = (Array.isArray(item.engines) ? item.engines : [item.engine || '']).filter(Boolean);
    const pub     = item.publishedDate ? fmtDate(item.publishedDate) : '';
    const desc    = (item.content || '').replace(/\s+/g, ' ').trim();
    const title   = deEnt(item.title || 'No title');
    const engTag  = engines.length ? engines[0] : '';

    card.innerHTML =
      '<div class="res-card-top">' +
        '<div class="res-favicon-wrap">' +
          '<img class="res-favicon" src="' + esc(favicon) + '" loading="lazy" ' +
          'onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'block\'">' +
          '<span class="res-favicon-fallback" style="display:none">🌐</span>' +
        '</div>' +
        '<span class="res-domain">' + esc(domain) + '</span>' +
        (engTag ? '<span class="res-engine-tag">' + esc(engTag) + '</span>' : '') +
      '</div>' +
      '<div class="res-title">' + esc(title) + '</div>' +
      (desc ? '<div class="res-desc">' + esc(desc) + '</div>' : '') +
      (pub  ? '<div class="res-pub-date">📅 ' + pub + '</div>' : '') +
      '<div class="res-url">' + esc(item.url || '') + '</div>';

    card.addEventListener('click', () => openResult(item.url));
    list.appendChild(card);
  });
}

// ── RENDER IMAGES ─────────────────────────────────────────────────────────
function renderImages(results) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (!grid) { renderResults(results); return; }
  if (list) list.style.display = 'none';
  grid.style.display = 'grid';
  grid.innerHTML = '';

  const imgs = (results || []).filter(r => r.img_src || r.thumbnail);
  if (!imgs.length) {
    grid.style.display = 'none';
    if (list) list.style.display = 'block';
    showEmpty('No images found — try the All tab');
    return;
  }

  imgs.forEach(item => {
    const card  = document.createElement('div');
    card.className = 'res-img-card';
    const src   = item.thumbnail || item.img_src || '';
    const title = deEnt(item.title || getDomain(item.url));
    card.innerHTML =
      '<img src="' + esc(src) + '" loading="lazy" ' +
      'onerror="this.parentElement.style.display=\'none\'" alt="' + esc(title) + '">' +
      '<div class="res-img-card-label">' + esc(title) + '</div>';
    card.addEventListener('click', () => openResult(item.url));
    grid.appendChild(card);
  });
}

// ── SKELETONS ─────────────────────────────────────────────────────────────
function showSkeletons(n) {
  n = n || 7;
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (!list) return;
  list.style.display = 'block';
  list.innerHTML = '';
  if (grid) grid.style.display = 'none';
  for (var i = 0; i < n; i++) {
    var s = document.createElement('div');
    s.className = 'res-skeleton';
    s.style.animationDelay = (i * 40) + 'ms';
    s.innerHTML = '<div class="skel-line skel-top"></div>' +
      '<div class="skel-line skel-title"></div>' +
      '<div class="skel-line skel-title2"></div>' +
      '<div class="skel-line skel-desc"></div>' +
      '<div class="skel-line skel-desc2"></div>';
    list.appendChild(s);
  }
}

// ── ERROR / EMPTY STATES ──────────────────────────────────────────────────
function showError(msg) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (list) list.style.display = 'block';
  if (grid) grid.style.display = 'none';
  if (!list) return;
  list.innerHTML = '<div class="res-error">' +
    '<div class="res-error-icon">⚠️</div>' +
    '<div class="res-error-title">Search unavailable</div>' +
    '<div class="res-error-sub">' + esc(msg) + '</div>' +
    '<div class="res-error-hint">Check your internet connection and try again.</div>' +
    '<button class="res-retry-btn" onclick="fetchResults()">Try Again</button>' +
    '</div>';
}

function showEmpty(msg) {
  const list = document.getElementById('resList');
  const grid = document.getElementById('resImgGrid');
  if (list) list.style.display = 'block';
  if (grid) grid.style.display = 'none';
  if (!list) return;
  list.innerHTML = '<div class="res-empty">' +
    '<div class="res-empty-icon">🔭</div>' +
    '<div class="res-empty-title">' + esc(msg) + '</div>' +
    '<div class="res-empty-sub">Try different keywords</div>' +
    '</div>';
}

// ── UI HELPERS ────────────────────────────────────────────────────────────
function setMeta(text) { var m = document.getElementById('resMeta'); if (m) m.textContent = text; }
function showFooter()   { var f = document.getElementById('resFooter'); if (f) f.style.display = 'block'; }
function hideFooter()   { var f = document.getElementById('resFooter'); if (f) f.style.display = 'none'; }

function openResult(url) {
  if (!url) return;
  try { Android.openUrl(url); }
  catch(e) { window.location.href = url; }
}

// ── ANDROID FETCH BRIDGE ──────────────────────────────────────────────────
window._eclFetchCbs = {};
window.EclFetchCallback = function(id, b64, err) {
  var cb = window._eclFetchCbs[id];
  if (!cb) return;
  delete window._eclFetchCbs[id];
  if (err) { cb(null, new Error(err)); return; }
  try { cb(atob(b64), null); } catch(e) { cb(b64, null); }
};

function eclFetch(url, ms) {
  ms = ms || 14000;
  return new Promise(function(resolve, reject) {
    var id    = 'ecl_' + Date.now() + '_' + Math.random().toString(36).slice(2);
    var timer = setTimeout(function() {
      delete window._eclFetchCbs[id];
      reject(new Error('Timed out'));
    }, ms);

    window._eclFetchCbs[id] = function(text, err) {
      clearTimeout(timer);
      if (err) { reject(err); } else { resolve(text); }
    };

    try {
      Android.fetchUrl(url, id);
    } catch(e) {
      clearTimeout(timer);
      delete window._eclFetchCbs[id];
      var ctrl = new AbortController();
      var t2   = setTimeout(function() { ctrl.abort(); }, ms);
      fetch(url, { signal: ctrl.signal })
        .then(function(r) { return r.text(); })
        .then(function(txt) { clearTimeout(t2); resolve(txt); })
        .catch(function(er) { clearTimeout(t2); reject(er); });
    }
  });
}

// ── UTILS ─────────────────────────────────────────────────────────────────
function getDomain(url) {
  try { return new URL(url).hostname.replace('www.', ''); }
  catch(e) { return (url || '').slice(0, 30); }
}

function esc(str) {
  return String(str || '')
    .replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"').replace(/&#39;/g,"'")
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function deEnt(str) {
  return String(str || '')
    .replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"').replace(/&#39;/g,"'").replace(/&nbsp;/g,' ');
}

function fmtNum(n) {
  if (!n) return '?';
  if (n >= 1000000) return (n/1000000).toFixed(1) + 'M';
  if (n >= 1000)    return (n/1000).toFixed(0) + 'K';
  return String(n);
}

function fmtDate(str) {
  try {
    var d    = new Date(str);
    var diff = Math.floor((Date.now() - d.getTime()) / 1000);
    if (diff < 60)     return 'Just now';
    if (diff < 3600)   return Math.floor(diff/60) + 'm ago';
    if (diff < 86400)  return Math.floor(diff/3600) + 'h ago';
    if (diff < 604800) return Math.floor(diff/86400) + 'd ago';
    return d.toLocaleDateString('en', {month:'short', day:'numeric'});
  } catch(e) { return ''; }
}

function showToast(msg) {
  var t = document.getElementById('restoast');
  if (!t) return;
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(t._t);
  t._t = setTimeout(function() { t.classList.remove('show'); }, 2500);
}
