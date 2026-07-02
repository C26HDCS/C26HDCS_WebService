'use strict';

qosApp.factory('http_method', ['$http', function ($http) {

    var http_method = {};

    http_method.getMessage = function (url, param, success, fail) {
        $http({
            headers: { 'Content-type': 'application/json; charset=UTF-8' },
            method: 'GET',
            url: url,
            params: param
        }).then(success, fail);
    };

    http_method.postMessage = function (url, param, success, fail) {
        $http({
            headers: { 'Content-type': 'application/json; charset=UTF-8' },
            method: 'POST',
            url: url,
            data: param
        }).then(success, fail);
    };

    http_method.putMessage = function (url, param, success, fail) {
        $http({
            headers: { 'Content-type': 'application/json; charset=UTF-8' },
            method: 'PUT',
            url: url,
            data: param
        }).then(success, fail);
    };

    http_method.deleteMessage = function (url, param, success, fail) {
        $http({
            headers: { 'Content-type': 'application/json; charset=UTF-8' },
            method: 'DELETE',
            url: url,
            params: param
        }).then(success, fail);
    };

    http_method.formMessage = function (url, param, success, fail) {
        $http({
            headers: { 'Content-type': undefined },
            method: 'POST',
            url: url,
            data: param
        }).then(success, fail);
    };

    return http_method;
}]);
