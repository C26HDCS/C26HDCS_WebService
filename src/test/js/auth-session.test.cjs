const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function jwt(exp) {
    return 'header.' + Buffer.from(JSON.stringify({ exp })).toString('base64url') + '.signature';
}

function harness(initialToken = jwt(2000)) {
    const factories = {}, configs = [], runs = [], controllers = {};
    const moves = [], listeners = {}, ticks = [];
    let token = initialToken, now = 1000000;
    const window = {
        atob: value => Buffer.from(value, 'base64').toString('binary'),
        localStorage: { getItem: () => token, removeItem: () => { token = null; } },
        location: { replace: url => moves.push(url) },
        addEventListener: (name, callback) => { listeners[name] = callback; }
    };
    const app = {
        config: deps => configs.push(deps),
        factory: (name, deps) => { factories[name] = deps; },
        run: deps => runs.push(deps),
        controller: (name, deps) => { controllers[name] = deps; }
    };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/app.js'), 'utf8'), {
        angular: { module: () => app }, ctx: '/hdcs', Date: { now: () => now }, console
    });
    const session = factories.AuthSession.at(-1)(window);
    const http = { defaults: { headers: { common: {} } }, interceptors: [] };
    configs[1].at(-1)(http);
    const interceptor = http.interceptors[0].at(-1)({ reject: value => ({ rejected: value }) }, session);
    return { session, interceptor, moves, listeners, controllers,
        token: () => token, setToken: value => { token = value; },
        setTime: value => { now = value; },
        start() { runs[0].at(-1)(session, (callback, delay) => ticks.push({ callback, delay }), window); }, ticks };
}

test('valid token is retained and attached to requests', () => {
    const h = harness();
    h.start();
    const config = h.interceptor.request({ headers: {} });
    assert.equal(config.headers.Authorization, 'Bearer ' + jwt(2000));
    assert.equal(h.moves.length, 0);
});

for (const token of [null, 'malformed', jwt(999), jwt('2000'), jwt(null)]) {
    test('missing, malformed or expired token redirects once: ' + token, () => {
        const h = harness(token);
        h.start();
        h.session.check();
        h.session.rejected(401);
        assert.deepEqual(h.moves, ['/hdcs/login']);
        assert.equal(h.token(), null);
        assert.equal(h.interceptor.request({ headers: {} }).rejected.status, 401);
    });
}

test('idle session redirects when expiry is reached without an API request', () => {
    const h = harness();
    h.start();
    assert.equal(h.ticks[0].delay, 1000);
    h.setTime(2000000);
    h.ticks[0].callback();
    assert.deepEqual(h.moves, ['/hdcs/login']);
});

test('server authentication rejection redirects, permission denial and outages do not', () => {
    const h = harness();
    for (const status of [403, 500, 503, -1]) h.interceptor.responseError({ status });
    assert.equal(h.moves.length, 0);
    h.interceptor.responseError({ status: 401 });
    assert.deepEqual(h.moves, ['/hdcs/login']);
});

test('legacy 403 for expired tokens also redirects', () => {
    const h = harness(jwt(999));
    h.interceptor.responseError({ status: 403 });
    assert.deepEqual(h.moves, ['/hdcs/login']);
});

test('another tab removing the token or waking a sleeping tab triggers a check', () => {
    const h = harness();
    h.start();
    h.listeners.storage({ key: 'unrelated' });
    assert.equal(h.moves.length, 0);
    h.setToken(null);
    h.listeners.storage({ key: 'accessToken' });
    h.listeners.focus();
    assert.deepEqual(h.moves, ['/hdcs/login']);
});
