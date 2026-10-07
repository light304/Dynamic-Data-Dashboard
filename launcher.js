#!/usr/bin/env node
'use strict';

/*
 * Dynamic Retail Dashboard - shared launcher (macOS / Windows / Linux)
 *
 * You normally don't run this directly; the three wrapper files do it for you:
 *   Launch_Dashboard_Mac.command
 *   Launch_Dashboard_Windows.bat
 *   Launch_Dashboard_Linux.sh
 *
 * Requires on the machine: Node.js, Maven (mvn) and JDK 21.
 */

const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const net = require('net');
const path = require('path');
const readline = require('readline');

const PROJECT_DIR = __dirname;
const IS_WIN = process.platform === 'win32';
const IS_MAC = process.platform === 'darwin';

const PORT = 3000;
const REQUIRED_JAVA = 21;
const MAIN_CLASS = 'dashboard.Main';

const BACKEND_SCRIPT = path.join(
    PROJECT_DIR, 'src', 'main', 'java', 'dashboard', 'database', 'server.js');

const BACKEND_DIR = path.dirname(BACKEND_SCRIPT);

// The classpath file is per-OS because Maven writes OS-specific separators
// (":" vs ";"), so a project folder shared between machines still works.
const CLASSPATH_NAME = `classpath-${process.platform}.txt`;
const CLASSPATH_REL = `target/${CLASSPATH_NAME}`;
const CLASSPATH_FILE = path.join(PROJECT_DIR, 'target', CLASSPATH_NAME);

let backend = null;
let javaHome = null;

// ------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------

const log = (msg = '') => console.log(msg);
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function pause(message = 'Press Enter to close...') {
    return new Promise((resolve) => {
        if (!process.stdin.isTTY) return resolve();
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout,
        });
        rl.question(message, () => {
            rl.close();
            resolve();
        });
    });
}

function stopBackend() {
    if (backend && backend.exitCode === null && !backend.killed) {
        backend.kill();
    }
}

async function fail(message) {
    log();
    log('ERROR: ' + message);
    log();
    stopBackend();
    await pause();
    process.exit(1);
}

// Safety net: never leave the backend running if we exit for any reason.
process.on('exit', stopBackend);
process.on('SIGINT', () => process.exit(0));
process.on('SIGTERM', () => process.exit(0));

// ------------------------------------------------------------------
// Java
// ------------------------------------------------------------------

function javaBinary(home) {
    return path.join(home, 'bin', IS_WIN ? 'java.exe' : 'java');
}

/**
 * Finds a JDK home: JAVA_HOME if valid, otherwise (macOS only) the installed
 * JDK 21. Returns null to fall back to whatever `java` is on the PATH.
 */
function findJavaHome() {
    const fromEnv = process.env.JAVA_HOME;
    if (fromEnv && fs.existsSync(javaBinary(fromEnv))) return fromEnv;

    if (IS_MAC) {
        const r = spawnSync(
            '/usr/libexec/java_home', ['-v', String(REQUIRED_JAVA)],
            { encoding: 'utf8' });
        if (r.status === 0 && r.stdout.trim()) return r.stdout.trim();
    }
    return null;
}

async function checkJava(java) {
    const r = spawnSync(java, ['-version'], { encoding: 'utf8' });

    if (r.error) {
        await fail(
            'Java was not found.\n' +
            `Install JDK ${REQUIRED_JAVA} and make sure "java" is on your PATH ` +
            '(or set JAVA_HOME).');
    }

    const match = /version "(\d+)/.exec((r.stderr || '') + (r.stdout || ''));
    const major = match ? Number(match[1]) : null;

    if (major !== null && major < REQUIRED_JAVA) {
        await fail(
            `Java ${major} was found, but the dashboard needs Java ${REQUIRED_JAVA} or newer.\n` +
            `Install JDK ${REQUIRED_JAVA} or point JAVA_HOME at it.`);
    }
}

