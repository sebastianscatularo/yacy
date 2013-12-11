/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitMultipleDump')
  }).addElement($('#multipleCount'), {
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