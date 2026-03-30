const fs = require('fs');
const path = require('path');
const os = require('os');

const args = process.argv.slice(2);
const modeArg = args.indexOf('--mode');
const envArg = args.indexOf('--env');

const mode = modeArg !== -1 ? args[modeArg + 1] : 'desktop';
const env = envArg !== -1 ? args[envArg + 1] : 'development';
const configFile = `${env}.json`;
const configPath = path.resolve(__dirname, `../configs/${configFile}`);

const colors = {
    reset: "\x1b[0m",
    bright: "\x1b[1m",
    dim: "\x1b[2m",
    underscore: "\x1b[4m",
    blink: "\x1b[5m",
    reverse: "\x1b[7m",
    hidden: "\x1b[8m",
    fg: {
        black: "\x1b[30m",
        red: "\x1b[31m",
        green: "\x1b[32m",
        yellow: "\x1b[33m",
        blue: "\x1b[34m",
        magenta: "\x1b[35m",
        cyan: "\x1b[36m",
        white: "\x1b[37m",
        crimson: "\x1b[38m"
    },
    bg: {
        black: "\x1b[40m",
        red: "\x1b[41m",
        green: "\x1b[42m",
        yellow: "\x1b[43m",
        blue: "\x1b[44m",
        magenta: "\x1b[45m",
        cyan: "\x1b[36m",
        white: "\x1b[47m",
        crimson: "\x1b[48m"
    }
};

let packageJson = { name: 'unknown', version: '0.0.0', description: 'IM Client' };
try {
    packageJson = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../package.json'), 'utf8'));
} catch (e) {
    // ignore
}

const banner = `
${colors.fg.cyan}${colors.bright}
   ____                                   ___  __  __ 
  / ___|_ __ ___  _   _ _ __             |_ _| \\/  |
 | |  _| '__/ _ \\| | | | '_ \\  _____      | | | |\\/| |
 | |_| | | | (_) | |_| | |_) ||_____|     | | | |  | |
  \\____|_|  \\___/ \\__,_| .__/            |___||_|  |_|
                       |_|                            
${colors.reset}
${colors.fg.green}${colors.bright}> ${packageJson.description || 'Messenger Client'} ${colors.reset}
`;

console.log(banner);
console.log(`${colors.fg.yellow}------------------------------------------------------------${colors.reset}`);
console.log(`${colors.bright}🚀 Project:${colors.reset}    ${packageJson.name} v${packageJson.version}`);
console.log(`${colors.bright}💻 Platform:${colors.reset}   ${mode === 'desktop' ? 'Desktop (Electron)' : 'Web (Browser)'}`);
console.log(`${colors.bright}🌐 Environment:${colors.reset} ${env === 'dev' ? colors.fg.magenta + 'Development' : colors.fg.green + 'Production'}${colors.reset}`);
console.log(`${colors.bright}📂 Config File:${colors.reset}  configs/${configFile}`);
console.log(`${colors.bright}⏰ Start Time:${colors.reset}  ${new Date().toLocaleString()}`);
console.log(`${colors.bright}🛠  Node Version:${colors.reset} ${process.version}`);
console.log(`${colors.bright}🏠 Hostname:${colors.reset}    ${os.hostname()}`);
console.log(`${colors.fg.yellow}------------------------------------------------------------${colors.reset}`);

// Display Environment Variable Overrides
if (process.env.API_BASE || process.env.TCP_HOST || process.env.TCP_PORT) {
    console.log(`${colors.bright}⚙️  Config Overrides:${colors.reset}`);
    if (process.env.API_BASE) console.log(`   - API_BASE:  ${colors.fg.cyan}${process.env.API_BASE}${colors.reset}`);
    if (process.env.TCP_HOST) console.log(`   - TCP_HOST:  ${colors.fg.cyan}${process.env.TCP_HOST}${colors.reset}`);
    if (process.env.TCP_PORT) console.log(`   - TCP_PORT:  ${colors.fg.cyan}${process.env.TCP_PORT}${colors.reset}`);
    console.log(`${colors.fg.yellow}------------------------------------------------------------${colors.reset}`);
}

const protoDir = path.resolve(__dirname, '../main/generated');
try {
    if (fs.existsSync(protoDir) && fs.readdirSync(protoDir).length > 0) {
        console.log(`${colors.fg.green}✅ Protobuf bundle detected.${colors.reset}`);
    } else {
        console.log(`${colors.fg.yellow}⚠️  Protobuf bundle not found. Syncing soon...${colors.reset}`);
    }
} catch (e) {
    console.log(`${colors.fg.yellow}⚠️  Protobuf status unknown.${colors.reset}`);
}

console.log(`\n${colors.dim}Preparing to start services...${colors.reset}\n`);
