// ════════════════════════════════════
//  CUSTOMIZE PANEL
// ════════════════════════════════════

const Customize = {
    init() {
        // Set initial state of toggles based on stored settings
        const weatherEnabled = Store.getSetting('weatherWidget', false);
        const weatherToggle = document.getElementById('toggleWeather');
        if (weatherToggle) {
            weatherToggle.classList.toggle('on', weatherEnabled);
        }

        const newsEnabled = Store.getSetting('newsSection', true);
        const newsToggle = document.getElementById('toggleNews');
        if (newsToggle) {
            newsToggle.classList.toggle('on', newsEnabled);
        }

        const sitesEnabled = Store.getSetting('sitesSection', true);
        const sitesToggle = document.getElementById('toggleSites');
        if (sitesToggle) {
            sitesToggle.classList.toggle('on', sitesEnabled);
        }
    },

    open() {
        closeMenu();
        document.getElementById('cpOverlay').classList.add('open');
    },

    close() {
        document.getElementById('cpOverlay').classList.remove('open');
    },

    toggleWeather(el) {
        const isOn = el.classList.toggle('on');
        Weather.toggle(isOn);
    },

    toggleNews(el) {
        const isOn = el.classList.toggle('on');
        Store.saveSetting('newsSection', isOn);
        document.querySelector('.news-section').style.display = isOn ? '' : 'none';
    },

    toggleSites(el) {
        const isOn = el.classList.toggle('on');
        Store.saveSetting('sitesSection', isOn);
        document.querySelector('.sites-section').style.display = isOn ? '' : 'none';
    }
};
