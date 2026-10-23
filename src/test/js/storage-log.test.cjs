const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

test('storage events can be filtered and use compact status labels', async () => {
    const scope = {};
    let query;
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,
        '../../main/resources/static/js/log/log.controller.js'), 'utf8'), {
        qosApp: { controller(name, dependencies) { dependencies.at(-1)(scope, {
            get(url, options) { query = options.params; return Promise.resolve({ data: [] }); }
        }); } },
        ctx: '/hdcs',
        alert() { throw new Error('Unexpected alert'); }
    });
    await new Promise(resolve => setImmediate(resolve));
    scope.filterCategory = 'EVENT';
    scope.onCategoryChange();
    const storage = scope.levelOptions.filter(o => o.value.startsWith('STORAGE_'));
    assert.equal(storage.length, 7);
    for (const option of storage) {
        assert.ok(scope.getLevelLabel(option.value).length <= 5);
    }
    scope.filterLevel = 'STORAGE_DELETE_SUCCESS';
    scope.load();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(query.searchLevel, 'STORAGE_DELETE_SUCCESS');
    assert.equal(scope.getLevelClass('STORAGE_DELETE_FAIL'), 'log-badge-error');
    scope.filterCategory = 'LOG';
    scope.onCategoryChange();
    assert.equal(scope.filterLevel, null);
    assert.ok(scope.levelOptions.every(o => !o.value.startsWith('STORAGE_')));
});
