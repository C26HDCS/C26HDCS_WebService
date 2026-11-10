'use strict';

qosApp.controller('DashboardCtrl', ['$scope', '$http', '$interval', function ($scope, $http, $interval) {

    $scope.summary = { totalDevices: 0, normal: 0, fault: 0, warn: 0 };
    $scope.devices       = [];
    $scope.listDevices = [];
    $scope.deviceGroups = [];
    $scope.deviceListFilter = { groupName: '' };
    $scope.deviceHistory = [];
    $scope.currentTime   = '';
    $scope.equipPanelCollapsed = true;
    $scope.diagramCollapsed = false;
    $scope.selectedDevice  = null;
    $scope.highlightedDeviceId = null;
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

    $scope.toggleDiagram = function () {
        $scope.diagramCollapsed = !$scope.diagramCollapsed;
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
    $scope.filterDeviceList = function () {
        var groupName = $scope.deviceListFilter.groupName;
        $scope.listDevices = groupName
            ? $scope.devices.filter(function (device) { return device.groupName === groupName; })
            : $scope.devices;
    };

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
                $scope.deviceGroups = Array.from(new Set($scope.devices.map(function (device) {
                    return device.groupName;
                }).filter(Boolean))).sort(function (a, b) { return a.localeCompare(b, 'ko'); });
                if (!$scope.deviceGroups.includes($scope.deviceListFilter.groupName)) {
                    $scope.deviceListFilter.groupName = '';
                }
                $scope.filterDeviceList();
                $scope.statusLoaded = true;
                $scope.statusError = '';
                $scope.lastStatusCheckedAt = res.data.checkedAt;
                syncDeviceMarkers();
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
        if (removeSelectionRenderListener) removeSelectionRenderListener();
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

    var cesiumViewer = null;
    var selectionOverlay = null;
    var selectedPosition = null;
    var removeSelectionRenderListener = null;
    var markerStates = new Map();
    var statusPriority = { NORMAL: 1, WARNING: 2, ERROR: 3 };

    function hasMapCoordinates(device) {
        return Number.isFinite(device.lat) && Math.abs(device.lat) <= 90 &&
               Number.isFinite(device.lng) && Math.abs(device.lng) <= 180;
    }

    function focusDeviceLocation(device) {
        if (!cesiumViewer || !hasMapCoordinates(device)) return;
        var camera = cesiumViewer.camera;
        camera.flyTo({
            destination: Cesium.Cartesian3.fromDegrees(
                device.lng, device.lat, camera.positionCartographic.height),
            duration: 0.6
        });
    }

    $scope.highlightDeviceOnMap = function (device) {
        $scope.highlightedDeviceId = device.deviceId;
        syncDeviceMarkers();
        focusDeviceLocation(device);
    };

    function positionSelectionOverlay(scene) {
        if (!selectedPosition) {
            selectionOverlay.hidden = true;
            return;
        }
        var height = scene.globe.getHeight(selectedPosition) || 0;
        var position = Cesium.Cartesian3.fromRadians(
            selectedPosition.longitude, selectedPosition.latitude, height);
        var pixel = Cesium.SceneTransforms.wgs84ToWindowCoordinates(scene, position);
        var visible = pixel && pixel.x >= 0 && pixel.y >= 0 &&
                      pixel.x < scene.canvas.clientWidth && pixel.y < scene.canvas.clientHeight;
        selectionOverlay.hidden = !visible;
        if (visible) {
            selectionOverlay.style.transform =
                'translate(' + pixel.x + 'px, ' + pixel.y + 'px) translate(-50%, -50%)';
        }
    }

    function syncDeviceMarkers() {
        if (!cesiumViewer) return;
        var locations = new Map();
        $scope.devices.forEach(function (device) {
            if (!hasMapCoordinates(device)) return;
            var key = device.lat + ',' + device.lng;
            var location = locations.get(key);
            var priority = statusPriority[device.deviceStatus] || 0;
            var highlighted = device.deviceId === $scope.highlightedDeviceId;
            if (!location) {
                locations.set(key, { device: device, priority: priority, count: 1, highlighted: highlighted });
            } else {
                location.count++;
                location.highlighted = location.highlighted || highlighted;
                if (priority > location.priority) {
                    location.device = device;
                    location.priority = priority;
                }
            }
        });

        selectedPosition = null;
        locations.forEach(function (location, key) {
            var device = location.device;
            var name = device.stationName || device.deviceName;
            if (location.count > 1) name += ' 외 ' + (location.count - 1) + '대';
            var color = $scope.statusColor(device.deviceStatus);
            if (selectionOverlay && location.highlighted) {
                selectedPosition = Cesium.Cartographic.fromDegrees(device.lng, device.lat);
                selectionOverlay.style.setProperty('--station-marker-color', color);
            }
            var pixelSize = location.highlighted ? 19 : 10;
            var outlineWidth = location.highlighted ? 4 : 2;
            var marker = markerStates.get(key);
            if (marker) {
                if (marker.color !== color) {
                    marker.entity.point.color = Cesium.Color.fromCssColorString(color);
                    marker.color = color;
                }
                if (marker.name !== name) {
                    marker.entity.name = name;
                    marker.entity.label.text = name;
                    marker.name = name;
                }
                if (marker.highlighted !== location.highlighted) {
                    marker.entity.point.pixelSize = pixelSize;
                    marker.entity.point.outlineWidth = outlineWidth;
                    marker.highlighted = location.highlighted;
                }
                return;
            }
            var entity = cesiumViewer.entities.add({
                id:       'device_location_' + key,
                name:     name,
                position: Cesium.Cartesian3.fromDegrees(device.lng, device.lat, 0),
                point: {
                    pixelSize:                pixelSize,
                    color:                    Cesium.Color.fromCssColorString(color),
                    outlineColor:             Cesium.Color.WHITE,
                    outlineWidth:             outlineWidth,
                    heightReference:          Cesium.HeightReference.CLAMP_TO_GROUND,
                    disableDepthTestDistance: Number.POSITIVE_INFINITY
                },
                label: {
                    text:                     name,
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
            markerStates.set(key, { entity: entity, color: color, name: name, highlighted: location.highlighted });
        });
        markerStates.forEach(function (marker, key) {
            if (locations.has(key)) return;
            cesiumViewer.entities.remove(marker.entity);
            markerStates.delete(key);
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
        if (destroyed) return;
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
        selectionOverlay = document.getElementById('dash-selected-marker');
        if (selectionOverlay) {
            removeSelectionRenderListener = viewer.scene.postRender.addEventListener(positionSelectionOverlay);
        }
        syncDeviceMarkers();
        var highlighted = $scope.devices.find(function (device) {
            return device.deviceId === $scope.highlightedDeviceId;
        });
        if (highlighted) focusDeviceLocation(highlighted);
    }

    // Ion 한국 지형 비동기 로드 → 실패 시 기본 타원체로 폴백
    Cesium.CesiumTerrainProvider.fromIonAssetId(ION_TERRAIN_ID)
        .catch(function () { return new Cesium.EllipsoidTerrainProvider(); })
        .then(initViewer);
}]);
