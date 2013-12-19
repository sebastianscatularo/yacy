/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page validators. */
YaCyPage.init = function() {
  $('#crawlingURL').on('input', function() {
    $('#engageUrlDelete').prop('disabled', true);
  });
  var validatorUrlDelete = new YaCyUi.Form.Validator({
    toggle: $('#simulateUrlDelete'),
    display: $('#ycu-error-count-url'),
    showLink: false,
    onload: true
  }).addElement($('#crawlingURL'), {
    validators: [{
      type: 'notEmpty'
    }]
  });

  $('#timeDeleteNumber, #timeDeleteUnit').on('change', function() {
    $('#engageTimeDelete').prop('disabled', true);
  });

  $('#indexDeletionCollection').find('input[name="collectiondelete-mode"]')
    .on('change', function() {
      $('#engageCollectionDelete').prop('disabled', true);
    });
  $('#collectionDelete').on('change', function() {
    $('#engageCollectionDelete').prop('disabled', true);
  });

  $('#queryDelete').on('change', function() {
    $('#engageQueryDelete').prop('disabled', true);
  });
  var validatorQueryDelete = new YaCyUi.Form.Validator({
    toggle: $('#simulateQueryDelete'),
    display: $('#ycu-error-count-query'),
    showLink: false,
    onload: true
  }).addElement($('#queryDelete'), {
    validators: [{
      type: 'notEmpty'
    }]
  });
};