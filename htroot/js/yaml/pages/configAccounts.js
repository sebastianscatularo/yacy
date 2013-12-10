/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitAccount'),
    display: $('#ycu-error-count'),
  }).addElement($('#adminUser, #adminPw1, #adminPw2'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }]
  }).addElement($('#adminPw2'), {
    onload: false,
    validators: [{
      type: 'same',
      element: '#adminPw1',
      error: 'notSame'
    }]
  });
};