/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  if ($('#author').length > 0) {
    var validatorAuthor = new YaCyUi.Form.Validator({
      onload: true
    }).addElement($('#author'), {
      validators: [{
        type: 'notEmpty',
        failType: 'warning'
      }]
    });
  }

  $('a[data-id="wikiHelp"]').on('click', function(evObj) {
    evObj.preventDefault();
    YaCyUi.Tools.openWindow('WikiHelp.html', 'WikiHelp');
  });
};