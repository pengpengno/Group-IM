const fs = require('fs');
const path = require('path');

const sourceDir = path.resolve(__dirname, '../../../common/src/main/proto');
const targetDir = path.resolve(__dirname, '../protos');

console.log(`Syncing protos from ${sourceDir} to ${targetDir}...`);

// Ensure target directory exists
if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
}

// Read all files in source directory
try {
    const files = fs.readdirSync(sourceDir);
    const protoFiles = files.filter(file => file.endsWith('.proto'));

    protoFiles.forEach(file => {
        const sourceFile = path.join(sourceDir, file);
        const targetFile = path.join(targetDir, file);

        // Copy file
        fs.copyFileSync(sourceFile, targetFile);
        console.log(`- Copied ${file}`);
    });

    console.log('Protos synced successfully!');
} catch (error) {
    console.error('Error syncing protos:', error.message);
    process.exit(1);
}
