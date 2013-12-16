/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  if ($('#networkPicture').length > 0) {
    new YaCyUi.Tools.PictureAnimation({
      imageStub: 'NetworkPicture.png?width=1024&amp;height=720&amp;bgcolor=FFFFFF&amp;ct=15000&amp;coronaangle=',
      element: $('#networkPicture')
    });
  }

  if ($('#submitSearch').length > 0) {
    var validator = new YaCyUi.Form.Validator({
      toggle: $('#submitSearch')
    }).addElement($('#match'), {
      onload: true,
      validators: [{
        type: 'notEmpty'
      }]
    });
  }
};