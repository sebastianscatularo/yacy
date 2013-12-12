/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.clicked = function(e) {
  $('#key').val(e.id.substring(1));
  $('#value').val(e.value);
};

YaCyPage.filterList = function(value, propName) {
  if (typeof value === 'undefined') {
    YaCyPage.options.removeClass('filtered');
  } else {
    value = value.toLowerCase();
    YaCyPage.options.each(function() {
      if (this[propName].toLowerCase().indexOf(value) == -1) {
        $(this).addClass('filtered');
      } else {
        $(this).removeClass('filtered');
      }
    });
  }
};

YaCyPage.clearForm = function(evObj) {
  $('#key').val('');
  $('#value').val('');
  YaCyPage.filterList();
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.options = $('#options').find('option');

  $('#btnClearForm').on('click', function(evObj) {
    evObj.preventDefault();
    YaCyPage.clearForm();
  });

  $('#key').on('input', function() {
    YaCyPage.filterList($(this).val(), 'id');
  });

  $('#value').on('input', function() {
    YaCyPage.filterList($(this).val(), 'value');
  });

  if ($('#key').val() !== '') {
    YaCyPage.filterList($('#key').val(), 'id');
  } else if ($('#value').val() !== '') {
    YaCyPage.filterList($('#value').val(), 'value');
  }
};