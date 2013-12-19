/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.findRDFSource = function() {
  $.getJSON('yacysearch.json?query=rdfdatasource&Enter=Search&verify=never&contentdom=text&nav=hosts%2Cauthors%2Cnamespace%2Ctopics%2Cfiletype%2Cprotocol&startRecord=0&indexof=off&meanCount=5&resource=global&urlmaskfilter=.*&prefermaskfilter=&maximumRecords=10', null, function(data) {
    $('#rdffileslist').val(data.channels[0].items[0].urlname);
  });
}

/** Initialize the page validators. */
YaCyPage.init = function() {
  $('#submitRdfSearch').on('click', function() {
    YaCyPage.findRDFSource();
  });
};