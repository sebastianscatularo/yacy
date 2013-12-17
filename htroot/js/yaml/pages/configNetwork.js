/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.checkForRobinson = function() {
  var dist = $('#indexDistribute').prop('checked');
  var recv = $('#indexReceive').prop('checked');

  if (!(dist || recv)) {
    //robinson-mode
    $('#robinson').prop('checked', true);
  } else {
    //p2p-mode
    $('#p2p').prop('checked', true);
  }

  if (recv) {
    $('#indexReceiveSearchOn').prop('checked', true);
  } else {
    $('#indexReceiveSearchOff').prop('checked', true);
  }
};

YaCyPage.enableMode = function() {
  if ($('#p2p').prop('checked')) {
    $('#indexDistribute').prop('checked', true);
    $('#indexReceive').prop('checked', true);
    $('#indexReceiveSearchOn').prop('checked', true);
  } else if ($('#robinson').prop('checked')) {
    $('#indexDistribute').prop('checked', false);
    $('#indexReceive').prop('checked', false);
    $('#indexReceiveSearchOn').prop('checked', false);
  }
};

/** Initialize the page. */
YaCyPage.init = function() {
  var validatorRobinson = new YaCyUi.Form.Validator({
    toggle: $('#submitRobinson'),
    display: $('#ycu-error-count-robinson'),
    showLink: false
  }).addElement($('#clusterPeersYacyDomain'), {
    onload: true,
    validators: [{
      type: 'regEx',
      exp: /^[a-zA-z0-9\-_]+\.yacyh?(,\s*[a-zA-z0-9\-_]+\.yacyh?)*$/,
      error: 'invalid'
    }]
  });

  $('#indexDistribute, #indexReceive').on('click', function() {
    if ($(this).is(':checked')) {
      YaCyPage.checkForRobinson();
    }
  });

  $('#p2p, #robinson').on('click', function() {
    if ($(this).is(':checked')) {
      YaCyPage.enableMode();
    }
  });

  $('#addCustomUrl').on('click', function(evObj) {
    evObj.preventDefault();
    $('#networkDefinitionURLfield').show();
    $(this).hide();
  });
};