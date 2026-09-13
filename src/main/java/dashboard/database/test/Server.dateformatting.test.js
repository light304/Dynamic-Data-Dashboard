'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

// For testing purposes, we don't care about the actual express() or Database()
// implementations - we just need something to exist and not throw an error. 
// Thus, by creating fakes which are enough to satisfy server.js's own calls 
// to those modules, we can now test the functions in server.js freely with light-weight fakes.

function stubModule(specifier, fakeExports) {
    const resolvedPath = require.resolve(specifier);
    require.cache[resolvedPath] = {
        id: resolvedPath,
        filename: resolvedPath,
        loaded: true,
        exports: fakeExports
    };
}

// fakeExpress() returns a temp "app" whose methods are safe no-ops.
// server.js calls .use(), .get(), .post(), and .listen() on this - none
// of that logic runs in this test file, so the fakes just need to exist
// and not error.

function fakeExpress() {
    const app = {
        use: () => app,
        get: () => app,
        post: () => app,
        listen: (_port, callback) => {
            if (typeof callback === 'function') callback();
            return { close: () => {} };
        }
    };
    return app;
}
fakeExpress.json = () => (req, res, next) => { if (next) next(); };

stubModule('express', fakeExpress);

// FakeDatabase creates a temp db, as `new Database(dbPath)` must not fail. 
// None of the functions tested in this file ever touch the db, so .prepare()
// just needs to exist.

function FakeDatabase() {
    return {
        prepare: () => ({
            all: () => [],
                        get: () => undefined,
                        run: () => ({})
        }),
        exec: () => {}
    };
}
stubModule('better-sqlite3', FakeDatabase);

// Now its safe to require the real server.js - express() / Database()
// calls inside it resolve to the fakes above. 
// And since no real port is bound, no real database file is ever touched.

const { testHelpers } = require('../server.js');
const { pad2, isoDate, daysInMonth } = testHelpers;



// pad2 tests

test('pad2: zero-pads a single digit', () => {
    assert.equal(pad2(5), '05');
});

test('pad2: leaves two digits unchanged', () => {
    assert.equal(pad2(12), '12');
});

test('pad2: zero pads to "00"', () => {
    assert.equal(pad2(0), '00');
});



// isoDate tests

test('isoDate: pads both month and day when needed', () => {
    assert.equal(isoDate(2023, 1, 5), '2023-01-05');
});

test('isoDate: leaves already-two-digit month/day alone', () => {
    assert.equal(isoDate(2023, 12, 31), '2023-12-31');
});



// daysInMonth tests

test('daysInMonth: February in a non-leap year has 28 days', () => {
    assert.equal(daysInMonth(2023, 2), 28);
});

test('daysInMonth: February in a leap year has 29 days', () => {
    assert.equal(daysInMonth(2024, 2), 29);
});

test('daysInMonth: April is a 30-day month', () => {
    assert.equal(daysInMonth(2023, 4), 30);
});

test('daysInMonth: January is a 31-day month', () => {
    assert.equal(daysInMonth(2023, 1), 31);
});