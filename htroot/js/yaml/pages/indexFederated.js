/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page validators. */
YaCyPage.init = function() {
  $('#coreServiceFulltext').on('click', function() {
    if ($(this).is(':checked') === false) {
      $('#solrIndexingSolrRemote').prop('checked', true);
    }
  });
  $('#solrIndexingSolrRemote').on('click', function() {
    if ($(this).is(':checked') === false) {
      $('#coreServiceFulltext').prop('checked', true);
    }
  });


  var validatorSolrUrls = new YaCyUi.Form.Validator({
    toggle: $('#submitSolr'),
    display: $('#ycu-error-count-solr-url'),
    onload: true
  }).addElement($('#solrIndexingUrl'), {
    validators: [{
      type: 'notEmpty',
      error: 'invalid'
    }, {
      type: 'url',
      error: 'invalid',
      splitExp: /,/
    }, {
      type: 'urlProtocol',
      protocols: ['https?'],
      splitExp: /,/,
      error: 'protocol'
    }]
  });
};