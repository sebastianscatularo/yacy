/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page validators. */
YaCyPage.init = function() {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitSave'),
    display: $('#ycuErrorCount'),
    onload: true,
    showLink: false
  }).addElement($('#acceptCrawlLimit'), {
    validators: [{
      type: 'notEmpty'
    }, {
      type: 'range',
      min: $('#acceptCrawlLimit').data('min'),
      max: $('#acceptCrawlLimit').data('max')
    }]
  });
};