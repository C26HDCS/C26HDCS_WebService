'use strict';

qosApp.controller('StorageCtrl', ['$scope', '$http', function ($scope, $http) {
    $scope.devices = [];
    $scope.files = [];
    $scope.deleteTargets = [];
    $scope.retentionPolicies = [];
    $scope.selectedFiles = {};
    $scope.selectedDevice = null;
    $scope.currentPath = '';
    $scope.pathHistory = [];
    $scope.loadingDevices = false;
    $scope.loadingFiles = false;
    $scope.loadingDeleteTargets = false;
    $scope.loadingRetentionPolicies = false;
    $scope.deletingFiles = false;
    $scope.editingDeleteTarget = null;
    $scope.savingDeleteTargetId = null;
    $scope.savingRetentionPolicyId = null;
    $scope.creatingDeleteTarget = false;
    $scope.creatingRetentionPolicy = false;
    $scope.deletingDeleteTargetId = null;
    $scope.deletingRetentionPolicyId = null;
    $scope.showingNewDeleteTarget = false;
    $scope.showingNewRetentionPolicy = false;
    $scope.policyTab = 'deleteTargets';
    $scope.errorMessage = '';

    $scope.setPolicyTab = function (tab) {
        $scope.policyTab = tab;
    };

    $scope.resetNewDeleteTarget = function () {
        $scope.newDeleteTarget = {
            targetName: '',
            deleteMode: 'MANUAL',
            targetType: 'RULE',
            basePath: '',
            filePattern: '',
            fileExtension: '',
            minAgeDays: null,
            minSizeBytes: null,
            enabled: true,
            description: ''
        };
    };

    $scope.resetNewRetentionPolicy = function () {
        $scope.newRetentionPolicy = {
            fileTypeName: '',
            fileExtension: '',
            retentionDays: 30,
            enabled: true,
            description: ''
        };
    };

    $scope.openNewDeleteTarget = function () {
        $scope.cancelEditDeleteTarget();
        $scope.resetNewDeleteTarget();
        $scope.showingNewDeleteTarget = true;
    };

    $scope.cancelNewDeleteTarget = function () {
        $scope.resetNewDeleteTarget();
        $scope.showingNewDeleteTarget = false;
    };

    $scope.openNewRetentionPolicy = function () {
        $scope.resetNewRetentionPolicy();
        $scope.showingNewRetentionPolicy = true;
    };

    $scope.cancelNewRetentionPolicy = function () {
        $scope.resetNewRetentionPolicy();
        $scope.showingNewRetentionPolicy = false;
    };

    function toNullableNumber(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }
        var parsed = parseInt(value, 10);
        return isNaN(parsed) ? null : parsed;
    }

    function normalizeExtension(value) {
        if (!value) {
            return '';
        }
        return String(value).replace(/^\.+/, '').trim().toLowerCase();
    }

    $scope.loadDevices = function () {
        $scope.loadingDevices = true;
        $scope.errorMessage = '';

        $http.get(ctx + '/api/storage/devices')
            .then(function (res) {
                $scope.devices = res.data || [];
                if ($scope.devices.length > 0) {
                    $scope.selectDevice($scope.devices[0]);
                } else {
                    $scope.selectedDevice = null;
                    $scope.currentPath = '';
                    $scope.pathHistory = [];
                    $scope.files = [];
                    $scope.clearSelection();
                }
            })
            .catch(function () {
                $scope.devices = [];
                $scope.selectedDevice = null;
                $scope.currentPath = '';
                $scope.pathHistory = [];
                $scope.files = [];
                $scope.clearSelection();
                $scope.errorMessage = '저장장치 정보를 조회하지 못했습니다.';
            })
            .finally(function () {
                $scope.loadingDevices = false;
            });
    };

    $scope.loadDeleteTargets = function () {
        $scope.loadingDeleteTargets = true;

        $http.get(ctx + '/api/storage/delete-targets')
            .then(function (res) {
                $scope.deleteTargets = res.data || [];
                if ($scope.editingDeleteTarget) {
                    var editingId = $scope.editingDeleteTarget.id;
                    var latestTarget = $scope.deleteTargets.find(function (target) {
                        return target.id === editingId;
                    });
                    if (latestTarget) {
                        $scope.startEditDeleteTarget(latestTarget);
                    } else {
                        $scope.cancelEditDeleteTarget();
                    }
                }
            })
            .catch(function () {
                $scope.deleteTargets = [];
                $scope.errorMessage = '삭제 대상 기준을 조회하지 못했습니다.';
            })
            .finally(function () {
                $scope.loadingDeleteTargets = false;
            });
    };

    $scope.startEditDeleteTarget = function (target) {
        $scope.showingNewDeleteTarget = false;
        $scope.editingDeleteTarget = angular.copy(target);
        $scope.editingDeleteTarget.minAgeDays = target.minAgeDays === null || target.minAgeDays === undefined
            ? null
            : parseInt(target.minAgeDays, 10);
        $scope.editingDeleteTarget.minSizeBytes = target.minSizeBytes === null || target.minSizeBytes === undefined
            ? null
            : parseInt(target.minSizeBytes, 10);
    };

    $scope.cancelEditDeleteTarget = function () {
        $scope.editingDeleteTarget = null;
    };

    $scope.isEditingDeleteTarget = function (target) {
        return $scope.editingDeleteTarget && target && $scope.editingDeleteTarget.id === target.id;
    };

    $scope.createDeleteTarget = function () {
        var target = $scope.newDeleteTarget;
        if (!target || $scope.creatingDeleteTarget) {
            return;
        }
        if (!target.targetName || !target.basePath) {
            alert('기준명과 기준 경로를 입력해주세요.');
            return;
        }

        var minAgeDays = toNullableNumber(target.minAgeDays);
        var minSizeBytes = toNullableNumber(target.minSizeBytes);
        if (minAgeDays !== null && minAgeDays < 0) {
            alert('경과일 조건은 0 이상으로 입력해주세요.');
            return;
        }
        if (minSizeBytes !== null && minSizeBytes < 0) {
            alert('최소 크기 조건은 0 이상으로 입력해주세요.');
            return;
        }

        $scope.creatingDeleteTarget = true;
        $scope.errorMessage = '';

        $http.post(ctx + '/api/storage/delete-targets', {
            targetName: target.targetName,
            deleteMode: target.deleteMode,
            targetType: target.targetType,
            basePath: target.basePath,
            filePattern: target.filePattern,
            fileExtension: normalizeExtension(target.fileExtension),
            minAgeDays: minAgeDays,
            minSizeBytes: minSizeBytes,
            enabled: !!target.enabled,
            description: target.description
        }).then(function (res) {
            var createdTarget = res.data || {};
            $scope.deleteTargets.push(createdTarget);
            $scope.startEditDeleteTarget(createdTarget);
            $scope.resetNewDeleteTarget();
            $scope.showingNewDeleteTarget = false;
            alert('삭제 대상 기준을 추가했습니다.');
        }).catch(function () {
            $scope.errorMessage = '삭제 대상 기준을 추가하지 못했습니다.';
        }).finally(function () {
            $scope.creatingDeleteTarget = false;
        });
    };

    $scope.saveDeleteTarget = function () {
        var target = $scope.editingDeleteTarget;
        if (!target || $scope.savingDeleteTargetId) {
            return;
        }
        if (!target.targetName || !target.basePath) {
            alert('기준명과 기준 경로를 입력해주세요.');
            return;
        }
        if (target.minAgeDays !== null && target.minAgeDays !== undefined && target.minAgeDays < 0) {
            alert('경과일 조건은 0 이상으로 입력해주세요.');
            return;
        }
        if (target.minSizeBytes !== null && target.minSizeBytes !== undefined && target.minSizeBytes < 0) {
            alert('최소 크기 조건은 0 이상으로 입력해주세요.');
            return;
        }

        target.fileExtension = normalizeExtension(target.fileExtension);
        $scope.savingDeleteTargetId = target.id;
        $scope.errorMessage = '';

        $http.put(ctx + '/api/storage/delete-targets/' + target.id, {
            targetName: target.targetName,
            deleteMode: target.deleteMode,
            targetType: target.targetType,
            basePath: target.basePath,
            filePattern: target.filePattern,
            fileExtension: target.fileExtension,
            minAgeDays: target.minAgeDays,
            minSizeBytes: target.minSizeBytes,
            enabled: !!target.enabled,
            description: target.description
        }).then(function (res) {
            var updatedTarget = res.data || {};
            var replaced = false;
            $scope.deleteTargets = $scope.deleteTargets.map(function (item) {
                if (item.id === updatedTarget.id) {
                    replaced = true;
                    return updatedTarget;
                }
                return item;
            });
            if (!replaced) {
                $scope.deleteTargets.push(updatedTarget);
            }
            $scope.startEditDeleteTarget(updatedTarget);
            alert('삭제 대상 기준을 저장했습니다.');
        }).catch(function () {
            $scope.errorMessage = '삭제 대상 기준을 저장하지 못했습니다.';
        }).finally(function () {
            $scope.savingDeleteTargetId = null;
        });
    };

    $scope.deleteDeleteTarget = function (target) {
        if (!target || $scope.deletingDeleteTargetId) {
            return;
        }
        if (!confirm('삭제 대상 기준 "' + target.targetName + '"을 삭제하시겠습니까?')) {
            return;
        }

        $scope.deletingDeleteTargetId = target.id;
        $scope.errorMessage = '';

        $http.delete(ctx + '/api/storage/delete-targets/' + target.id)
            .then(function () {
                $scope.deleteTargets = $scope.deleteTargets.filter(function (item) {
                    return item.id !== target.id;
                });
                if ($scope.isEditingDeleteTarget(target)) {
                    $scope.cancelEditDeleteTarget();
                }
                alert('삭제 대상 기준을 삭제했습니다.');
            })
            .catch(function () {
                $scope.errorMessage = '삭제 대상 기준을 삭제하지 못했습니다.';
            })
            .finally(function () {
                $scope.deletingDeleteTargetId = null;
            });
    };

    $scope.loadRetentionPolicies = function () {
        $scope.loadingRetentionPolicies = true;

        $http.get(ctx + '/api/storage/retention-policies')
            .then(function (res) {
                $scope.retentionPolicies = res.data || [];
            })
            .catch(function () {
                $scope.retentionPolicies = [];
                $scope.errorMessage = '파일 종류별 보관 기간을 조회하지 못했습니다.';
            })
            .finally(function () {
                $scope.loadingRetentionPolicies = false;
            });
    };

    $scope.createRetentionPolicy = function () {
        var policy = $scope.newRetentionPolicy;
        if (!policy || $scope.creatingRetentionPolicy) {
            return;
        }

        var retentionDays = parseInt(policy.retentionDays, 10);
        var extension = normalizeExtension(policy.fileExtension);
        if (!policy.fileTypeName || !extension) {
            alert('파일 종류명과 확장자를 입력해주세요.');
            return;
        }
        if (!retentionDays || retentionDays < 1 || retentionDays > 3650) {
            alert('보관 기간은 1일부터 3650일 사이로 입력해주세요.');
            return;
        }

        $scope.creatingRetentionPolicy = true;
        $scope.errorMessage = '';

        $http.post(ctx + '/api/storage/retention-policies', {
            fileTypeName: policy.fileTypeName,
            fileExtension: extension,
            retentionDays: retentionDays,
            enabled: !!policy.enabled,
            description: policy.description
        }).then(function (res) {
            $scope.retentionPolicies.push(res.data || {});
            $scope.resetNewRetentionPolicy();
            $scope.showingNewRetentionPolicy = false;
            alert('보관 기간 정책을 추가했습니다.');
        }).catch(function () {
            $scope.errorMessage = '보관 기간 정책을 추가하지 못했습니다. 이미 등록된 확장자인지 확인해주세요.';
        }).finally(function () {
            $scope.creatingRetentionPolicy = false;
        });
    };

    $scope.saveRetentionPolicy = function (policy) {
        if (!policy || $scope.savingRetentionPolicyId) {
            return;
        }

        var retentionDays = parseInt(policy.retentionDays, 10);
        if (!retentionDays || retentionDays < 1 || retentionDays > 3650) {
            alert('보관 기간은 1일부터 3650일 사이로 입력해주세요.');
            return;
        }

        $scope.savingRetentionPolicyId = policy.id;
        $scope.errorMessage = '';

        $http.put(ctx + '/api/storage/retention-policies/' + policy.id, {
            retentionDays: retentionDays,
            enabled: !!policy.enabled
        }).then(function (res) {
            angular.extend(policy, res.data || {});
            alert('보관 기간 설정을 저장했습니다.');
        }).catch(function () {
            $scope.errorMessage = '보관 기간 설정을 저장하지 못했습니다.';
        }).finally(function () {
            $scope.savingRetentionPolicyId = null;
        });
    };

    $scope.deleteRetentionPolicy = function (policy) {
        if (!policy || $scope.deletingRetentionPolicyId) {
            return;
        }
        if (!confirm('확장자 "' + policy.fileExtension + '" 보관 기간 정책을 삭제하시겠습니까?')) {
            return;
        }

        $scope.deletingRetentionPolicyId = policy.id;
        $scope.errorMessage = '';

        $http.delete(ctx + '/api/storage/retention-policies/' + policy.id)
            .then(function () {
                $scope.retentionPolicies = $scope.retentionPolicies.filter(function (item) {
                    return item.id !== policy.id;
                });
                alert('보관 기간 정책을 삭제했습니다.');
            })
            .catch(function () {
                $scope.errorMessage = '보관 기간 정책을 삭제하지 못했습니다.';
            })
            .finally(function () {
                $scope.deletingRetentionPolicyId = null;
            });
    };

    $scope.selectDevice = function (device) {
        $scope.selectedDevice = device;
        $scope.currentPath = device.path;
        $scope.pathHistory = [];
        $scope.loadFiles(device.path);
    };

    $scope.loadFiles = function (path) {
        if (!path) {
            $scope.currentPath = '';
            $scope.files = [];
            $scope.clearSelection();
            return;
        }

        $scope.loadingFiles = true;
        $scope.errorMessage = '';

        $http.get(ctx + '/api/storage/files', { params: { path: path } })
            .then(function (res) {
                $scope.currentPath = path;
                $scope.files = res.data || [];
                $scope.clearSelection();
            })
            .catch(function () {
                $scope.files = [];
                $scope.clearSelection();
                $scope.errorMessage = '폴더 목록을 조회하지 못했습니다.';
            })
            .finally(function () {
                $scope.loadingFiles = false;
            });
    };

    $scope.openFolder = function (file) {
        if (!file || !file.directory || $scope.loadingFiles || $scope.deletingFiles) {
            return;
        }

        $scope.pathHistory.push($scope.currentPath);
        $scope.loadFiles(file.path);
    };

    $scope.goBack = function () {
        if (!$scope.canGoBack()) {
            return;
        }

        var previousPath = $scope.pathHistory.pop();
        $scope.loadFiles(previousPath);
    };

    $scope.canGoBack = function () {
        return $scope.pathHistory.length > 0 && !$scope.loadingFiles && !$scope.deletingFiles;
    };

    $scope.clearSelection = function () {
        $scope.selectedFiles = {};
    };

    $scope.getSelectedFilePaths = function () {
        return Object.keys($scope.selectedFiles).filter(function (path) {
            return $scope.selectedFiles[path];
        });
    };

    $scope.selectedFileCount = function () {
        return $scope.getSelectedFilePaths().length;
    };

    $scope.selectableFiles = function () {
        return $scope.files.filter(function (file) {
            return !file.directory;
        });
    };

    $scope.hasSelectableFiles = function () {
        return $scope.selectableFiles().length > 0;
    };

    $scope.isAllSelected = function () {
        var selectableFiles = $scope.selectableFiles();
        return selectableFiles.length > 0 && selectableFiles.every(function (file) {
            return $scope.selectedFiles[file.path];
        });
    };

    $scope.toggleAllFiles = function ($event) {
        $event.stopPropagation();
        var nextChecked = !$scope.isAllSelected();

        $scope.selectableFiles().forEach(function (file) {
            $scope.selectedFiles[file.path] = nextChecked;
        });
    };

    $scope.deleteSelectedFiles = function () {
        var selectedPaths = $scope.getSelectedFilePaths();
        if (selectedPaths.length === 0 || $scope.deletingFiles) {
            return;
        }

        if (!confirm(selectedPaths.length + '개 파일을 삭제하시겠습니까?')) {
            return;
        }

        $scope.deletingFiles = true;
        $scope.errorMessage = '';

        $http({
            method: 'DELETE',
            url: ctx + '/api/storage/files',
            data: {
                directoryPath: $scope.currentPath,
                filePaths: selectedPaths
            },
            headers: { 'Content-Type': 'application/json' }
        }).then(function (res) {
            var result = res.data || {};
            var message = result.message || '파일 삭제를 완료했습니다.';
            if (result.skippedCount > 0) {
                message += '\n삭제하지 못한 파일: ' + result.skippedCount + '개';
            }
            alert(message);
            $scope.loadFiles($scope.currentPath);
            $scope.refreshSelectedDevice();
        }).catch(function () {
            $scope.errorMessage = '파일을 삭제하지 못했습니다.';
        }).finally(function () {
            $scope.deletingFiles = false;
        });
    };

    $scope.refreshSelectedDevice = function () {
        if (!$scope.selectedDevice) {
            return;
        }

        var selectedPath = $scope.selectedDevice.path;
        $http.get(ctx + '/api/storage/devices').then(function (res) {
            $scope.devices = res.data || [];
            $scope.devices.some(function (device) {
                if (device.path === selectedPath) {
                    $scope.selectedDevice = device;
                    return true;
                }
                return false;
            });
        });
    };

    $scope.isSelected = function (device) {
        return $scope.selectedDevice && $scope.selectedDevice.path === device.path;
    };

    $scope.statusLabel = function (status) {
        if (status === 'danger') {
            return '위험';
        }
        if (status === 'warning') {
            return '주의';
        }
        return '정상';
    };

    $scope.deleteModeLabel = function (mode) {
        if (mode === 'AUTO') {
            return '자동';
        }
        if (mode === 'MANUAL') {
            return '수동';
        }
        return mode || '-';
    };

    $scope.targetTypeLabel = function (type) {
        if (type === 'FILE') {
            return '파일';
        }
        if (type === 'RULE') {
            return '분류기준';
        }
        return type || '-';
    };

    $scope.targetConditionText = function (target) {
        var conditions = [];
        if (target.filePattern) {
            conditions.push('패턴 ' + target.filePattern);
        }
        if (target.fileExtension) {
            conditions.push('확장자 ' + target.fileExtension);
        }
        if (target.minAgeDays !== null && target.minAgeDays !== undefined) {
            conditions.push(target.minAgeDays + '일 경과');
        }
        if (target.minSizeText && target.minSizeText !== '-') {
            conditions.push(target.minSizeText + ' 이상');
        }
        return conditions.length > 0 ? conditions.join(' / ') : '-';
    };

    $scope.resetNewDeleteTarget();
    $scope.resetNewRetentionPolicy();
    $scope.loadDevices();
    $scope.loadDeleteTargets();
    $scope.loadRetentionPolicies();
}]);
