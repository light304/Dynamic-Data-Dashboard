'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

// Same temp boilerplate as the other test files in this folder - see
// server.dateFormatting.test.js for a full explanation

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
const { previousRange } = testHelpers;

// ---------------------------------------------------------------------

test('previousRange: a full 31-day month rolls back cleanly across a year boundary', () => {
    const previous = previousRange({ start: '2023-01-01', end: '2023-01-31' });
    assert.deepEqual(previous, { start: '2022-12-01', end: '2022-12-31' });
});



// The window length is fixed at whatever the current range's duration
// is - here, 30 days. It does NOT snap to "the previous calendar
// month" or otherwise stretch/shrink to fit May's actual length.
// May 2 -> May 31 inclusive is exactly 30 days.

test('previousRange: a 30-day window stays exactly 30 days, even though May has 31', () => {

    const previous = previousRange({ start: '2023-06-01', end: '2023-06-30' });
    assert.deepEqual(previous, { start: '2023-05-02', end: '2023-05-31' });
});

test('previousRange: a single-day range has a single-day previous period', () => {
    const previous = previousRange({ start: '2023-03-15', end: '2023-03-15' });
    assert.deepEqual(previous, { start: '2023-03-14', end: '2023-03-14' });
});



// 2024-03-01 to 2024-03-31 is 31 days. The day before March 1st is
// Feb 29th (2024 is a leap year), and counting 31 days back from
// there lands on Jan 30th - verified independently via `date -d`,
// not derived from this function itself.
test('previousRange: correctly counts a leap-year February when crossing back into it', () => {

    const previous = previousRange({ start: '2024-03-01', end: '2024-03-31' });
    assert.deepEqual(previous, { start: '2024-01-30', end: '2024-02-29' });
});
