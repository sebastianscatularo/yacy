/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page. */
YaCyPage.init = function(toggles) {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submit'),
    display: $('#ycuErrorCount'),
    onload: true,
    showLink: false
  }).addElement($('#importFile'), {
    validators: [{
      type: 'notEmpty'
    }]
  });
};