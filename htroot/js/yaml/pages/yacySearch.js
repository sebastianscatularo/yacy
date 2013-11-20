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
    resultsOffset: $('#resultsOffset'),
    totalCount: $('#totalcount')
  };
};

YaCyPage.statistics = function(offset, itemscount, itemsperpage, totalcount, localResourceSize, remoteResourceSize, remoteIndexCount, remotePeerCount, navurlbase) {
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
  resnav = "";
  thispage = Math.floor(offset / itemsperpage);
  if (thispage == 0) {
    resnav += ("<img src=\"env/grafics/navdl.gif\" alt=\"arrowleft\" width=\"16\" height=\"16\" />&nbsp;");
  } else {
    resnav += ("<a id=\"prevpage\" href=\"");
      resnav += (navurlbase + "&amp;startRecord=" + ((thispage - 1) * itemsperpage));
    resnav += ("\"><img src=\"env/grafics/navdl.gif\" alt=\"arrowleft\" width=\"16\" height=\"16\" /></a>&nbsp;");
  }

  numberofpages = Math.floor(Math.min(10, 1 + ((totalcount.replace(/\./g,'') - 1) / itemsperpage)));
  if (!numberofpages) numberofpages = 10;
  for (i = 0; i < numberofpages; i++) {
      if (i == thispage) {
         resnav += "<img src=\"env/grafics/navs";
         resnav += (i + 1);
         resnav += (".gif\" alt=\"page");
         resnav += (i + 1);
         resnav += ("\" width=\"16\" height=\"16\" />&nbsp;");
      } else {
         resnav += ("<a href=\"");
         resnav += (navurlbase + "&amp;startRecord=" + (i * itemsperpage));
         resnav += ("\"><img src=\"env/grafics/navd");
         resnav += (i + 1);
         resnav += (".gif\" alt=\"page");
         resnav += (i + 1);
         resnav += ("\" title=\"use the TAB key to navigate to next page\" width=\"16\" height=\"16\" /></a>&nbsp;");
      }
  }
  if (thispage >= numberofpages) {
    resnav += ("<img src=\"env/grafics/navdr.gif\" alt=\"arrowright\" title=\"use the TAB key to navigate to next page\" width=\"16\" height=\"16\" />");
  } else {
      resnav += ("<a id=\"nextpage\" href=\"");
      resnav += (navurlbase + "&amp;startRecord=" + ((thispage + 1) * itemsperpage));
      resnav += ("\"><img src=\"env/grafics/navdr.gif\" alt=\"arrowright\" title=\"use the TAB key to navigate to next page\" width=\"16\" height=\"16\" /></a>");
  }

  YaCyPage.e.resNav.html(resnav);
};

YaCyPage.EventHandler = function() {
  this.addInteractionHandler = function() {
    // handle navigational keys
    $(document).on('keydown', this.handleArrowKeys);
    $('#search').on('keydown', function() {
      $('#startSearch').text('Search');
    });
  };

  this.handleArrowKeys = function(evObj) {
    switch (evObj.keyCode) {
      //case 9:
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
};