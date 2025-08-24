#!/usr/bin/env node

const { spawn, exec } = require('child_process');
const path = require('path');

// ANSI color codes for better output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m'
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function logError(message) {
  console.error(`${colors.red}${message}${colors.reset}`);
}

function logSuccess(message) {
  console.log(`${colors.green}${message}${colors.reset}`);
}

function logInfo(message) {
  console.log(`${colors.blue}${message}${colors.reset}`);
}

function logWarning(message) {
  console.log(`${colors.yellow}${message}${colors.reset}`);
}

// Ensure Docker services are torn down
async function ensureDockerDown() {
  return new Promise((resolve) => {
    logInfo('🧹 Cleaning up Docker services...');
    const dockerDown = spawn('docker-compose', ['-f', 'docker-compose-test.yml', 'down'], {
      stdio: 'inherit',
      shell: true
    });
    
    dockerDown.on('close', (code) => {
      if (code === 0) {
        logSuccess('✅ Docker services cleaned up successfully');
      } else {
        logWarning(`⚠️  Docker cleanup exited with code ${code}`);
      }
      resolve();
    });
    
    dockerDown.on('error', (err) => {
      logError(`❌ Failed to run docker-compose down: ${err.message}`);
      resolve();
    });
  });
}

// Start Docker services
async function startDockerServices() {
  return new Promise((resolve, reject) => {
    logInfo('🚀 Starting Docker services...');
    const dockerUp = spawn('docker-compose', ['-f', 'docker-compose-test.yml', 'up', '-d'], {
      stdio: 'inherit',
      shell: true
    });
    
    dockerUp.on('close', (code) => {
      if (code === 0) {
        logSuccess('✅ Docker services started successfully');
        resolve();
      } else {
        reject(new Error(`Docker services failed to start (exit code: ${code})`));
      }
    });
    
    dockerUp.on('error', (err) => {
      reject(new Error(`Failed to start Docker services: ${err.message}`));
    });
  });
}

// Wait for services to be ready
async function waitForServices() {
  return new Promise((resolve) => {
    logInfo('⏳ Waiting for services to be ready...');
    
    // Wait for Schema Registry to be ready
    const checkSchemaRegistry = () => {
      exec('curl --fail --silent --show-error http://localhost:8081/subjects', (error) => {
        if (error) {
          logInfo('⏳ Schema Registry not ready yet, waiting...');
          setTimeout(checkSchemaRegistry, 2000);
        } else {
          logSuccess('✅ Schema Registry is ready');
          resolve();
        }
      });
    };
    
    // Start checking after a brief delay
    setTimeout(checkSchemaRegistry, 5000);
  });
}

// Run Playwright tests
async function runTests() {
  return new Promise((resolve, reject) => {
    logInfo('🧪 Running Playwright tests...');
    const testProcess = spawn('npx', ['playwright', 'test'], {
      stdio: 'inherit',
      shell: true
    });
    
    testProcess.on('close', (code) => {
      if (code === 0) {
        logSuccess('✅ Tests completed successfully');
        resolve();
      } else {
        reject(new Error(`Tests failed with exit code: ${code}`));
      }
    });
    
    testProcess.on('error', (err) => {
      reject(new Error(`Failed to run tests: ${err.message}`));
    });
  });
}

// Main execution function
async function main() {
  let dockerStarted = false;
  
  try {
    // Set up cleanup handlers
    const cleanup = async () => {
      if (dockerStarted) {
        await ensureDockerDown();
      }
    };
    
    // Handle various exit scenarios
    process.on('SIGINT', async () => {
      logWarning('\n⚠️  Received SIGINT, cleaning up...');
      await cleanup();
      process.exit(1);
    });
    
    process.on('SIGTERM', async () => {
      logWarning('\n⚠️  Received SIGTERM, cleaning up...');
      await cleanup();
      process.exit(1);
    });
    
    process.on('uncaughtException', async (err) => {
      logError(`\n❌ Uncaught exception: ${err.message}`);
      await cleanup();
      process.exit(1);
    });
    
    process.on('unhandledRejection', async (reason, promise) => {
      logError(`\n❌ Unhandled rejection at: ${promise}, reason: ${reason}`);
      await cleanup();
      process.exit(1);
    });
    
    // Start Docker services
    await startDockerServices();
    dockerStarted = true;
    
    // Wait for services to be ready
    await waitForServices();
    
    // Run tests
    await runTests();
    
    logSuccess('\n🎉 All tests completed successfully!');
    
  } catch (error) {
    logError(`\n❌ Error: ${error.message}`);
    process.exitCode = 1;
  } finally {
    // Always ensure cleanup
    if (dockerStarted) {
      await ensureDockerDown();
    }
  }
}

// Run the main function
if (require.main === module) {
  main().catch((error) => {
    logError(`\n💥 Fatal error: ${error.message}`);
    process.exit(1);
  });
}

module.exports = { main, startDockerServices, runTests, ensureDockerDown };
