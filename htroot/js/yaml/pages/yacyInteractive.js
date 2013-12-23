/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.conf = {
  apiLink: '/solr/select?hl=false&wt=opensearch&facet=true&facet.mincount=1&facet.field=url_file_ext_s&start=0&rows=10&query=',
  currentQuery: '',
  xhr: null
};

YaCyPage.conf.elements = {};

YaCyPage.conf.templates = {
  imagesFound: '<em>Found <code>%NUM%</code> images, preparing&hellip;</em>',
  documentsFound: '<em>Found <code>%NUM%</code> documents, preparing&hellip;</em>',
  noDocumentsFound: '<em>Found <code>no</code> documents matching your query.</em>',
  numResults: '<code>%NUM%</code> results from a total of <code>%TOTAL%</code> docs in index; search time: <code>%DUR%</code> milliseconds.',
  maxRecordsLink: '<button class="plain" value="%NUM%">%NUM%</button>',
  listResult: '<td>%PROTOCOL%</td><td class="breakAll"><a href="%PROTOCOL%://%HOST%/">%HOST%</a></td><td class="breakAll"><a href="%PROTOCOL%://%HOST%%PATH%">%PATH%</a></td><td class="breakAll"><a href="%LINK%">%LINK%</a></td><td>%SIZE%</td><td>%PD%</td>',
  extNavLink: '<span class="facet"><a href="#" class="icon-add" data-type="%FTYPE%">%FTYPE%</a> <span>(%COUNT%)</span></span>',
  thumbnail: '<div class="thumbContainer breakAll" style="background-image:url(\'%URL%\');"><span class="content"><a class="imgLink" href="/ViewImage.png?maxwidth=96&amp;maxheight=96&amp;code=%GUID%&amp;url=%URL%">Cache Info</a><a href="%URL%"><em>%TITLE%</em></a></span></div>'
};

YaCyPage.setQuery = function(query) {
  $('#search').val(query);
  YaCyPage.search(query, YaCyPage.conf.maximumRecords, YaCyPage.conf.startRecord);
};

YaCyPage.search = function(query, maximumRecords, startRecord) {
  $('#searchNavigation, #searchFacetNavigation, #searchResults, #downloadScript, #serverList').hide();
  if (query == null || query.trim() == '') {
    return;
  }

  YaCyPage.conf.searchStart = new Date();
  YaCyPage.conf.currentQuery = query;
  YaCyPage.conf.maximumRecords = maximumRecords ||
    YaCyPage.conf.elements.maximumRecords.val() || 100;
  YaCyPage.conf.maximumRecords = Number(YaCyPage.conf.maximumRecords);
  YaCyPage.conf.startRecord = typeof startRecord === 'undefined' ?
    YaCyPage.conf.elements.startRecord.val() : startRecord || 0;
  YaCyPage.conf.startRecord = Number(YaCyPage.conf.startRecord);

  $('#apiLink').attr('href', YaCyPage.conf.apiLink + encodeURIComponent(
    $('#search').val()));
  $('#searchInfo').html('<em>Loading from local index&hellip;</em>');

  if (YaCyPage.conf.xhr) {
    // abort any (may be) running requests
    YaCyPage.conf.xhr.abort();
  }
  YaCyPage.conf.xhr = $.ajax({
    type: 'GET',
    url: '/solr/select',
    dataType: 'text', // json is invalid so we must parse manual here
    data: {
      hl: false,
      wt: 'yjson',
      facet: true,
      'facet.mincount': 1,
      'facet.field': 'url_file_ext_s',
      start: YaCyPage.conf.startRecord,
      rows: YaCyPage.conf.maximumRecords,
      query: YaCyPage.conf.currentQuery
    }
  }).done(
    function(response) {
      response = eval("(" + response + ")");
      $('#searchNavigation, #searchFacetNavigation, #searchResults, #downloadScript, #serverList').show();
      YaCyPage.handleResponse(response.channels[0]);
    }
  );
};

