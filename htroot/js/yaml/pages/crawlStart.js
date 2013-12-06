YaCyPage.Func = YaCyPage.Func || {};

YaCyPage.Func.CrawlStart = function() {
  var self = this;
  this.dataLoaded = false;
  // state holding for url list checking
  this.urls = {
    checked: 0,
    count: 0,
    failed: 0, // robots disallowed
    unknown: 0 // robots unknown
  };
  // text strings
  this.text = {
    robotsAllowed: 'yes',
    robotsUnknown: 'unknown',
    robotsDisallowed: 'no - you can not start a crawl for this URL',
    robotsDisallowedShort: 'no',
    startSingleUrl: 'Single URL',
    startUrlBatch: 'URLs batch'
  };
  this.startType = 'single'; // may be single or list, based on start urls

  function init() {};

  init();
};
YaCyPage.Func.CrawlStart.prototype = {
  private: {
    handleResponse: {
      /** Handle the JSON response from getpageinfo.json for a list of URLs
       * @param {object} JSON response object */
      list: function(response, url) {
        console.debug("RSP", response);
        this.urls.checked++;

        if (!('item' in response)) {
          this.urls.failed++;
          return;
        }
        response = response.item;

        // title
        if (this.urls.checked == 1) {
          $('#startPointDetails').find('dd[data-id="bookmarkTitle"]')
            .text(this.text.startUrlBatch + ' ' + this.getDateString());
        }

        // robots
        var robotsInfoList = $('#startPointDetails').find(
          'dd[data-id="robotsAllowed"]').children('ul');
        var robotsInfo = $('<li></li>');
        var favicon = '<div style="width:16px;height:16px;display:inline-block;margin-right:0.3em;"></div>';

        switch (response.robots) {
          case "0":
            robotsInfo.addClass('ycu-data-false');
            this.urls.failed++;
            break;
          case "1":
            robotsInfo.addClass("ycu-data-true");
            break;
          case "2":
            robotsInfo.addClass("ycu-data-unknown");
            this.urls.unknown++;
            break;
          default:
            console.debug("ROBOTS unkwn:", response.robots);
        }

        if (response.favicon.length > 0) {
          favicon = '<img src="' + response.favicon +
            '" style="width:16px;height:16px;margin-right:0.3em;"/>'
        }
        robotsInfo.append('<a href="' + url + '">' + favicon + url + '</a>');
        if (response.robotsInfo.trim().length > 0) {
          robotsInfo.append('<p class="extInfo">' + response.robotsInfo + '</p>');
        }
        robotsInfoList.append(robotsInfo);

        $('#startPointDetails').show('slow');
      },

      /** Handle the JSON response from getpageinfo.json for a single URL
       * @param {object} JSON response object */
      single: function(response, url) {
        console.debug("RSP", response);
        var crawlAllowed = true;
        this.urls.checked++;

        if (!('item' in response)) {
          return;
        }
        response = response.item;

        // title
        var bookmarkTitle = response.title.trim();
        var bookmarkIcon = '';
        if (bookmarkTitle.length === 0) {
          bookmarkTitle = this.text.startSingleUrl + ' ' + this.getDateString();
        }
        if (response.favicon.length > 0) {
          bookmarkIcon = '<img src="' + response.favicon +
            '" style="width:16px;height:16px;margin-right:0.3em;"/>'
        }
        $('#startPointDetails').find('dd[data-id="bookmarkTitle"]')
          .html(bookmarkIcon + bookmarkTitle);

        // robots
        var robotsInfo = $('#startPointDetails').find(
          'dd[data-id="robotsAllowed"]');
        if (response.robots == "0") {
          robotsInfo.addClass("ycu-data-false");
          crawlAllowed = false;
        } else if (response.robots == "2") {
          robotsInfo.addClass("ycu-data-unknown");
        } else {
          robotsInfo.addClass("ycu-data-true");
        }
        robotsInfo.append('<a href="' + url + '">' + url + '</a>');
        if (response.robotsInfo.trim().length > 0) {
          robotsInfo.append('<p class="extInfo">' + response.robotsInfo + '</p>');
        }

        // only if optional elements are defined
        var hasStartPointSelect = $('#startPointSelect').length > 0;

        if (hasStartPointSelect) {
          var option, data;

          // site-list
          option = $('#startPointSelectBox').find('option[data-id="siteList"]');
          data = response.sitemap.trim();
          data.length > 0 ? option.prop('disabled', false) : option.prop(
            'disabled', true);
          this.private.listUrls(data,
            $('#startPointDetails dd[data-id="siteList"]'));

          // link-list
          option = $('#startPointSelectBox').find('option[data-id="linkList"]');
          data = response.links.trim();
          data.length > 0 ? option.prop('disabled', false) :
            option.prop('disabled', true);
          this.private.listUrls(data,
            $('#startPointDetails dd[data-id="linkList"]'));

          if (crawlAllowed && hasStartPointSelect) {
            $('#startPointSelect').show('slow');
          }
        }

        $('#startPointDetails').show('slow');
      }
    },

    /** List URLs checked in handleResponse.list.
     * @param {json} Data
     * @param {Query} Target element */
    listUrls: function(data, targetElement) {
      if (targetElement.length === 0) {
        // target element does not exist
        return;
      }

      if (data.length > 0) {
        var list = data.split(' ');
        var listElement = targetElement
          .empty()
          .addClass("ycu-data-list")
          .append('<ul></ul>')
          .children('ul');
        for (var i = 0; i < list.length; i++) {
          listElement.append('<li><a href="' + list[i] + '">' +
            list[i] + '</a></li>');
        }
      } else {
        targetElement.addClass("ycu-data-false")
          .text('none');
      }
    }
  },

  /** Empty out the response data elements. */
  emptyData: function() {
    $('#startPointDetails').find(
      'dd[data-id="robotsAllowed"], dd[data-id="linkList"], dd[data-id="siteList"]')
      .empty().removeClass(function(index, css) {
        return (css.match(/(^|\s)ycu-data(-\S+)?/g) || []).join(' ');
      });

    $('#startPointSelectBox').children('option').each(function() {
      if (typeof $(this).attr('selected') === 'undefined') {
        $(this).prop('selected', false);
      } else {
        $(this).prop('selected', true);
      }
    });

    this.dataLoaded = false;
  },

  /** Create a String from the current date. */
  getDateString: function() {
    var d = new Date();
    return ('(' + d.getFullYear().toString() + '-' + d.getMonth().toString() +
      '-' + d.getDate().toString() + ' ' + d.getHours().toString() +
      ':' + d.getMinutes().toString() + ')');
  },

  /** Load informations for a given list of pages.
   * @param {function} Callback to call after all pages infos are loaded.
   * @param {object} Optional scope for callback
   */
  getPagesInfo: function(callback, scope) {
    var self = this;
    var urls; // url entered by user
    var content = $('#crawlingURL').val().trim();

    if (content.length === 0) {
      // no urls
      return;
    }
    urls = YaCyUi.Tools.cleanStringArray(content.split('\n'));

    this.urls.checked = 0;
    this.urls.failed = 0;
    this.urls.count = urls.length;

    this.emptyData();

    if (this.urls.count == 1) {
      $('#startPointDetails').find('*[data-id]').show();
      YaCyUi.Tools.checkCrawlUrl(urls[0], this.private.handleResponse.single, this);
    } else {
      $('#startPointDetails').find('*[data-id]').hide();
      $('#startPointDetails').find('*[data-id="bookmarkTitle"]').show();
      $('#startPointDetails').find('*[data-id="robotsAllowed"]').show();

      $('#startPointDetails').find('dd[data-id="robotsAllowed"]')
        .addClass('ycu-data-list')
        .append('<ul></ul>');

      for (var i = 0; i < this.urls.count; i++) {
        YaCyUi.Tools.checkCrawlUrl(urls[i], this.private.handleResponse.list, this);
      }
    }

    // handle callback, if any
    if (typeof callback === 'function') {
      this.responseCallback = callback;
      // delay stats reset until all data is loaded
      var t = setInterval(function() {
        if (self.urls.checked == self.urls.count) {
          clearInterval(t);
          $('#startPoint').find('*[data-id="getSiteData"]').hide();
          //self.responseDone(self.responseCallback);
          if (typeof scope === 'undefined') {
            self.responseCallback(self.urls);
          } else {
            self.responseCallback.call(scope, self.urls);
          }

        }
      }, 500);
    }
  }
};