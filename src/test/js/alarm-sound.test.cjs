const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

function load(opts = {}) {
  const store = opts.store || {};
  let created = 0, oscillators = 0;
  const win = {
    localStorage: {
      getItem: k => (k in store ? store[k] : null),
      setItem: (k, v) => { store[k] = v; }
    },
    addEventListener() {}, removeEventListener() {},
    AudioContext: opts.noAudio ? undefined : function () {
      created++;
      this.state = opts.state || 'running';
      this.currentTime = 0;
      this.destination = {};
      this.resume = () => {};
      this.createOscillator = () => { oscillators++; return { type: '', frequency: {}, connect() {}, start() {}, stop() {} }; };
      this.createGain = () => ({ gain: { setValueAtTime() {}, exponentialRampToValueAtTime() {} }, connect() {} });
    }
  };
  let factory;
  const sandbox = { qosApp: { factory: (n, arr) => { factory = arr[arr.length - 1]; } } };
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/common/alarm-sound.js'), 'utf8'), sandbox);
  const svc = factory(win);
  return { svc, store, stats: () => ({ created, oscillators }) };
}

test('첫 값은 기준값이라 울리지 않는다', () => {
  const { svc, stats } = load();
  assert.strictEqual(svc.observeUnread(3), false);
  assert.strictEqual(stats().oscillators, 0);
});
test('개수가 늘면 1회(3음) 울린다', () => {
  const { svc, stats } = load();
  svc.observeUnread(0);
  assert.strictEqual(svc.observeUnread(1), true);
  assert.strictEqual(stats().oscillators, 3);
});
test('같거나 줄면 울리지 않고, 줄어든 값이 새 기준이 된다', () => {
  const { svc } = load();
  svc.observeUnread(5);
  assert.strictEqual(svc.observeUnread(5), false);
  assert.strictEqual(svc.observeUnread(2), false);
  assert.strictEqual(svc.observeUnread(3), true);
});
test('꺼 두면 울리지 않고 설정이 저장된다', () => {
  const { svc, store } = load();
  svc.setEnabled(false);
  assert.strictEqual(store['hdcs.alarmSound.enabled'], 'off');
  svc.observeUnread(0);
  assert.strictEqual(svc.observeUnread(4), false);
});
test('저장된 off 설정을 읽는다', () => {
  const { svc } = load({ store: { 'hdcs.alarmSound.enabled': 'off' } });
  assert.strictEqual(svc.isEnabled(), false);
});
test('브라우저가 막은 상태면 blocked 로 표시한다', () => {
  const { svc } = load({ state: 'suspended' });
  let blocked = null;
  svc.onBlockedChange = v => { blocked = v; };
  svc.observeUnread(0);
  assert.strictEqual(svc.observeUnread(1), false);
  assert.strictEqual(svc.blocked, true);
  assert.strictEqual(blocked, true);
});
test('오디오 미지원 브라우저에서도 오류 없이 넘어간다', () => {
  const { svc } = load({ noAudio: true });
  svc.observeUnread(0);
  assert.strictEqual(svc.observeUnread(1), false);
});
test('숫자가 아닌 값은 무시한다', () => {
  const { svc } = load();
  assert.strictEqual(svc.observeUnread(undefined), false);
  assert.strictEqual(svc.observeUnread('3'), false);
});
