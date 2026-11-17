'use strict';

qosApp.component('errorCodeBadge', {
    bindings: { entry: '<', onOpen: '&?' },
    controller: ['$location', function ($location) {
        this.openHelp = function (event) {
            event.stopPropagation();
            if (this.onOpen) this.onOpen();
            $location.path('/help').search({code: this.entry.code}).hash('');
        };
    }],
    template: '<span class="log-code-with-help" ng-if="$ctrl.entry">' +
        '<code class="log-code">{{$ctrl.entry.code}}</code>' +
        '<button type="button" class="log-help-icon" ng-click="$ctrl.openHelp($event)" ' +
        'ng-attr-title="{{$ctrl.entry.title + \'\\n\' + $ctrl.entry.description + \'\\n\' + $ctrl.entry.check}}" ' +
        'ng-attr-aria-label="{{$ctrl.entry.code}} 도움말, {{$ctrl.entry.title}}">?</button></span>'
});
