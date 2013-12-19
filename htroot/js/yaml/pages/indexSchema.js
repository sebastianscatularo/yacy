/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page validators. */
YaCyPage.init = function() {
  $('#core').on('change', function() {
    $('#schemaSelectForm').submit();
  });
};