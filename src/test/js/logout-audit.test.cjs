const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function setup(fetch) {
    const html = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/index.html'), 'utf8');
    const script = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)]
        .map(match => match[1]).find(source => source.includes('async function logout()'));
    const button = { disabled: false };
    const elements = { 'logout-overlay': { style: {} }, 'logout-modal': { style: {} } };
    const state = { removed: false, alerts: [] };
    const context = {
        ctx: '/hdcs', fetch,
        localStorage: { getItem() { return 'test-token'; }, removeItem() { state.removed = true; } },
        document: { querySelector() { return button; }, getElementById(id) { return elements[id]; } },
        alert(message) { state.alerts.push(message); }, window: { location: {} }
    };
    vm.createContext(context);
    vm.runInContext(script, context);
    return { context, button, elements, state };
}

test('logout sends authenticated audit before clearing the token and prevents duplicate clicks', async () => {
    let resolve;
    let requests = 0;
    const setupState = setup((url, options) => {
        requests++;
        assert.equal(url, '/hdcs/api/logout');
        assert.equal(options.headers.Authorization, 'Bearer test-token');
        return new Promise(done => { resolve = done; });
    });
    const pending = setupState.context.logout();
    await setupState.context.logout();
    assert.equal(requests, 1);
    assert.equal(setupState.state.removed, false);
    resolve({ ok: true });
    await pending;
    assert.equal(setupState.state.removed, true);
    assert.equal(setupState.elements['logout-modal'].style.display, 'block');
});

test('failed logout audit keeps the token and allows retry', async () => {
    const state = setup(async () => ({ ok: false }));
    await state.context.logout();
    assert.equal(state.state.removed, false);
    assert.equal(state.button.disabled, false);
    assert.equal(state.state.alerts.length, 1);
});
