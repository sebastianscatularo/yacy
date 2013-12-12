/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.parseResponse = function(response) {
  var total = response.totalResults.replace(/[,.]/, "");
  var table = $('#searchResults').find('tbody').empty();
  var header = $('#searchResults').find('legend').empty();
  header.text(total + ' results');
  if (total > 0 && response.items.length > 0) {
    $('#searchResults').show();
    for (var i = 0; i < response.items.length; i++) {
      console.debug(response.items[i]);
      table.append('<tr><td class="breakAll"><a href="HostBrowser.html?pathsearch=&amp;path=' +
        response.items[i].link + '">' + response.items[i].link + '</a></td></tr>');
    }
  } else {
    $('#searchResults').hide();
  }
};

YaCyPage.search = function(query) {
  console.debug('search', query);
  var searchStringTemplate = 'sku:%Q% OR host_s:%Q% OR host_dnc_s:%Q% OR host_organization_s:%Q% OR host_organizationdnc_s:%Q% OR host_subdomain_s:%Q% OR url_paths_sxt:%Q% OR url_file_name_s:%Q%';
  $.ajax({
    method: 'GET',
    url: '/solr/select',
    data: {
      q: searchStringTemplate.replace(/%Q%/g, query),
      start: 0,
      rows: 100,
      wt: 'yjson'
    }
  }).done(
    function(response) {
      if ('channels' in response) {
        YaCyPage.parseResponse(response.channels[0]);
      }
    }
  );
};

/** Initialize the page validators. */
YaCyPage.init = function() {
  var typeTimeout = null;
  var searchEntry = $('#searchPath');

  var queryParam = YaCyUi.Tools.getUrlParameters();
  if ('path' in queryParam) {
    $('#submitDelete')
      .html('Delete subpath <code>' + queryParam.path + '</code>')
      .on('click', function() {
        return confirm('Please confirm the subpath deletion of \'' + queryParam.path + '\'.');
      });
    $('#subpathDeleteSection').show();
  }

  searchEntry.on('input', function() {
    if (searchEntry.val().length > 0) {
      if (searchEntry.val().length >= 3) {
        if (typeTimeout !== null) {
          window.clearTimeout(typeTimeout);
        }
        typeTimeout = window.setTimeout(function() {
          YaCyPage.search(searchEntry.val());
        }, 500);
      }
    }
  });
};