YaCyPage.navGet = function(list, name) {
  for (var i = 0; i < list.length; i++) {
    if (list[i].facetname == name) return list[i];
  }
};

YaCyPage.handleResponse = function(channel) {
  $('#searchInfo').html('<em>Parsing results&hellip;</em>');

  YaCyPage.conf.totalResults = Number(channel.totalResults.replace(/[,.]/, ""));
  YaCyPage.conf.resultsCount = Number(channel.items.length);

  var modifierType = '';
  // any filter already set
  if (YaCyPage.conf.currentQuery.toLowerCase().indexOf('filetype:') > -1) {
    modifierType = YaCyPage.conf.currentQuery.match(/.*\+filetype:(\S+).*/i)[1];
    $('#searchFacetNavigation .control').html(
      $('<a href="#" class="icon-delete">Remove filter.</a>').on('click', function() {
        YaCyPage.setQuery(YaCyPage.conf.currentQuery.replace(/\+filetype:\S+/g, '').trim());
      })
    );
  } else {
    $('#searchFacetNavigation .control').empty();
  }

  // hide previously filled elements
  $('#serverList').empty();
  $('#downloadScript .content, #searchFacetNavigation .facet').hide();

  if (channel.items.length === 0) {
    // no results - stop here
    $('#searchInfo').html(YaCyPage.conf.templates.noDocumentsFound);
    $('#downloadScript, #searchResults, #searchNavigation').hide();
    return;
  }

  // clear any previous results
  $('#searchResults tbody').empty();

  $('#searchInfo').html(
    YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.documentsFound, {
      num: channel.items.length
    }));

  $('#downloadScript, #searchResults').show();

  var fileTypeFacet = YaCyPage.navGet(channel.navigation, "filetypes");
  if (fileTypeFacet) {
    // add extension navigation
    var hasExtNav = false;
    var queryLc = YaCyPage.conf.currentQuery.toLowerCase();

    $('#searchFacetNavigation .facet span').remove();
    $('#thumbList').empty();

    for (var fc = 0; fc < fileTypeFacet.elements.length; fc++) {
      var name = fileTypeFacet.elements[fc].name;
      var count = fileTypeFacet.elements[fc].count;
      // filetype has results & is not already filetered by
      if (count > 0 && queryLc.indexOf("filetype:" + name) == -1) {
        hasExtNav = true;
        $('#searchFacetNavigation .facet[data-id="fileType"]').append(
          $(YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.extNavLink, {
            ftype: name,
            count: count
          })).on('click', function(evObj) {
            evObj.preventDefault();
            YaCyPage.setQuery(YaCyPage.conf.currentQuery.trim() + ' +filetype:' +
              $(this).find('a').attr('data-type'));
          })
        );
      }
    }
    if (hasExtNav) {
      $('#searchFacetNavigation .facet').show();
    }
  }

  if (/(jpe?g|gif|png)/i.test(modifierType)) {
    for (var i = 0; i < channel.items.length; i++) {
      if (channel.items[i] === null || channel.items[i].link === null) {
        continue;
      }
      YaCyPage.addResultImage(channel.items[i]);
    }
    $('#thumbList').show();
  } else {
    for (var i = 0; i < channel.items.length; i++) {
      if (channel.items[i] === null || channel.items[i].link === null) {
        continue;
      }
      YaCyPage.addResultItem(channel.items[i]);
    }
    $('#searchResults table').show();
  }

  YaCyPage.resultNavigation();
};

