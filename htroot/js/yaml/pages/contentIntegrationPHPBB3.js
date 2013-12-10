/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page validators. */
YaCyPage.init = function() {
  // init parts
  var validatorExport = new YaCyUi.Form.Validator({
    toggle: $('#submitCheck, #submitExport'),
    display: $('#ycuErrorCountExport'),
    onload: true
  }).addElement($('#urlStub'), {
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'url',
      error: 'invalid'
    }, {
      type: 'urlProtocol',
      protocols: ['https?'],
      error: 'protocol'
    }]
  }).addElement($('#postsPerFile'), {
    validators: [{
      type: 'range',
      min: $('#postsPerFile').data('min')
    }]
  }).addElement($('#dbHost'), {
    validators: [{
      type: 'notEmpty'
    }]
  }).addElement($('#dbPort'), {
    validators: [{
      type: 'number'
    }]
  }).addElement($('#dbName, #dbUser, #dbPw'), {
    validators: [{
      type: 'notEmpty'
    }]
  });

  var validatorImport = new YaCyUi.Form.Validator({
    toggle: $('#submitImport'),
    display: $('#ycuErrorCountImport'),
    onload: true,
    showLink: false
  });
  validatorImport.addElement($('#dumpFile'), {
    validators: [{
      type: 'notEmpty'
    }]
  });
};