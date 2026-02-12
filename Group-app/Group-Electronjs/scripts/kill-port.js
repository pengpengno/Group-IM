#!/usr/bin/env node

const { exec } = require('child_process');
const os = require('os');

const PORTS = [3000, 3001, 3002, 3003, 8080];

function killProcessesOnPorts() {
  const platform = os.platform();
  
  PORTS.forEach(port => {
    console.log(`Checking port ${port}...`);
    
    if (platform === 'win32') {
      // Windows command to find and kill processes
      exec(`netstat -ano | findstr :${port}`, (error, stdout) => {
        if (stdout) {
          const lines = stdout.split('\n');
          lines.forEach(line => {
            const parts = line.trim().split(/\s+/);
            if (parts.length >= 5 && parts[1].includes(`:${port}`)) {
              const pid = parts[4];
              console.log(`Killing process ${pid} on port ${port}`);
              exec(`taskkill /PID ${pid} /F`, (killError) => {
                if (killError) {
                  console.log(`Failed to kill process ${pid}: ${killError.message}`);
                } else {
                  console.log(`Successfully killed process ${pid}`);
                }
              });
            }
          });
        }
      });
    } else {
      // Unix/Linux/Mac command
      exec(`lsof -ti :${port}`, (error, stdout) => {
        if (stdout) {
          const pids = stdout.trim().split('\n');
          pids.forEach(pid => {
            if (pid) {
              console.log(`Killing process ${pid} on port ${port}`);
              exec(`kill -9 ${pid}`, (killError) => {
                if (killError) {
                  console.log(`Failed to kill process ${pid}: ${killError.message}`);
                } else {
                  console.log(`Successfully killed process ${pid}`);
                }
              });
            }
          });
        }
      });
    }
  });
}

console.log('Killing processes on common development ports...');
killProcessesOnPorts();