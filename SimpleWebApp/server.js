const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const PORT = 3000;

const server = http.createServer((req, res) => {
    // Handle POST requests to /api/track
    if (req.method === 'POST' && req.url === '/api/track') {
        let body = '';
        
        req.on('data', chunk => {
            body += chunk.toString();
        });
        
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const inputText = data.text || '';
                
                // Call Kotlin TrackingClient with the input
                callTrackingClient(inputText, (result) => {
                    res.writeHead(200, { 
                        'Content-Type': 'application/json',
                        'Access-Control-Allow-Origin': '*',
                        'Access-Control-Allow-Methods': 'POST',
                        'Access-Control-Allow-Headers': 'Content-Type'
                    });
                    res.end(JSON.stringify({ message: result }));
                });
                
            } catch (error) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Invalid JSON' }));
            }
        });
        return;
    }
    
    // Handle OPTIONS requests for CORS
    if (req.method === 'OPTIONS') {
        res.writeHead(200, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type'
        });
        res.end();
        return;
    }
    
    // Serve the HTML file for GET requests
    const filePath = path.join(__dirname, 'index.html');
    
    fs.readFile(filePath, 'utf8', (err, data) => {
        if (err) {
            res.writeHead(500, { 'Content-Type': 'text/plain' });
            res.end('Server Error');
            return;
        }
        
        res.writeHead(200, { 'Content-Type': 'text/html' });
        res.end(data);
    });
});

/**
 * Call the Kotlin TrackingClient with the input text
 */
function callTrackingClient(inputText, callback) {
    console.log(`Processing input: ${inputText}`);
    
    // Path to the Kotlin project (adjust this path as needed)
    const kotlinProjectPath = path.join(__dirname, '..', 'ShipmentTracker');
    
    // Call the Kotlin application using gradlew
    const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
    const kotlinProcess = spawn(gradlew, ['run', '--args', `"${inputText}"`], {
        cwd: kotlinProjectPath,
        shell: true
    });
    
    let output = '';
    let errorOutput = '';
    
    kotlinProcess.stdout.on('data', (data) => {
        output += data.toString();
    });
    
    kotlinProcess.stderr.on('data', (data) => {
        errorOutput += data.toString();
    });
    
    kotlinProcess.on('close', (code) => {
        if (code === 0) {
            // Parse the output from Kotlin
            const lines = output.split('\n');
            const resultLine = lines.find(line => line.startsWith('SUCCESS:') || line.startsWith('ERROR:'));
            
            if (resultLine) {
                if (resultLine.startsWith('SUCCESS:')) {
                    callback(resultLine.substring(8).trim()); // Remove "SUCCESS: " prefix
                } else {
                    callback(resultLine.substring(6).trim()); // Remove "ERROR: " prefix
                }
            } else {
                callback('Tracking request processed (no specific result)');
            }
        } else {
            console.error('Kotlin process error:', errorOutput);
            callback(`Error: Failed to process request (exit code: ${code})`);
        }
    });
    
    kotlinProcess.on('error', (error) => {
        console.error('Failed to start Kotlin process:', error);
        callback(`Error: Could not start tracking client (${error.message})`);
    });
}

server.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}/`);
    console.log('Ready to receive tracking requests!');
    console.log('Press Ctrl+C to stop the server');
});

// Handle graceful shutdown
process.on('SIGINT', () => {
    console.log('\nShutting down server...');
    server.close(() => {
        console.log('Server closed.');
        process.exit(0);
    });
}); 