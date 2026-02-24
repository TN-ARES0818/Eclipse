// ════════════════════════════════════
//  WEATHER WIDGET
// ════════════════════════════════════

const Weather = {
    enabled: false,
    data: null,

    async init() {
        this.enabled = Store.getSetting('weatherWidget', false);
        if (this.enabled) {
            document.getElementById('weatherWidget').style.display = 'flex';
            await this.fetchWeather();
            this.render();
        }
    },

    async fetchWeather() {
        try {
            const response = await androidFetch('https://api.open-meteo.com/v1/forecast?latitude=51.50&longitude=-0.12&current_weather=true');
            this.data = JSON.parse(response);
        } catch (error) {
            console.error('Error fetching weather:', error);
        }
    },

    render() {
        if (!this.data) return;
        const widget = document.getElementById('weatherWidget');
        const temp = Math.round(this.data.current_weather.temperature);
        const code = this.data.current_weather.weathercode;
        const icon = this.getIcon(code);
        widget.innerHTML = `<div class="weather-icon">${icon}</div><div class="weather-temp">${temp}°</div>`;
    },

    getIcon(code) {
        if (code === 0) return '☀️';
        if (code <= 2) return '🌤️';
        if (code === 3) return '☁️';
        if (code <= 48) return '🌫️';
        if (code <= 65) return '🌧️';
        if (code <= 67) return '🌨️';
        if (code <= 82) return '⛈️';
        if (code <= 86) return '❄️';
        if (code <= 99) return '🌪️';
        return '🌑';
    },

    toggle(on) {
        this.enabled = on;
        Store.saveSetting('weatherWidget', on);
        document.getElementById('weatherWidget').style.display = on ? 'flex' : 'none';
        if (on && !this.data) {
            this.init();
        }
    }
};
