const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function harness(get) {
    const timers = [];
    const cancelled = [];
    let destroy;
    const scope = { $on(name, fn) { if (name === '$destroy') destroy = fn; } };
    const interval = (fn, ms) => { const timer = {fn, ms}; timers.push(timer); return timer; };
    interval.cancel = timer => cancelled.push(timer);
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,
        '../../main/resources/static/js/dashboard/dashboard.controller.js'), 'utf8'), {
        qosApp: { controller(name, deps) { deps.at(-1)(scope, {get}, interval); } },
        ctx: '/hdcs',
        Cesium: { Ion: {}, CesiumTerrainProvider: { fromIonAssetId: () => new Promise(() => {}) } }
    });
    return { scope, poll: () => timers.find(t => t.ms === 5000).fn(), destroy: () => destroy(), cancelled };
}
const flush = () => new Promise(resolve => setImmediate(resolve));
const data = { summary: {totalDevices: 2, normal: 0, warn: 1, fault: 1}, checkedAt: '2026-10-23T01:00:00Z', devices: [
    {deviceId:'75', pipelineComponents:[{dataKind:'DCS',status:'WARNING'}]},
    {deviceId:'86', pipelineComponents:[{dataKind:'KA',status:'ERROR'}]}
] };

test('polls live snapshot and retains last real state on failure, then recovers', async () => {
    let fail = false;
    const h = harness(url => { assert.equal(url, '/hdcs/api/dashboard/live'); return fail ? Promise.reject(new Error('offline')) : Promise.resolve({data}); });
    await flush();
    assert.equal(h.scope.bandStatus('DCS'), 'WARNING');
    assert.equal(h.scope.bandStatus('KA'), 'ERROR');
    assert.equal(h.scope.summary.totalDevices, 2);
    fail = true;
    h.poll(); await flush();
    assert.ok(h.scope.statusError);
    assert.equal(h.scope.summary.totalDevices, 2);
    fail = false;
    h.poll(); await flush();
    assert.equal(h.scope.statusError, '');
});

test('failed first query does not invent counts or healthy band state', async () => {
    const h = harness(() => Promise.reject(new Error('offline')));
    await flush();
    assert.equal(h.scope.statusLoaded, false);
    assert.equal(h.scope.bandStatus('DCS'), 'UNKNOWN');
    assert.ok(h.scope.statusError);
});

test('no overlapping polling and no state mutation after view destruction', async () => {
    let resolve, calls = 0;
    const h = harness(() => { calls++; return new Promise(r => { resolve = r; }); });
    h.poll(); h.poll();
    assert.equal(calls, 1);
    h.destroy();
    resolve({data}); await flush();
    assert.equal(h.scope.statusLoaded, false);
    assert.equal(h.cancelled.length, 2);
    h.poll();
    assert.equal(calls, 1);
});
