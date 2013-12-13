/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitScan'),
    display: $('#ycu-error-count'),
    onload: true
  }).addElement($('#scanHosts'), {
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'ipv4',
      error: 'invalid'
    }]
  }).addElement($('#timeoutMs'), {
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number',
      error: 'invalid'
    }, {
      type: 'range',
      min: 0,
      error: 'range'
    }]
  });

  $('#scanForm').on('submit', function() {
    $('#scanhostinfo').text('Please wait&hellip;');
    $('#submitScan').prop('disabled', true);
  });

  $('#scheduleUse').on('click', function() {
    $('#accumulateScanCache').prop('checked', false);
  });
};