// ------------------------------------------------------------------
// Port handling
// ------------------------------------------------------------------

/** PIDs of processes listening on the given TCP port. */
function pidsOnPort(port) {
    const pids = new Set();

    if (IS_WIN) {
        const r = spawnSync('netstat', ['-ano'], { encoding: 'utf8' });
        for (const line of (r.stdout || '').split(/\r?\n/)) {
            // Proto  Local  Foreign  State  PID
            const p = line.trim().split(/\s+/);
            if (p[0] !== 'TCP' || p.length < 5) continue;
            const isListening = p[2].endsWith(':0');
            if (isListening && p[1].endsWith(':' + port)) pids.add(p[4]);
        }
    } else {
        const r = spawnSync('lsof', [`-tiTCP:${port}`, '-sTCP:LISTEN'],
            { encoding: 'utf8' });
        if (!r.error) {
            r.stdout.split(/\s+/).filter(Boolean).forEach((x) => pids.add(x));
        } else {
            // lsof not installed (some Linux distros): try fuser instead.
            const f = spawnSync('fuser', [`${port}/tcp`], { encoding: 'utf8' });
            if (!f.error) {
                (f.stdout || '').split(/\s+/)
                    .filter((x) => /^\d+$/.test(x))
                    .forEach((x) => pids.add(x));
            }
        }
    }

    pids.delete('0');
    pids.delete(String(process.pid));
    return [...pids];
}

async function stopOldBackend() {
    const pids = pidsOnPort(PORT);
    if (pids.length === 0) return;

    log('Stopping old backend...');
    for (const pid of pids) {
        if (IS_WIN) {
            spawnSync('taskkill', ['/PID', pid, '/F']);
        } else {
            try { process.kill(Number(pid)); } catch (e) { /* already gone */ }
        }
    }
    await delay(300);
}

function canConnect(port) {
    return new Promise((resolve) => {
        const socket = net.connect({ port, host: 'localhost' });
        socket.setTimeout(500, () => { socket.destroy(); resolve(false); });
        socket.once('connect', () => { socket.destroy(); resolve(true); });
        socket.once('error', () => resolve(false));
    });
}

/*
 * The repository tracks a handful of stray files under node_modules, so
 * the folder itself exists in a fresh clone even though nothing is
 * actually installed. Checking for the packages is the only reliable test.
 */
function backendDependenciesMissing() {
    return ['express', 'better-sqlite3', 'csv-parse'].some(
        (pkg) => !fs.existsSync(
            path.join(BACKEND_DIR, 'node_modules', pkg, 'package.json')));
}

async function installBackendDependencies() {
    if (!backendDependenciesMissing()) return;

    log('Installing backend dependencies (first run only)...');
    log('This can take a couple of minutes.');
    log();

    const result = spawnSync(
        IS_WIN ? 'cmd.exe' : 'npm',
        IS_WIN ? ['/c', 'npm', 'install'] : ['install'],
        {
            cwd: BACKEND_DIR,
            stdio: 'inherit',
        });

    if (result.error || result.status !== 0) {
        await fail(
            'Could not install the backend dependencies.\n\n' +
            'Run this manually, then try again:\n\n' +
            '    cd ' + BACKEND_DIR + '\n' +
            '    npm install');
    }

    log('Dependencies installed.');
    log();
}

// ------------------------------------------------------------------
// Backend
// ------------------------------------------------------------------

async function startBackend() {
    log('Starting backend...');

    backend = spawn(process.execPath, [BACKEND_SCRIPT], {
        cwd: PROJECT_DIR,
        stdio: 'inherit',
    });

    let exited = false;
    backend.on('exit', () => { exited = true; });
    backend.on('error', () => { exited = true; });

    const deadline = Date.now() + 15000;

    while (Date.now() < deadline && !exited) {
        if (await canConnect(PORT)) {
            log('Backend ready.');
            return;
        }
        await delay(250);
    }

    await fail(
        'Backend failed to start.\n' +
        'Scroll up for the error printed by server.js.');
}

