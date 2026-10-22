// "View in Avni" link on the Bahmni patient dashboard (JSS Ganiyari).
// Append to openmrs/apps/customDisplayControl/js/customControl.js in the JSS Bahmni config.
// The Avni integration stores the Avni subject UUID on the patient (person attribute "avni_subject_uuid");
// this control turns it into a link to the person's page in the Avni web app.
angular.module('bahmni.common.displaycontrol.custom')
    .directive('avniLink', ['$http', 'appService', 'spinner', function ($http, appService, spinner) {
        var link = function ($scope, element) {
            var config = $scope.config || {};
            var attributeTypeUuid = config.avniSubjectUuidAttributeTypeUuid || 'ceddfccf-7913-4a63-b60e-d3114f867e4b';
            var avniWebAppUrl = (config.avniWebAppUrl || 'https://app.avniproject.org').replace(/\/$/, '');
            $scope.contentUrl = appService.configBaseUrl() + '/customDisplayControl/views/avniLink.html';
            spinner.forPromise($http.get('/openmrs/ws/rest/v1/person/' + $scope.patient.uuid, {
                params: {v: 'custom:(attributes:(value,voided,attributeType:(uuid)))'}
            }).then(function (response) {
                var attribute = (response.data.attributes || []).filter(function (a) {
                    return !a.voided && a.attributeType && a.attributeType.uuid === attributeTypeUuid;
                })[0];
                $scope.avniUrl = attribute ? avniWebAppUrl + '/#/app/subject?uuid=' + encodeURIComponent(attribute.value) : null;
                $scope.loaded = true;
            }), element);
        };
        return {
            restrict: 'E',
            link: link,
            template: '<ng-include src="contentUrl"/>'
        };
    }]);
