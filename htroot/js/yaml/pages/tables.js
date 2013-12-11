/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.editCheckedCount = 0; // count of checked elements in edit table

YaCyPage.setTableEditButtons = function() {
  if (YaCyPage.editCheckedCount > 0) {
    $('#submitDeleteRows, #submitEditRows').prop('disabled', false);
  } else {
    $('#submitDeleteRows, #submitEditRows').prop('disabled', true);
  }
};

/** Initialize the page. */
YaCyPage.init = function() {
  $('#api').find('a.api').attr('href', '/api/table_p.xml' + window.location.search);

  $('#tableAdminSelect, #tableAdminCount').on('change', function() {
    $('#tableAdmin').submit();
  });

  $('#editTable').find('tbody input[type="checkbox"]').on('change', function() {
    if ($(this).is(':checked')) {
      YaCyPage.editCheckedCount++;
    } else {
      YaCyPage.editCheckedCount--;
    }
    YaCyPage.setTableEditButtons();
  });

  YaCyPage.setTableEditButtons();
};