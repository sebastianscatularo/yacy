/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.search = function(query) {
  var queryString = 'sku:%Q% OR host_s:%Q% OR host_dnc_s:%Q% OR host_organization_s:%Q% OR host_organizationdnc_s:%Q% OR host_subdomain_s:%Q%';
  $.ajax({
    type: 'GET',
    url: '/solr/select',
    data: {
      q: queryString.replace(/%Q%/g, query),
      start: 0,
      rows: 100,
      wt: 'yjson'
    }
  }).done(
    function(response) {
      YaCyPage.parseResponse(response.channels[0]);
    }
  );
};

YaCyPage.parseResponse = function(response) {
  var total = response.totalResults.replace(/[,.]/, "");
  var table = $('#searchResults').find('tbody').empty();
  var header = $('#searchResults').find('legend').empty();
  header.text(total + ' results');
  if (total > 0 && response.items.length > 0) {
    $('#searchResults').show();
    for (var i = 0; i < response.items.length; i++) {
      console.debug(response.items[i]);
      table.append('<tr><td class="breakAll">' + response.items[i].link + '</td>' +
        '<td><a href="ViewFile.html?url=' + response.items[i].link + '">Show Metadata</a></td>' +
        '<td><a href="HostBrowser.html?pathsearch=&amp;path=' + response.items[i].link + '">Browse Host</a></td></tr>');
    }
  } else {
    $('#searchResults').hide();
  }
};

/** Initialize the page. */
YaCyPage.init = function() {
  var typeTimeout = null;
  var searchEntry = $('#url');

  $('#browseAll').on('click', function(evObj) {
    evObj.preventDefault();
    window.location.href = '/HostBrowser.html?path=';
  });

  $('#viewMode').on('change', function() {
    $('#detailsForm').submit();
  });

  searchEntry.on('input', function() {
    if (searchEntry.val().length >= 3) {
      if (typeTimeout !== null) {
        window.clearTimeout(typeTimeout);
      }
      typeTimeout = window.setTimeout(function() {
        YaCyPage.search(searchEntry.val());
      }, 500);
    }
  });
};