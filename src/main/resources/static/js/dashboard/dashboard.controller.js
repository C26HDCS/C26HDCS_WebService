'use strict';

qosApp.controller('DashboardCtrl', ['$scope', '$http', '$interval', function ($scope, $http, $interval) {

    $scope.summary = { totalDevices: 0, normal: 0, fault: 0, warn: 0 };
    $scope.devices       = [];
    $scope.deviceHistory = [];
    $scope.currentTime   = '';
    $scope.equipPanelCollapsed = true;
    $scope.selectedDevice  = null;
    $scope.deviceModalOpen = false;

    $scope.toggleEquipPanel = function () {
        $scope.equipPanelCollapsed = !$scope.equipPanelCollapsed;
    };

    $scope.statusLabel = function (status) {
        if (status === 'NORMAL')  return '정상';
        if (status === 'WARNING') return '경고';
        if (status === 'ERROR')   return '장애';
        return status || '-';
    };

    $scope.historyStatusLabel = function (status) {
        if (status === 'NORMAL'  || status === 'SUCCESS') return '성공';
        if (status === 'WARNING')                         return '경고';
        if (status === 'ERROR'   || status === 'FAILED' || status === 'FAIL') return '오류';
        return status || '-';
    };

    $scope.openDeviceDetail = function (device) {
        $scope.selectedDevice  = device;
        $scope.deviceHistory   = [];
        $scope.deviceModalOpen = true;
        $http.get(ctx + '/api/dashboard/devices/' + device.deviceId + '/data', {
            params: { deviceType: device.deviceType }
        })
            .then(function (res) { $scope.deviceHistory = res.data; })
            .catch(function ()   { $scope.deviceHistory = []; });
    };

    $scope.closeDeviceModal = function () {
        $scope.deviceModalOpen = false;
        $scope.selectedDevice  = null;
        $scope.deviceHistory   = [];
    };

    function pad(n) { return n < 10 ? '0' + n : n; }
    function updateTime() {
        var now = new Date();
        $scope.currentTime =
            now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) +
            ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds()) + ' KST';
    }
    updateTime();
    $interval(updateTime, 1000);

    $http.get(ctx + '/api/dashboard/summary')
        .then(function (res) {
            $scope.summary = res.data;
        })
        .catch(function () {
            $scope.summary = { totalDevices: 80, normal: 75, fault: 5, warn: 5 };
        });

    $http.get(ctx + '/api/dashboard/devices')
        .then(function (res) {
            $scope.devices = res.data;
        });

    // ── CesiumJS 지도 초기화 (CesiumMapManager.ts 설정값 적용) ──
    var ION_TOKEN          = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJmYWJlYjRmYy03ZTAxLTRjMDAtYTA3NC0wNTg2M2RiOThkNTIiLCJpZCI6MjIyMTkxLCJpYXQiOjE3MTgzNTI0MjJ9.Ieo02cB_7Iin7ed4IN_Vnv0ivyuXkNBHduWnFFVm7hw';
    var ION_TERRAIN_ID     = 3124909; // 한국 고해상도 지형 Asset
    var VWORLD_KEY         = 'EFEF8448-8509-379E-97FE-E69D9B60914D';

    Cesium.Ion.defaultAccessToken = ION_TOKEN;

    // VWorld WMTS 프로바이더 생성 (WebMapTileServiceImageryProvider 방식이 정확)
    function createVWorldProvider(layerName, tileType) {
        return new Cesium.WebMapTileServiceImageryProvider({
            url:            'https://api.vworld.kr/req/wmts/1.0.0/' + VWORLD_KEY + '/' + layerName + '/{TileMatrix}/{TileRow}/{TileCol}.' + tileType,
            layer:          layerName,
            style:          'default',
            tileMatrixSetID: 'EPSG:900913',
            maximumLevel:   19,
            credit:         new Cesium.Credit('국토지리정보원 VWorld')
        });
    }

    function initViewer(terrainProvider) {
        var viewer = new Cesium.Viewer('cesiumContainer', {
            baseLayer:            new Cesium.ImageryLayer(createVWorldProvider('Satellite', 'jpeg')),
            terrainProvider:      terrainProvider,
            baseLayerPicker:      false,
            navigationHelpButton: false,
            sceneModePicker:      false,
            animation:            false,
            timeline:             false,
            fullscreenButton:     false,
            homeButton:           false,
            geocoder:             false,
            infoBox:              false,
            selectionIndicator:   false
        });

        // 한국어 수계·지명 레이블 (Hybrid 레이어)
        viewer.imageryLayers.addImageryProvider(createVWorldProvider('Hybrid', 'png'));

        // 초기 카메라: 한반도 전체 (CesiumMapManager.ts initialPoint 기준)
        viewer.camera.setView({
            destination: Cesium.Cartesian3.fromDegrees(127.7, 37.9, 1000000)
        });
    }

    // Ion 한국 지형 비동기 로드 → 실패 시 기본 타원체로 폴백
    Cesium.CesiumTerrainProvider.fromIonAssetId(ION_TERRAIN_ID)
        .then(function (terrain) { initViewer(terrain); })
        .catch(function ()        { initViewer(new Cesium.EllipsoidTerrainProvider()); });
}]);
