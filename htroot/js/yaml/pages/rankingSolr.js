/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.setFieldsState = function() {
  if ($('#bf').val().trim().length > 0) {
    $('#submitEnterBF').prop('disabled', false);
  } else {
    $('#submitEnterBF').prop('disabled', true);
  }

  if ($('#bq').val().trim().length > 0) {
    $('#submitEnterBQ').prop('disabled', false);
  } else {
    $('#submitEnterBQ').prop('disabled', true);
  }
};

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitEnterBoosts'),
    display: $('#ycu-error-count')
  }).addElement($('#boostFields input[name^="boost_"]'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      failType: 'warning',
      stopExec: true
    }, {
      type: 'number'
    }, {
      type: 'range',
      min: 0
    }]
  });

  $('#submitEnterBF').prop('disabled', false);

  $('#profile').on('change', function() {
    $('#profileSelectForm').submit();
  });

  $('#bf, #bq').on('input', function() {
    YaCyPage.setFieldsState();
  });

  YaCyPage.setFieldsState();
};