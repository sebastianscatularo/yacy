/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.setFields = function() {
  if ($('#instantShallowCrawl').is(':checked')) {
    $('#instantShallowCrawlValue').attr('name', 'site_on');
  } else {
    $('#instantShallowCrawlValue').attr('name', 'site_off');
  }

  if ($('#shallowCrawlSearchResults').is(':checked')) {
    $('#shallowCrawlSearchResultsValue').attr('name', 'searchresult_on');
  } else {
    $('#shallowCrawlSearchResultsValue').attr('name', 'searchresult_off');
  }

  if ($('#searchResultGlobal').is(':checked')) {
    $('#searchResultGlobalValue').attr('name', 'searchresultglobal_on');
  } else {
    $('#searchResultGlobalValue').attr('name', 'searchresultglobal_off');
  }

  if ($('#twitterCheck').is(':checked')) {
    $('#twitterCheckValue').attr('name', 'twitter_on');
  } else {
    $('#twitterCheckValue').attr('name', 'twitter_off');
  }

  if ($('#blekkoCheck').is(':checked')) {
    $('#blekkoCheckValue').attr('name', 'blekko_on');
  } else {
    $('#blekkoCheckValue').attr('name', 'blekko_off');
  }

  if ($('#openSearchCheck').is(':checked')) {
    $('#openSearchCheckValue').attr('name', 'opensearch_on');
  } else {
    $('#openSearchCheckValue').attr('name', 'opensearch_off');
  }
};

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#addNewOsd')
  }).addElement($('#ossysNewUrl'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'url',
      error: 'invalid'
    }, {
      type: 'urlProtocol',
      protocols: ['https?'],
      error: 'protocol'
    }]
  }).addElement($('#ossysNewTitle'), {
    onload: true,
    validators: [{
      type: 'notEmpty'
    }]
  });

  $('#instantShallowCrawl, #shallowCrawlSearchResults, #searchResultGlobal, #twitterCheck, #blekkoCheck, #openSearchCheck').on('change', function() {
    YaCyPage.setFields();
  });

  $('#discoverOsd').on('click', function() {
    return confirm('start background task, depending on index size this may run a long time');
  });

  $('#switchSolrFieldsOn').on('click', function() {
    return confirm('Modify Solr Schema?');
  });

  YaCyPage.setFields();
};