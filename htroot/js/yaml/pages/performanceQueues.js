/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  var validatorCacheSize = new YaCyUi.Form.Validator({
    toggle: $('#submitCacheSize'),
    display: $('#ycu-error-count-cache-size'),
    showLink: false
  }).addElement($('#wordCacheMaxCount'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number'
    }]
  });

  var validatorPoolConfig = new YaCyUi.Form.Validator({
    toggle: $('#submitPoolConfig'),
    display: $('#ycu-error-count-pool-config'),
    showLink: false
  }).addElement($('input[name$=_maxActive]'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number'
    }]
  });
};