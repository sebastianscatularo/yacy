/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page. */
YaCyPage.init = function() {
  var validatorStartup = new YaCyUi.Form.Validator({
    toggle: $('#submitStartup'),
    display: $('#ycu-error-count-startup'),
    showLink: false
  }).addElement($('#Xmx'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number'
    }]
  });

  var validatorObserver = new YaCyUi.Form.Validator({
    toggle: $('#submitObserver'),
    display: $('#ycu-error-count-observer'),
    showLink: false
  }).addElement($('#diskFree, #diskFreeHardlimit, #memoryAcceptDHT'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number'
    }]
  });

  var validatorObserver = new YaCyUi.Form.Validator({
    toggle: $('#submitCaution'),
    display: $('#ycu-error-count-caution'),
    showLink: false
  }).addElement($('#crawlPauseProxy, #crawlPauseLocalsearch, #crawlPauseRemotesearch'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number'
    }]
  });

  window.setInterval(function() {
    if ($('#autoReload').is(':checked')) {
      $('#graph').attr('src', 'PerformanceGraph.png?nopeers=&amp;time=' + (new Date()).getTime());
    }
  }, 1000);
};