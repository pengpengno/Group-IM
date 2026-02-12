const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// 必需的核心依赖
const CORE_DEPENDENCIES = {
  dependencies: [
    'electron-squirrel-startup',
    '@reduxjs/toolkit',
    'axios',
    'react',
    'react-dom',
    'socket.io-client'
  ],
  devDependencies: [
    '@types/node',
    '@types/react',
    '@types/react-dom',
    '@types/electron-squirrel-startup',
    'electron',
    'typescript',
    'webpack'
  ]
};

// 可选但推荐的依赖
const OPTIONAL_DEPENDENCIES = {
  dependencies: [
    'form-data',
    'ws'
  ],
  devDependencies: [
    '@types/react-redux',
    'clean-webpack-plugin',
    'concurrently',
    'cross-env',
    'css-loader',
    'electron-builder',
    'express',
    'html-webpack-plugin',
    'nodemon',
    'style-loader',
    'ts-loader',
    'webpack-cli',
    'webpack-dev-server'
  ]
};

function getCurrentDependencies() {
  try {
    const packageJson = JSON.parse(fs.readFileSync('./package.json', 'utf8'));
    return {
      dependencies: Object.keys(packageJson.dependencies || {}),
      devDependencies: Object.keys(packageJson.devDependencies || {})
    };
  } catch (error) {
    console.error('无法读取 package.json:', error.message);
    return { dependencies: [], devDependencies: [] };
  }
}

function installMissingDependencies(missingDeps, depType) {
  if (missingDeps.length === 0) {
    console.log(`✅ 所有${depType}依赖都已安装`);
    return;
  }

  console.log(`❌ 缺少${depType}依赖:`, missingDeps);
  const installCmd = `npm install ${depType === 'devDependencies' ? '--save-dev ' : ''}${missingDeps.join(' ')}`;
  
  try {
    console.log(`正在安装: ${installCmd}`);
    execSync(installCmd, { stdio: 'inherit' });
    console.log(`✅ ${depType}依赖安装完成`);
  } catch (error) {
    console.error(`❌ 安装${depType}依赖失败:`, error.message);
  }
}

function checkAndInstallDependencies() {
  console.log('🔍 检查项目依赖...\n');
  
  const currentDeps = getCurrentDependencies();
  const missingCoreDeps = {};
  const missingOptionalDeps = {};

  // 检查核心依赖
  missingCoreDeps.dependencies = CORE_DEPENDENCIES.dependencies.filter(
    dep => !currentDeps.dependencies.includes(dep)
  );
  
  missingCoreDeps.devDependencies = CORE_DEPENDENCIES.devDependencies.filter(
    dep => !currentDeps.devDependencies.includes(dep)
  );

  // 检查可选依赖
  missingOptionalDeps.dependencies = OPTIONAL_DEPENDENCIES.dependencies.filter(
    dep => !currentDeps.dependencies.includes(dep)
  );
  
  missingOptionalDeps.devDependencies = OPTIONAL_DEPENDENCIES.devDependencies.filter(
    dep => !currentDeps.devDependencies.includes(dep)
  );

  // 安装缺失的核心依赖
  installMissingDependencies(missingCoreDeps.dependencies, 'dependencies');
  installMissingDependencies(missingCoreDeps.devDependencies, 'devDependencies');

  // 提示安装可选依赖
  if (missingOptionalDeps.dependencies.length > 0 || missingOptionalDeps.devDependencies.length > 0) {
    console.log('\n💡 发现可选依赖未安装:');
    if (missingOptionalDeps.dependencies.length > 0) {
      console.log('  依赖包:', missingOptionalDeps.dependencies.join(', '));
    }
    if (missingOptionalDeps.devDependencies.length > 0) {
      console.log('  开发依赖:', missingOptionalDeps.devDependencies.join(', '));
    }
    
    const readline = require('readline');
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });

    rl.question('\n是否要安装这些可选依赖? (y/N): ', (answer) => {
      if (answer.toLowerCase() === 'y') {
        installMissingDependencies(missingOptionalDeps.dependencies, 'dependencies');
        installMissingDependencies(missingOptionalDeps.devDependencies, 'devDependencies');
      }
      rl.close();
    });
  } else {
    console.log('✅ 所有推荐依赖都已安装');
  }
}

// 添加到 package.json scripts
function updatePackageScripts() {
  try {
    const packageJsonPath = './package.json';
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    
    if (!packageJson.scripts) {
      packageJson.scripts = {};
    }
    
    // 添加依赖检查脚本
    packageJson.scripts['check-deps'] = 'node scripts/install-deps.js';
    packageJson.scripts['install-all'] = 'npm run check-deps';
    
    fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2));
    console.log('✅ 已添加依赖检查脚本到 package.json');
  } catch (error) {
    console.error('❌ 更新 package.json 失败:', error.message);
  }
}

// 主执行函数
function main() {
  console.log('🚀 Electron IM 项目依赖检查工具\n');
  
  // 确保 scripts 目录存在
  if (!fs.existsSync('./scripts')) {
    fs.mkdirSync('./scripts', { recursive: true });
  }
  
  // 更新 package.json 脚本
  updatePackageScripts();
  
  // 检查并安装依赖
  checkAndInstallDependencies();
}

main();