(function () {
  if (!window.Bridge || !window.Bridge.save || window.__w20dl) return;
  window.__w20dl = 1;

  var origToast = window.toast;
  window.downloadImage = function (src, file, thumb) {
    try {
      var url = thumb || src;
      var name = (typeof cleanName === 'function') ? cleanName(file, url) : 'image.jpg';
      if (origToast) origToast('Saving ' + name + '…');
      var go = function (u) {
        fetch(u, { mode: 'cors' }).then(function (r) {
          if (!r.ok) throw new Error('HTTP ' + r.status);
          return r.blob();
        }).then(function (b) {
          var fr = new FileReader();
          fr.onload = function () { Bridge.save(name, String(fr.result)); };
          fr.readAsDataURL(b);
        }).catch(function () {
          if (origToast) origToast('Download failed — check connection');
        });
      };
      if (file && typeof commons === 'function') {
        commons({ action: 'query', titles: file, prop: 'imageinfo', iiprop: 'url', iiurlwidth: '1600' })
          .then(function (d) {
            var pg = (d.query && d.query.pages) ? d.query.pages[0] : null;
            var ii = pg && pg.imageinfo ? pg.imageinfo[0] : null;
            go(ii && ii.thumburl ? ii.thumburl : url);
          }).catch(function () { go(url); });
      } else {
        go(url);
      }
    } catch (e) {
      if (origToast) origToast('Download failed');
    }
  };
})();

/* ---- transparent status bar: push the header down, keep the glass behind the bar ---- */
(function () {
  if (!window.Bridge || !window.Bridge.sb || window.__w20sb) return;
  window.__w20sb = 1;
  var h = 0;
  try { h = Bridge.sb() | 0; } catch (e) { h = 0; }
  if (h > 0) {
    var st = document.createElement('style');
    st.id = 'w20-sb';
    st.textContent = 'header.top{padding-top:' + h + 'px}';
    document.head.appendChild(st);
  }
  function isDark() {
    var t = document.documentElement.getAttribute('data-theme');
    if (t) return t === 'dark';
    return !!(window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches);
  }
  function sync() { try { Bridge.statusBar(isDark()); } catch (e) {} }
  var orig = window.toggleTheme;
  if (typeof orig === 'function') {
    window.toggleTheme = function () { var r = orig.apply(this, arguments); setTimeout(sync, 30); return r; };
  }
  sync();
})();
