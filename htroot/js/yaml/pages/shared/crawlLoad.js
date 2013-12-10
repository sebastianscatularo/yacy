/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page validators. */
YaCyPage.addValidators = function(toggles) {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: toggles || $('#submitCrawlStart'),
    display: $('#ycuErrorCount'),
    onload: true,
    showLink: false
  }).addElement($('#crawlingURL'), {
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
  });
};