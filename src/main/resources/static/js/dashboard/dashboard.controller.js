'use strict';

qosApp.controller('DashboardCtrl', ['$scope', '$http', '$interval', function ($scope, $http, $interval) {

    $scope.summary = { totalDevices: 0, normal: 0, fault: 0, warn: 0 };
    $scope.devices       = [];
    $scope.deviceHistory = [];
    $scope.currentTime   = '';
    $scope.equipPanelCollapsed = true;
    $scope.selectedDevice  = null;
    $scope.deviceModalOpen = false;
    $scope.statusLoaded = false;
    $scope.statusError = '';
    $scope.lastStatusCheckedAt = null;

    $scope.bandStatus = function (kind) {
        if (!$scope.statusLoaded) return 'UNKNOWN';
        var states = [];
        $scope.devices.forEach(function (device) {
            var components = device.pipelineComponents || [];
            components.forEach(function (component) {
                if (component.dataKind === kind) states.push(component.status);
            });
        });
        if (!states.length) return 'UNKNOWN';
        if (states.indexOf('ERROR') >= 0) return 'ERROR';
        if (states.indexOf('WARNING') >= 0) return 'WARNING';
        return 'NORMAL';
    };
    $scope.statusColor = function (status) {
        return { NORMAL: '#28b67a', WARNING: '#e2ad36', ERROR: '#ec6262' }[status] || '#85909d';
    };
    $scope.componentLabel = function (component) {
        if (component === 'MONITOR') return '수신 감시';
        if (component === 'DELIVERY') return '자료 전송';
        if (component === 'DCS_SENDER_RUNTIME' || component === 'KA_SENDER_RUNTIME') return '전송기';
        if (component === 'DCS_RECEIVER_RUNTIME' || component === 'KA_RECEIVER_RUNTIME') return '수신기';
        if (component === 'DCS_MONITOR_RUNTIME' || component === 'KA_MONITOR_RUNTIME') return '감시기';
        return component;
    };

    $scope.toggleEquipPanel = function () {
        $scope.equipPanelCollapsed = !$scope.equipPanelCollapsed;
    };

    $scope.statusLabel = function (status) {
        if (status === 'NORMAL')  return '정상';
        if (status === 'WARNING') return '경고';
        if (status === 'ERROR')   return '장애';
        if (status === 'UNKNOWN') return '정보 없음';
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
    var clockTimer = $interval(updateTime, 1000);
    var destroyed = false;
    var refreshing = false;
    function refreshStatus() {
        if (destroyed || refreshing) return;
        refreshing = true;
        $http.get(ctx + '/api/dashboard/live', { cache: false })
            .then(function (res) {
                if (destroyed) return;
                if (!res.data || !res.data.summary || !Array.isArray(res.data.devices)) throw new Error('Invalid status response');
                $scope.summary = res.data.summary;
                $scope.devices = res.data.devices;
                $scope.statusLoaded = true;
                $scope.statusError = '';
                $scope.lastStatusCheckedAt = res.data.checkedAt;
                if ($scope.selectedDevice) {
                    var updated = $scope.devices.find(function (d) { return d.deviceId === $scope.selectedDevice.deviceId; });
                    if (updated) $scope.selectedDevice = updated;
                }
            })
            .catch(function () {
                if (!destroyed) $scope.statusError = '상태 갱신 실패';
            })
            .finally(function () { refreshing = false; });
    }
    refreshStatus();
    var statusTimer = $interval(refreshStatus, 5000);
    $scope.$on('$destroy', function () {
        destroyed = true;
        $interval.cancel(clockTimer);
        $interval.cancel(statusTimer);
        if (cesiumViewer && !cesiumViewer.isDestroyed()) cesiumViewer.destroy();
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

    // ── 관측소 마커 렌더링 ──
    function renderStationMarkers(viewer, stations) {
        stations.forEach(function (station) {
            if (!station.lat || !station.lng) return;
            viewer.entities.add({
                id:       'station_' + station.stationId,
                name:     station.name,
                position: Cesium.Cartesian3.fromDegrees(station.lng, station.lat, 0),
                point: {
                    pixelSize:                10,
                    color:                    Cesium.Color.fromCssColorString('#2ecc71'),
                    outlineColor:             Cesium.Color.WHITE,
                    outlineWidth:             2,
                    heightReference:          Cesium.HeightReference.CLAMP_TO_GROUND,
                    disableDepthTestDistance: Number.POSITIVE_INFINITY
                },
                label: {
                    text:                     station.name,
                    font:                     '11px Malgun Gothic, sans-serif',
                    fillColor:                Cesium.Color.WHITE,
                    outlineColor:             Cesium.Color.BLACK,
                    outlineWidth:             2,
                    style:                    Cesium.LabelStyle.FILL_AND_OUTLINE,
                    pixelOffset:              new Cesium.Cartesian2(0, -18),
                    heightReference:          Cesium.HeightReference.CLAMP_TO_GROUND,
                    disableDepthTestDistance: Number.POSITIVE_INFINITY,
                    distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 300000)
                }
            });
        });
    }

    var cesiumViewer = null; // 지도 제어 함수에서 공유

    function loadStationMarkers(viewer) {
        $http.get(ctx + '/api/dashboard/stations')
            .then(function (res) {
                renderStationMarkers(viewer, res.data);
            })
            .catch(function (err) {
                console.warn('[stations API 오류]', err);
            });
    }

    // ── 지도 컨트롤 버튼 핸들러 ──
    $scope.mapZoomIn = function () {
        if (!cesiumViewer) return;
        var cam = cesiumViewer.camera;
        cam.zoomIn(cam.positionCartographic.height * 0.4);
    };

    $scope.mapZoomOut = function () {
        if (!cesiumViewer) return;
        var cam = cesiumViewer.camera;
        cam.zoomOut(cam.positionCartographic.height * 0.6);
    };

    $scope.mapGoHome = function () {
        if (!cesiumViewer) return;
        cesiumViewer.camera.flyTo({
            destination: Cesium.Cartesian3.fromDegrees(127.2, 35.2, 500000),
            duration: 1.5
        });
    };

    $scope.mapCurrentLocation = function () {
        if (!cesiumViewer || !navigator.geolocation) return;
        navigator.geolocation.getCurrentPosition(function (pos) {
            cesiumViewer.camera.flyTo({
                destination: Cesium.Cartesian3.fromDegrees(
                    pos.coords.longitude, pos.coords.latitude, 50000),
                duration: 1.5
            });
        }, function () {
            $scope.mapGoHome();
        });
    };

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

        // 초기 카메라: 전라남북도 중심
        viewer.camera.setView({
            destination: Cesium.Cartesian3.fromDegrees(127.2, 35.2, 500000)
        });

        cesiumViewer = viewer;
        loadStationMarkers(viewer);
    }

    // Ion 한국 지형 비동기 로드 → 실패 시 기본 타원체로 폴백
    Cesium.CesiumTerrainProvider.fromIonAssetId(ION_TERRAIN_ID)
        .then(function (terrain) { initViewer(terrain); })
        .catch(function ()        { initViewer(new Cesium.EllipsoidTerrainProvider()); });
}]);
