(function() {
	'use strict';

	angular.module('opennms.services.BuildConfig', [])
		.constant('config.build.billingKey', '12345')
		.constant('config.build.minSdk', 21)
		.constant('config.build.version', '2.1.0')
		.constant('config.build.build', 395)
		.constant('config.build.debug', true)
	;

}());