'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

// Same mock-express / mock-better-sqlite3 boilerplate idea as
// server.dateFormatting.test.js. Each test file runs in its own
// process under node:test, so this isn't shared automatically - it's
// duplicated here on purpose so all test files run completely seperately.

function stubModule(specifier, fakeExports) {
    const resolvedPath = require.resolve(specifier);
    require.cache[resolvedPath] = {
        id: resolvedPath,
        filename: resolvedPath,
        loaded: true,
        exports: fakeExports
    };
}

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

const { testHelpers } = require('../server.js');
const { dashboardRange } = testHelpers;

// Small helper so each test reads as "given these query params" rather
// than repeating the `{ query: {...} }` wrapper shape everywhere.

function req(query) {
    return { query };
}



// Yearly test

test('dashboardRange: Yearly - defaults to 2023 when the query is empty', () => {
    const range = dashboardRange(req({}));
    assert.deepEqual(range, { start: '2023-01-01', end: '2023-12-31' });
});

test('dashboardRange: Yearly - explicit year', () => {
    const range = dashboardRange(req({ year: '2024', scope: 'Yearly' }));
    assert.deepEqual(range, { start: '2024-01-01', end: '2024-12-31' });
});



// Quarterly test

test('dashboardRange: Quarterly - Q2 maps to April-June', () => {
    const range = dashboardRange(req({ year: '2023', scope: 'Quarterly', period: 'Q2' }));
    assert.deepEqual(range, { start: '2023-04-01', end: '2023-06-30' });
});

test('dashboardRange: Quarterly - Q4 maps to October-December', () => {
    const range = dashboardRange(req({ year: '2023', scope: 'Quarterly', period: 'Q4' }));
    assert.deepEqual(range, { start: '2023-10-01', end: '2023-12-31' });
});

test('dashboardRange: Quarterly - unparseable period falls back to Q1', () => {
    // period.replace(/[^1-4]/g, '') strips "abc" down to '', so
    // Number('') || 1 kicks in - this should behave exactly like "Q1".
    const range = dashboardRange(req({ year: '2023', scope: 'Quarterly', period: 'abc' }));
    assert.deepEqual(range, { start: '2023-01-01', end: '2023-03-31' });
});



// Monthly test

test('dashboardRange: Monthly - February in a leap year', () => {
    const range = dashboardRange(req({ year: '2024', scope: 'Monthly', period: 'February' }));
    assert.deepEqual(range, { start: '2024-02-01', end: '2024-02-29' });
});

test('dashboardRange: Monthly - unrecognized period falls back to January', () => {
    const range = dashboardRange(req({ year: '2023', scope: 'Monthly', period: 'NotAMonth' }));
    assert.deepEqual(range, { start: '2023-01-01', end: '2023-01-31' });
});



// Weekly test

test('dashboardRange: Weekly - a normal mid-month week', () => {
    const range = dashboardRange(
        req({ year: '2023', scope: 'Weekly', month: 'March', period: 'Week 2' })
    );
    assert.deepEqual(range, { start: '2023-03-08', end: '2023-03-14' });
});

test('dashboardRange: Weekly - final week of a 31-day month is clipped, not 7 days', () => {
    const range = dashboardRange(
        req({ year: '2023', scope: 'Weekly', month: 'January', period: 'Week 5' })
    );
    assert.deepEqual(range, { start: '2023-01-29', end: '2023-01-31' });
});

test('dashboardRange: Weekly - missing month falls back to January', () => {
    const range = dashboardRange(req({ year: '2023', scope: 'Weekly', period: 'Week 1' }));
    assert.deepEqual(range, { start: '2023-01-01', end: '2023-01-07' });
});



// Unrecognized scope test

test('dashboardRange: an unrecognized scope falls through to Yearly-shaped behavior', () => {
    const range = dashboardRange(req({ year: '2023', scope: 'Bogus' }));
    assert.deepEqual(range, { start: '2023-01-01', end: '2023-12-31' });
});