// ------------------------------------------------------------------
// Maven
// ------------------------------------------------------------------

/*
 * Prefer the Maven Wrapper committed to the repository, which downloads
 * the right Maven version on first use. Falls back to a system Maven if
 * the wrapper is not present.
 */
function mavenCommand() {
    const wrapper = path.join(PROJECT_DIR, IS_WIN ? 'mvnw.cmd' : 'mvnw');

    if (!fs.existsSync(wrapper)) return 'mvn';

    if (!IS_WIN) {
        // A zipped project can lose the executable bit.
        try { fs.chmodSync(wrapper, 0o755); } catch { /* ignore */ }
    }

    return wrapper;
}

function runMaven(args) {
    const env = { ...process.env };
    if (javaHome) env.JAVA_HOME = javaHome;

    const command = mavenCommand();

    /*
     * Node will not spawn a .cmd or .bat file directly, so on Windows the
     * wrapper runs through cmd.exe. Passing the arguments as a list rather
     * than setting shell:true keeps them escaped and avoids DEP0190.
     */
    const file = IS_WIN ? 'cmd.exe' : command;
    const fullArgs = IS_WIN ? ['/c', command, ...args] : args;

    return spawnSync(file, fullArgs, {
        cwd: PROJECT_DIR,
        stdio: 'inherit',
        env,
    });
}

function classpathIsStale() {
    if (!fs.existsSync(CLASSPATH_FILE)) return true;

    // Rebuild the dependency list if pom.xml changed since it was made.
    const pom = path.join(PROJECT_DIR, 'pom.xml');
    return fs.existsSync(pom) &&
        fs.statSync(pom).mtimeMs > fs.statSync(CLASSPATH_FILE).mtimeMs;
}

async function compile() {
    log();

    let args;

    if (classpathIsStale()) {
        log('Preparing dashboard for first launch...');
        args = ['-q', 'compile', 'dependency:build-classpath',
            `-Dmdep.outputFile=${CLASSPATH_REL}`];
    } else {
        log('Checking for code changes...');
        args = ['-q', 'compile'];
    }

    const result = runMaven(args);

    if (result.error || result.status !== 0) {
        await fail(
            'Java compilation failed.\n' +
            'If nothing above explains why, check that Java 21 is installed ' +
            'and that the project folder is complete - mvnw, mvnw.cmd and ' +
            '.mvn must all be present.' +
            (result.error ? '\n\n' + result.error.message : ''));
    }
}

// ------------------------------------------------------------------
// Dashboard
// ------------------------------------------------------------------

function startDashboard(java) {
    const deps = fs.readFileSync(CLASSPATH_FILE, 'utf8').trim();
    const classpath = [path.join(PROJECT_DIR, 'target', 'classes'), deps]
        .join(path.delimiter);

    log('Starting dashboard...');
    log();

    const dashboard = spawn(java, ['-cp', classpath, MAIN_CLASS], {
        cwd: PROJECT_DIR,
        stdio: 'inherit',
    });

    dashboard.on('error', (err) => {
        fail('Could not start Java: ' + err.message);
    });

    dashboard.on('exit', async (code) => {
        log();
        log('Closing backend...');
        stopBackend();
        log('Dashboard closed.');

        if (code) {
            // Non-zero exit: keep the window open so the error can be read.
            await pause();
        }
        process.exit(code || 0);
    });
}

// ------------------------------------------------------------------
// Main
// ------------------------------------------------------------------

async function main() {
    log('======================================');
    log(' Dynamic Retail Dashboard');
    log('======================================');
    log();

    javaHome = findJavaHome();
    const java = javaHome ? javaBinary(javaHome) : 'java';
    await checkJava(java);

    await stopOldBackend();
    await installBackendDependencies();
    await startBackend();
    await compile();
    startDashboard(java);
}

main().catch((err) => fail(err && err.message ? err.message : String(err)));
