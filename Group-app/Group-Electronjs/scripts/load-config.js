const path = require('path');
const fs = require('fs');

/**
 * Loads configuration similar to Spring Boot's application.yaml
 * 1. Loads default.json
 * 2. Overlays NODE_ENV.json (e.g. development.json or production.json)
 * 3. Overlays any Shell environment variables (highest priority)
 */
function getConfig() {
    const mode = process.env.NODE_ENV || 'development';
    const configDir = path.resolve(__dirname, '../configs');
    
    // 1. Load defaults
    let config = {};
    const defaultPath = path.join(configDir, 'default.json');
    if (fs.existsSync(defaultPath)) {
        try {
            config = JSON.parse(fs.readFileSync(defaultPath, 'utf8'));
        } catch (e) {
            console.error('Error parsing default.json', e);
        }
    }

    // 2. Load mode specific config
    const modePath = path.join(configDir, `${mode}.json`);
    if (fs.existsSync(modePath)) {
        try {
            const modeConfig = JSON.parse(fs.readFileSync(modePath, 'utf8'));
            config = { ...config, ...modeConfig };
        } catch (e) {
            console.error(`Error parsing ${mode}.json`, e);
        }
    }

    // 3. Command line/Process overrides (highest priority)
    if (process.env.API_BASE) config.API_BASE = process.env.API_BASE;
    if (process.env.TCP_HOST) config.TCP_HOST = process.env.TCP_HOST;
    if (process.env.TCP_PORT) config.TCP_PORT = process.env.TCP_PORT;

    return config;
}

module.exports = getConfig;
