/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitQuery'),
    display: $('#ycu-error-count'),
    onload: true,
    showLink: false
  }).addElement($('#query'), {
    validators: [{
      type: 'notEmpty'
    }]
  });

  $('#query').on('focus', function() {
    this.select();
  });
};