const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
test('storage query errors keep previous policy lists',async()=>{
  const scope={};
  let fail=false;
  vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/storage/storage.controller.js'),'utf8'),{
    ctx:'/hdcs', angular:{copy:v=>structuredClone(v),extend:Object.assign}, alert(){},
    qosApp:{controller(name,deps){deps.at(-1)(scope,{
      get(url){return fail?Promise.reject({status:503}):Promise.resolve({data:url.endsWith('/devices')?[]:[{id:1,enabled:true}]});}
    });}}
  });
  await new Promise(r=>setImmediate(r));
  fail=true;
  scope.loadDeleteTargets();scope.loadRetentionPolicies();scope.loadSpacePolicies();
  await new Promise(r=>setImmediate(r));
  assert.equal(scope.deleteTargets.length,1);
  assert.equal(scope.retentionPolicies.length,1);
  assert.equal(scope.spacePolicies.length,1);
  assert.ok(scope.errorMessage);
});
