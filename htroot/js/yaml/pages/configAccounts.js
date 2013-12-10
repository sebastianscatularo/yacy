/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page. */
YaCyPage.init = function() {
  var validatorAdmin = new YaCyUi.Form.Validator({
    toggle: $('#submitAccount'),
    display: $('#ycu-error-count-admin'),
    showLink: false
  }).addElement($('#adminUser, #adminPw1, #adminPw2'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }]
  }).addElement($('#adminUser'), {
    onload: true,
    validators: [{
      type: 'length',
      min: 4,
      error: 'length'
    }]
  }).addElement($('#adminPw2'), {
    onload: false,
    validators: [{
      type: 'same',
      element: '#adminPw1',
      error: 'notSame'
    }]
  });

  $('#adminUser, #adminPw1, #adminPw2').on('focus', function() {
    $('#access_account').prop('checked', true);
  });

  var validatorUser = new YaCyUi.Form.Validator({
    toggle: $('#submitUser'),
    display: $('#ycu-error-count-user'),
    onload: true
  }).addElement($('#userName, #password, #password2'), {
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }]
  }).addElement($('#userName'), {
    onload: false,
    validators: [{
      type: 'length',
      min: 4,
      error: 'length'
    }]
  }).addElement($('#password2'), {
    validators: [{
      type: 'same',
      element: '#password',
      error: 'notSame'
    }]
  });
};