YaCyPage.resultNavigation = function() {
  $('#searchNavigation button').remove();

  if (YaCyPage.conf.resultsCount > YaCyPage.conf.totalResults) {
    YaCyPage.conf.totalResults = YaCyPage.conf.resultsCount;
  }

  $('#searchInfo').html(
    YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.numResults, {
      num: YaCyPage.conf.resultsCount,
      total: YaCyPage.conf.totalResults,
      dur: (new Date()).getTime() - YaCyPage.conf.searchStart.getTime()
    }));

  for (var i = 10; i < YaCyPage.conf.totalResults; i = i * 10) {
    if (YaCyPage.conf.maximumRecords != i && YaCyPage.conf.totalResults >= i) {
      $('#searchNavigation').append(
        $(YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.maxRecordsLink, {
          num: i
        })).on('click', function() {
          YaCyPage.search(YaCyPage.conf.currentQuery, $(this).attr('value'),
            YaCyPage.conf.startRecord);
        })
      );
    }
  }
  if (YaCyPage.conf.maximumRecords < YaCyPage.conf.totalResults) {
    $('#searchNavigation').append(
      $(YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.maxRecordsLink, {
        num: YaCyPage.conf.totalResults
      })).on('click', function() {
        YaCyPage.search(YaCyPage.conf.currentQuery, YaCyPage.conf.totalResults, YaCyPage.conf.startRecord);
      })
    );
  }
};

YaCyPage.getItemInfo = function(item) {
  var info = {};
  info.link = document.createElement('a');
  info.link.href = item.link || '';

  info.path = info.link.pathname;
  info.pubDate = item.pubDate || '';
  info.pubDate = info.pubDate.replace(/\s\+0{4}$/, '').replace(/\s00:00:00$/, '');

  if (info.path.lastIndexOf("/")) {
    var idx = info.path.lastIndexOf("/");
    info.file = info.path.substring(idx + 1);
    info.path = unescape(info.path.substring(0, idx + 1));
  } else {
    info.file = info.path;
    info.path = '/';
  }

  info.title = item.title.trim() || info.path;

  return info;
}

YaCyPage.addResultImage = function(item) {
  var info = YaCyPage.getItemInfo(item);

  $('#thumbList').append(YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.thumbnail, {
    url: item.link,
    guid: item.guid,
    title: info.title
  }));
};

YaCyPage.addResultItem = function(item) {
  var info = YaCyPage.getItemInfo(item);

  // update download script
  if (info.link.protocol.toLowerCase() == 'smb:') {
    $('#downloadScript .content').append('smbget -n -a -r "' + item.link + '"\n');
  } else {
    $('#downloadScript .content').append('curl -OL "' + item.link + '"\n');
  }

  $('#searchResults tbody').append('<tr>' +
    YaCyUi.Tools.tplReplace(YaCyPage.conf.templates.listResult, {
      protocol: info.link.protocol.replace(':', ''),
      host: info.link.hostname,
      path: info.path,
      link: item.link,
      size: item.sizename == '-1 byte' ? '<i class="fa fa-question"></i>' : item.sizename,
      pd: info.pubDate
    }) + '</tr>');
};

/** Initialize the page. */
YaCyPage.init = function() {
  var typeTimeout = null;
  YaCyPage.conf.elements.maximumRecords = $('#searchForm').find('input[name="maximumRecords"]');
  YaCyPage.conf.elements.startRecord = $('#searchForm').find('input[name="startRecord"]');
  $('#searchFacetNavigation .facets').hide();

  $('#search').on('input', function() {
    if ($('#search').val().length > 0) {
      if ($('#search').val().length >= 3) {
        if (typeTimeout !== null) {
          window.clearTimeout(typeTimeout);
        }
        typeTimeout = window.setTimeout(function() {
          YaCyPage.search($('#search').val());
        }, 500);
      }
    }
  });

  $('#downloadScript button[data-id="create"]').on('click', function() {
    $('#downloadScript .content').show().height(
      $('#downloadScript .content')[0].scrollHeight);
    // keep position
    window.location.hash = '#downloadScript';
  });

  // add autocomplete to search field
  $('#search').autocomplete({
    source: function(request, response) {
      $.ajax({
        url: '/suggest.json',
        type: 'POST',
        data: {
          query: request.term
        },
        success: function(data) {
          response(
            $.map(data[1], function(item) {
              return {
                label: item
              };
            })
          );
        }
      });
    },
    minLength: 2
  });
};