YaCyPage.e; // frequent used elements cache, set by chacheResultElements

YaCyPage.chacheResultElements = function() {
  YaCyPage.e = {
    globalResults: $('#globalResults'),
    itemsCount: $('#itemscount'),
    localResourceSize: $('#localResourceSize'),
    remoteIndexCount: $('#remoteIndexCount'),
    remotePeerCount: $('#remotePeerCount'),
    remoteResourceSize: $('#remoteResourceSize'),
    resNav: $('#resNav'),
    resNavBottom: $('#resNavBottom'),
    resultsOffset: $('#resultsOffset'),
    totalCount: $('#totalcount')
  };
};

YaCyPage.latestInfo = function(eventId) {
  $.ajax({
    url:'yacysearchlatestinfo.json',
    type:'GET',
    data: {
      eventID: eventId
    }
  }).done(function(data) {
    YaCyPage.statistics(data.offset, data.itemscount, data.itemsperpage,
      data.totalcount, data.localResourceSize, data.remoteResourceSize,
      data.remoteIndexCount, data.remotePeerCount, data.navurlBase);
  });
};

YaCyPage.statistics = function(offset, itemscount, itemsperpage, totalcount,
    localResourceSize, remoteResourceSize, remoteIndexCount, remotePeerCount,
    navurlbase) {

  if (totalcount === 0) {
    return;
  }
  if (offset >= 0) {
    YaCyPage.e.resultsOffset.text(offset);
  }
  if (itemscount >= 0) {
    YaCyPage.e.itemsCount.text(itemscount);
  }
  YaCyPage.e.totalCount.text(totalcount);

  if (YaCyPage.e.globalResults.size() > 0) {
    YaCyPage.e.localResourceSize.text(localResourceSize);
    YaCyPage.e.remoteResourceSize.text(remoteResourceSize);
    YaCyPage.e.remoteIndexCount.text(remoteIndexCount);
    YaCyPage.e.remotePeerCount.text(remotePeerCount);
  }
  YaCyPage.e.resNav.text('X');

  // compose page navigation
  var resnav = "";
  var thispage = Math.floor(offset / itemsperpage);
  if (thispage === 0) {
    //resnav += '<span class="left"></span>';
  } else {
    resnav += '<a href="' + navurlbase + "&amp;startRecord="
      + ((thispage - 1) * itemsperpage) + '"><span class="left"></span></a>';
  }

  var numberofpages = Math.floor(Math.min(10, 1 + ((totalcount.replace(/\./g,'') - 1) / itemsperpage)));
  var tabText = 'Use the TAB key to navigate to next page.';

  if (numberofpages) {
    for (i = 0; i < numberofpages; i++) {
      if (i == thispage) {
        resnav += '<span class="current">' + (i + 1) + '</span>';
      } else {
        resnav += '<a href="' + navurlbase + "&amp;startRecord=" +
          (i * itemsperpage) + '" class="page" title="' + tabText + '">' +
          (i + 1) + '</a>';
        }
    }
    if (thispage >= numberofpages -1) {
      //resnav += '<span class="right" title="' + tabText + '"></span>';
    } else {
      resnav += '<a class="iconic" href="' + navurlbase + "&amp;startRecord=" +
        ((thispage + 1) * itemsperpage) + '" title="' + tabText +
        '"><span class="right"></span></a>';
    }

    YaCyPage.e.resNav.html(resnav);
    YaCyPage.e.resNavBottom.html(resnav);
  } else {
    YaCyPage.e.resNav.empty();
    YaCyPage.e.resNavBottom.empty();
  }
};

YaCyPage.EventHandler = function() {
  this.addInteractionHandler = function() {
    // handle navigational keys
    $(document).on('keydown', this.handleArrowKeys);
    // reset search button on changes
    $('#search').on('keydown', function() {
      $('#startSearch').text('Search');
    });
  };

  this.handleArrowKeys = function(evObj) {
    switch (evObj.keyCode) {
      case 9:
      case 33:
        window.location.href = document.getElementById("nextpage").href;
        break;
      case 34:
        window.location.href = document.getElementById("prevpage").href;
        break;
    }
  };
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.eventHandler = new YaCyPage.EventHandler();
  YaCyPage.eventHandler.addInteractionHandler();

  // add autocomplete to search field
  $('#search').autocomplete({
    source: function(request, response) {
      $.ajax({
        url:'/suggest.json',
        type: 'POST',
        data:{
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
      })
    },
    minLength: 2
  });

  // create the topics tag-cloud
  $.fn.tagcloud.defaults = {
    size: {start: 0.8, end: 1.3, unit: 'em'},
    color: {start: '#c08080', end: '#2200CC'}
  };
  //$("#tagCloud a").tagcloud();
  $('#tagCloud a').tsort().tagcloud({
    type:"sphere",
    power:.25,
    seed:0,
    sizemin:10,
    sizemax:20,
    height:80,
    colormin:"682",
    colormax:"20C"
  });

  // show sidebar after it's completely loaded
  $('#searchTrailer').css('visibility', 'visible');

  // search is done now - update stats
  if (YaCyPage.e.resNavBottom.size() === 0) {
    YaCyPage.e.resNavBottom = $('#resNavBottom');
    YaCyPage.e.resNavBottom.html(YaCyPage.e.resNav.clone());
  }
};