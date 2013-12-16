/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  var tableRows = $('#latestRequests tbody tr');
  if (tableRows.length === 0) {
    $('#partsearchEventPicture').hide();
  }
};