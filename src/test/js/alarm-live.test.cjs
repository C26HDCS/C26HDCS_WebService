const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

test('SSE alarm change reloads the alarm list without resetting the page', async () => {
    const handlers = {};
    const scope = {
        $on(name, handler) { handlers[name] = handler; }
    };
    let requests = 0;
    const http = {
        get() {
            requests++;
            return Promise.resolve({ data: Array.from({ length: 25 }, (_, id) => ({ id })) });
        }
    };
    const context = {
        qosApp: {
            controller(name, dependencies) {
                dependencies[dependencies.length - 1](scope, http);
            }
        },
        ctx: '/hdcs',
        confirm() { return true; },
        alert() { throw new Error('Unexpected validation alert'); }
    };
    const script = fs.readFileSync(path.join(__dirname,
        '../../main/resources/static/js/alarm/alarm.controller.js'), 'utf8');
    vm.runInNewContext(script, context);
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(requests, 1);
    scope.goToPage(2);
    handlers.alarmDataChanged();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(requests, 2);
    assert.equal(scope.currentPage, 2);
    assert.equal(scope.pagedList[0].id, 10);
});
