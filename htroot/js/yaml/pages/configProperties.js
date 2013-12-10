/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.clicked = function(e) {
  $('#key').val(e.id.substring(1));
  $('#value').val(e.value);
};

YaCyPage.filterList = function() {
  var key = $('#key').val();

  YaCyPage.options.each(function() {
    if ($(this).val().toLowerCase().indexOf(key.toLowerCase()) == -1) {
      $(this).addClass('filtered');
    } else {
      $(this).removeClass('filtered');
    }
  });
};

YaCyPage.clearForm = function() {
  $('#key').val('');
  $('#value').val('');
  YaCyPage.filterList();
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.options = $('#options').find('option');
  YaCyPage.filterList();
};