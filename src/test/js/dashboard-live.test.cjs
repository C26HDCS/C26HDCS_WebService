const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function harness(get, cesium = {}, overlay = null) {
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
        document: { getElementById: () => overlay },
        Cesium: { Ion: {}, CesiumTerrainProvider: { fromIonAssetId: () => new Promise(() => {}) }, ...cesium }
    });
    return { scope, poll: () => timers.find(t => t.ms === 5000).fn(), destroy: () => destroy(), cancelled };
}
const flush = () => new Promise(resolve => setImmediate(resolve));
const data = { summary: {totalDevices: 2, normal: 0, warn: 1, fault: 1}, checkedAt: '2026-10-23T01:00:00Z', devices: [
    {deviceId:'75', pipelineComponents:[{dataKind:'DCS',status:'WARNING'}]},
    {deviceId:'86', pipelineComponents:[{dataKind:'KA',status:'ERROR'}]}
] };

test('the diagram tab closes and reopens only the system diagram', () => {
    const { scope } = harness(() => Promise.resolve({data}));
    assert.equal(scope.diagramCollapsed, false);
    scope.toggleEquipPanel();
    scope.toggleDiagram();
    assert.equal(scope.diagramCollapsed, true);
    assert.equal(scope.equipPanelCollapsed, false);
    scope.toggleDiagram();
    assert.equal(scope.diagramCollapsed, false);
});

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

test('a selected watershed filters only the list and stays selected on live refresh', async () => {
    const devices = [{deviceId: '1', groupName: '영산강'}, {deviceId: '2', groupName: '섬진강'}];
    let snapshot = devices;
    const h = harness(() => Promise.resolve({data: {...data, devices: snapshot}}));
    await flush();
    h.scope.deviceListFilter.groupName = '영산강';
    h.scope.filterDeviceList();
    assert.equal(h.scope.listDevices.length, 1);
    assert.equal(h.scope.devices.length, 2);

    snapshot = [{...devices[0], latestData: 'updated'}, devices[1]];
    h.poll();
    await flush();
    assert.equal(h.scope.listDevices.length, 1);
    assert.equal(h.scope.listDevices[0].latestData, 'updated');
    assert.equal(h.scope.devices.length, 2);
});

test('live device coordinates drive markers and the worst co-located status updates in place', async () => {
    const markers = new Set();
    const cameraTargets = [];
    let renderSelection;
    const overlay = {
        hidden: true,
        style: { setProperty(name, value) { this[name] = value; } }
    };
    const scene = {
        globe: {getHeight: () => 20},
        canvas: {clientWidth: 800, clientHeight: 600},
        postRender: {addEventListener(fn) { renderSelection = fn; }}
    };
    const cesium = {
        CesiumTerrainProvider: { fromIonAssetId: () => Promise.resolve({}) },
        Viewer: function () {
            this.scene = scene;
            this.imageryLayers = { addImageryProvider() {} };
            this.camera = {
                setView() {},
                positionCartographic: {height: 50000},
                flyTo({destination}) { cameraTargets.push(destination); }
            };
            this.entities = {
                add(marker) { markers.add(marker); return marker; },
                remove(marker) { markers.delete(marker); }
            };
        },
        ImageryLayer: class {},
        WebMapTileServiceImageryProvider: class {},
        Credit: class {},
        Cartesian2: class {},
        Cartesian3: {
            fromDegrees: (...coordinates) => coordinates,
            fromRadians: (...coordinates) => coordinates
        },
        Cartographic: {fromDegrees: (longitude, latitude) => ({longitude, latitude})},
        SceneTransforms: {wgs84ToWindowCoordinates: (_, [longitude, latitude, height]) => {
            assert.deepEqual([longitude, latitude, height], [127, 35, 20]);
            return {x: 400, y: 300};
        }},
        Color: { fromCssColorString: value => value },
        HeightReference: {},
        LabelStyle: {},
        DistanceDisplayCondition: class {}
    };
    let offline = false;
    const device = {deviceId: '75', deviceStatus: 'NORMAL', lat: 35, lng: 127, stationName: '관측소'};
    const peer = {...device, deviceId: '86'};
    const missingLocation = {...device, deviceId: '87', lat: null};
    let devices = [device, peer, missingLocation];
    const h = harness(url => {
        assert.equal(url, '/hdcs/api/dashboard/live');
        return offline ? Promise.reject(new Error('offline')) : Promise.resolve({data: {...data, devices}});
    }, cesium, overlay);
    await flush();
    assert.equal(markers.size, 1);
    const [marker] = markers;
    assert.deepEqual(marker.position, [127, 35, 0]);
    assert.equal(marker.point.color, h.scope.statusColor('NORMAL'));
    h.scope.highlightDeviceOnMap(peer);
    renderSelection(scene);
    assert.deepEqual(cameraTargets, [[127, 35, 50000]]);
    assert.equal(marker.point.pixelSize, 19);
    assert.equal(overlay.hidden, false);
    assert.equal(overlay.style.transform, 'translate(400px, 300px) translate(-50%, -50%)');

    for (const status of ['WARNING', 'ERROR', 'NORMAL']) {
        devices = [{...device, deviceStatus: status}, peer];
        h.poll();
        await flush();
        assert.deepEqual([...markers], [marker]);
        assert.equal(marker.point.color, h.scope.statusColor(status));
        assert.equal(marker.point.pixelSize, 19);
        assert.equal(overlay.style['--station-marker-color'], h.scope.statusColor(status));
    }
    h.scope.highlightDeviceOnMap(missingLocation);
    renderSelection(scene);
    assert.equal(overlay.hidden, true);
    assert.equal(cameraTargets.length, 1);
    assert.equal(marker.point.pixelSize, 10);
    offline = true;
    h.poll();
    await flush();
    assert.equal(markers.size, 1);
    offline = false;
    devices = [];
    h.poll();
    await flush();
    assert.equal(markers.size, 0);
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
