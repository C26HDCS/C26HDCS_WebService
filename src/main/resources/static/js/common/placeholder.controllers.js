'use strict';

qosApp.controller('ConfigCtrl', ['$scope', '$http', function ($scope, $http) {

    /* ── 검색 조건 ── */
    $scope.search = {
        deviceId:     '',
        stationCode:  '',
        stationName:  '',
        deviceType:   '',
        group:        '',
        configStatus: ''
    };

    /* ── 그룹 선택지 ── */
    $scope.groupOptions = [];

    $http.get(ctx + '/api/config/groups')
        .then(function (res) { $scope.groupOptions = res.data; })
        .catch(function ()   { $scope.groupOptions = []; });

    /* ── 원본 / 표시용 데이터 ── */
    var allData = [];
    $scope.filteredList = [];
    $scope.pagedList    = [];
    $scope.hasDirty     = false;

    /* ── 페이지네이션 ── */
    $scope.pageSize    = 15;
    $scope.currentPage = 1;
    $scope.totalPages  = 1;
    $scope.pageNums    = [];

    function updatePage() {
        $scope.totalPages  = Math.max(1, Math.ceil($scope.filteredList.length / $scope.pageSize));
        $scope.currentPage = Math.min($scope.currentPage, $scope.totalPages);
        var start = ($scope.currentPage - 1) * $scope.pageSize;
        $scope.pagedList = $scope.filteredList.slice(start, start + $scope.pageSize);
        var from = Math.max(1, $scope.currentPage - 4);
        var to   = Math.min($scope.totalPages, $scope.currentPage + 4);
        var nums = [];
        for (var i = from; i <= to; i++) nums.push(i);
        $scope.pageNums = nums;
    }

    $scope.goToPage = function (n) {
        if (n < 1 || n > $scope.totalPages) return;
        $scope.currentPage = n;
        updatePage();
    };

    /* ── 초기 로드 ── */
    $scope.load = function () {
        $http.get(ctx + '/api/config/list')
            .then(function (res) {
                allData = res.data.map(function (item) {
                    item._dirty = false;
                    return item;
                });
                $scope.applySearch();
            })
            .catch(function () {
                allData = [];
                $scope.filteredList = [];
            });
    };

    /* ── 검색 필터 적용 ── */
    $scope.applySearch = function () {
        var s = $scope.search;
        $scope.filteredList = allData.filter(function (item) {
            if (s.deviceId    && String(item.equipId || '').indexOf(s.deviceId) === -1)       return false;
            if (s.stationCode && String(item.obsCode || '').indexOf(s.stationCode) === -1)    return false;
            if (s.stationName && (item.displayName || '').indexOf(s.stationName) === -1)      return false;
            if (s.deviceType  && item.equipType !== s.deviceType)                             return false;
            if (s.group       && item.groupName !== s.group)                                  return false;
            if (s.configStatus && item.configStatus !== s.configStatus)                       return false;
            return true;
        });
        $scope.currentPage = 1;
        updatePage();
    };

    /* ── 변경 마크 ── */
    $scope.markDirty = function (item) {
        item._dirty = true;
        $scope.hasDirty = true;
    };

    /* ── 연필 버튼 → 해당 input 포커스 ── */
    $scope.focusNameInput = function ($event) {
        var btn = $event.currentTarget;
        var input = btn.parentElement.querySelector('.cfg-name-input');
        if (input) { input.focus(); input.select(); }
    };

    /* ── NMS 상태 CSS 클래스 (뷰가 이미 정상/주의/장애로 변환해서 내려줌) ── */
    $scope.getNmsStatusClass = function (status) {
        if (status === '정상') return 'cfg-nms-normal';
        if (status === '주의') return 'cfg-nms-warn';
        if (status === '장애') return 'cfg-nms-error';
        return '';
    };

    /* ── 설정 상태 CSS 클래스 (뷰 config_status 값 직접 사용) ── */
    $scope.getConfigStatusClass = function (configStatus) {
        if (configStatus === '설정완료') return 'cfg-status-done';
        return 'cfg-status-partial';
    };

    /* ── 변경사항 저장 ── */
    $scope.saveChanges = function () {
        var dirty = allData.filter(function (item) { return item._dirty; });
        if (!dirty.length) return;
        /* TODO: $http.post(ctx + '/api/config/save', dirty) 로 교체 */
        dirty.forEach(function (item) { item._dirty = false; });
        $scope.hasDirty = false;
        alert('변경사항이 저장되었습니다. (' + dirty.length + '건)');
    };

    /* ── 장비 상세 모달 ── */
    $scope.detailItem       = {};
    $scope.showDetailModal  = false;

    $scope.openDetailModal = function (item) {
        $scope.detailItem      = item;
        $scope.showDetailModal = true;
    };

    $scope.closeDetailModal = function () {
        $scope.showDetailModal = false;
    };

    /* ── 그룹 관리 모달 (추후 구현) ── */
    $scope.openGroupManager = function () {
        alert('그룹 관리 기능은 준비 중입니다.');
    };

    /* ── 환경설정 CSV 일괄등록 모달 ── */
    $scope.showRegisterModal = false;
    $scope.csvRows           = [];
    $scope.csvPreview        = [];
    $scope.registerError     = '';
    $scope.registerLoading   = false;

    $scope.openRegisterModal = function () {
        $scope.csvRows         = [];
        $scope.csvPreview      = [];
        $scope.registerError   = '';
        $scope.registerLoading = false;
        $scope.showRegisterModal = true;
    };

    $scope.closeRegisterModal = function () {
        $scope.showRegisterModal = false;
    };

    $scope.triggerFileInput = function () {
        document.getElementById('csvFileInput').click();
    };

    $scope.onFileSelect = function (input) {
        if (!input.files || !input.files[0]) return;
        var file = input.files[0];
        var reader = new FileReader();
        reader.onload = function (e) {
            $scope.$apply(function () {
                var result = parseCsvText(e.target.result);
                if (result.error) {
                    $scope.registerError = result.error;
                    $scope.csvRows       = [];
                    $scope.csvPreview    = [];
                } else {
                    $scope.csvRows     = result.rows;
                    $scope.csvPreview  = result.rows.slice(0, 5);
                    $scope.registerError = '';
                }
            });
            input.value = '';
        };
        reader.readAsText(file, 'UTF-8');
    };

    function parseCsvText(text) {
        text = text.replace(/^﻿/, '');
        var lines = text.split(/\r?\n/).filter(function (l) { return l.trim(); });
        if (lines.length < 2) {
            return { error: '데이터가 없습니다. 헤더 포함 최소 2행이 필요합니다.' };
        }
        var headers = lines[0].split(',').map(function (h) { return h.trim(); });
        var obsIdx = -1, nameIdx = -1;
        headers.forEach(function (h, i) {
            if (h === '관측소코드' || h === 'obs_code' || h === 'obsCode') obsIdx = i;
            if (h === '관측소표시명' || h === 'display_name' || h === 'displayName') nameIdx = i;
        });
        if (obsIdx === -1 || nameIdx === -1) {
            return { error: '헤더 형식 오류. 관측소코드, 관측소표시명 컬럼이 필요합니다.' };
        }
        var rows = [];
        for (var i = 1; i < lines.length; i++) {
            var cols = lines[i].split(',');
            var obsCode     = (cols[obsIdx]  || '').trim();
            var displayName = (cols[nameIdx] || '').trim();
            if (!obsCode) continue;
            rows.push({ obsCode: obsCode, displayName: displayName });
        }
        if (!rows.length) return { error: '유효한 데이터가 없습니다.' };
        return { rows: rows };
    }

    $scope.downloadTemplate = function () {
        var csv  = '﻿관측소코드,관측소표시명\n';
        var blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        var url  = URL.createObjectURL(blob);
        var a    = document.createElement('a');
        a.href     = url;
        a.download = '환경설정등록_양식.csv';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    $scope.submitRegister = function () {
        if (!$scope.csvRows.length || $scope.registerLoading) return;
        $scope.registerLoading = true;
        $scope.registerError   = '';
        $http.post(ctx + '/api/config/register/csv', $scope.csvRows)
            .then(function (res) {
                $scope.registerLoading   = false;
                $scope.showRegisterModal = false;
                alert(res.data.count + '건이 등록되었습니다.');
                $scope.load();
            })
            .catch(function () {
                $scope.registerLoading = false;
                $scope.registerError   = '등록 중 오류가 발생했습니다. 다시 시도해주세요.';
            });
    };

    $scope.load();
}]);
qosApp.controller('ReportCtrl', ['$scope', '$http', function ($scope, $http) {
    $scope.reportList = [];
    $scope.searchKeyword = '';
    $scope.searchFormat  = '';
    $scope.searchMethod  = '';

    $scope.gen = { format: 'PDF', method: 'MANUAL' };

    $scope.resetGen = function () {
        $scope.gen = { format: 'PDF', method: 'MANUAL' };
    };

    $scope.generateReport = function () {
        // UI only — API 연동 시 구현
    };

    $scope.load = function () {
        $http.get(ctx + '/api/report/list')
            .then(function (res) { $scope.reportList = res.data; })
            .catch(function ()   { $scope.reportList = []; });
    };

    $scope.load();
}]);
