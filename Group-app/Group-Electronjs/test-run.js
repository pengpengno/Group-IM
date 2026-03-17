const { spawn } = require('child_process');
const child = spawn('npx.cmd', ['electron', '.'], { stdio: 'pipe', shell: true });
child.stdout.on('data', d => console.log(d.toString()));
child.stderr.on('data', d => console.error(d.toString()));
setTimeout(() => {
    child.kill();
    process.exit(0);
}, 5000);
