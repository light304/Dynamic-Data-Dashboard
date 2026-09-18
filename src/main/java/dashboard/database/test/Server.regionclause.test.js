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
const { regionClause } = testHelpers;

// ---------------------------------------------------------------------

test('regionClause: "All Regions" returns an empty clause and leaves params untouched', () => {
    const params = ['2023-01-01', '2023-12-31'];
    const clause = regionClause('s', 'All Regions', params);

    assert.equal(clause, '');
    assert.deepEqual(params, ['2023-01-01', '2023-12-31']);
});

test('regionClause: a falsy region (undefined) returns an empty clause and leaves params untouched', () => {
    const params = [];
    const clause = regionClause('s', undefined, params);

    assert.equal(clause, '');
    assert.deepEqual(params, []);
});

test('regionClause: an empty-string region also returns an empty clause', () => {
    const params = [];
    const clause = regionClause('s', '', params);

    assert.equal(clause, '');
    assert.deepEqual(params, []);
});

test('regionClause: a real region builds an AND clause using the given alias', () => {
    const params = [];
    const clause = regionClause('s', 'Auckland', params);

    assert.equal(clause, ' AND s.region = ? ');
    assert.deepEqual(params, ['Auckland']);
});

test('regionClause: appends to existing params rather than replacing them', () => {
    const params = ['2023-01-01', '2023-12-31'];
    regionClause('s', 'Wellington', params);

    assert.deepEqual(params, ['2023-01-01', '2023-12-31', 'Wellington']);
});

test('regionClause: uses whatever alias it is given, not a hardcoded one', () => {
    // Several call sites in server.js pass different table aliases
    // ("s" for sales, "c" for customers, etc.) - this locks in that the
    // alias is actually substituted, not just always "s".
    const params = [];
    const clause = regionClause('c', 'Sydney', params);

    assert.equal(clause, ' AND c.region = ? ');
});

test('regionClause: mutates the same array instance passed in, rather than returning a new one', () => {
    const params = [];
    const returned = regionClause('s', 'Melbourne', params);

    // regionClause's return value is the SQL clause string, not the
    // params array - the array is updated as a side effect via .push().
    // Worth locking in explicitly, since every call site relies on
    // exactly this behavior (they never use a return value for params).
    assert.equal(params.length, 1);
    assert.notEqual(typeof returned, 'object');
});