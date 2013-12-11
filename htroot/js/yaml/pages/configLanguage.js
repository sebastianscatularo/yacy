/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#startDownloadLang'),
    display: $('#ycu-error-count')
  }).addElement($('#urlInstall'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'url',
      error: 'invalid'
    }]
  });

  // hide language options, if there are no languages
  if ($('#langList').find('option').length === 0) {
    $('#langListGroup, #langListControls').hide();
  }
};