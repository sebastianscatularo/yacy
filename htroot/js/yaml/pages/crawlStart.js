YaCyPage.Func = YaCyPage.Func || {};

YaCyPage.Func.CrawlStart = function() {
  var self = this;
  this.dataLoaded = false;
  this.dataLoading = false;
  this.responseCallback = []; // functions to call after all URLs are validated
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
  this.bookmarkTitle = '';
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
        this.urls.checked++;

        if (!('item' in response)) {
          this.urls.failed++;
          return;
        }
        response = response.item;

        // title
        if (this.urls.checked == 1) {
          this.bookmarkTitle = this.text.startUrlBatch + ' ' + this.getDateString();
          $('#startPointDetails').find('dd[data-id="bookmarkTitle"]')
            .text(this.bookmarkTitle);
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
        var crawlAllowed = true;
        this.urls.checked++;

        if (!('item' in response)) {
          return;
        }
        response = response.item;

        // title
        this.bookmarkTitle = response.title.trim();
        var bookmarkIcon = '';
        if (this.bookmarkTitle.length === 0) {
          this.bookmarkTitle = this.text.startSingleUrl + ' ' + this.getDateString();
        }
        if (response.favicon.length > 0) {
          bookmarkIcon = '<img src="' + response.favicon +
            '" style="width:16px;height:16px;margin-right:0.3em;"/>'
        }
        $('#startPointDetails').find('dd[data-id="bookmarkTitle"]')
          .html(bookmarkIcon + this.bookmarkTitle);

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
          var hasOptions = false; // true if there are more than one option

          // site-list
          option = $('#startPointSelectBox').find('option[data-id="siteList"]');
          data = response.sitemap.trim();
          if (data.length > 0) {
            option.prop('disabled', false)
            hasOptions = true;
          } else {
            option.prop('disabled', true);
          }
          this.private.listUrls(data,
            $('#startPointDetails dd[data-id="siteList"]'));

          // link-list
          option = $('#startPointSelectBox').find('option[data-id="linkList"]');
          data = response.links.trim();
          if (data.length > 0) {
            option.prop('disabled', false)
            hasOptions = true;
          } else {
            option.prop('disabled', true);
          }
          this.private.listUrls(data,
            $('#startPointDetails dd[data-id="linkList"]'));

          if (crawlAllowed && hasOptions) {
            $('#startPointSelectBox').prop('disabled', false);
            $('#startPointSelect').show('slow');
          } else {
            $('#startPointSelectBox').prop('disabled', true);
          }
        }

        $('#startPointDetails').show('slow');
        this.dataLoaded = true;
        this.dataLoading = false;
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

    $('#startPointSelectBox').prop('disabled', false)
      .children('option').each(function() {
        if (typeof $(this).attr('selected') === 'undefined') {
          $(this).prop('selected', false);
        } else {
          $(this).prop('selected', true);
        }
      });

    this.responseCallback = []; // clear callbacks
    this.dataLoaded = false;
  },

  /** Create a String from the current date. */
  getDateString: function() {
    var d = new Date();
    return ('(' + d.getFullYear().toString() + '-' + d.getMonth().toString() +
      '-' + d.getDate().toString() + ' ' + d.getHours().toString() +
      ':' + d.getMinutes().toString() + ')');
  },

  /** Add a callback function that gets called, if all URLs are validated. This
   * callback persist only one session, then it get's remove automatically.
   * @param {function} Function to call
   * @param {object} Function scope
   */
  addRuntimeCallback: function(func, scope) {
    this.responseCallback.push([func, scope]);
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

      $('#startPointSelectBox').prop('disabled', true);

      this.dataLoading = true;
      for (var i = 0; i < this.urls.count; i++) {
        YaCyUi.Tools.checkCrawlUrl(urls[i], this.private.handleResponse.list, this);
      }
    }

    // delay stats reset until all data is loaded
    this.responseCallback.push([callback, scope]);
    var t = setInterval(function() {
      if (self.urls.checked == self.urls.count) {
        clearInterval(t);
        self.dataLoaded = true;
        this.dataLoading = false;
        $('#startPoint').find('*[data-id="getSiteData"]').hide();
        // handle callbacks, if any
        if (self.responseCallback.length > 0) {
          for (var i = 0; i < self.responseCallback.length; i++) {
            if (typeof self.responseCallback[i][1] === 'undefined') {
              self.responseCallback[i][0](self.urls);
            } else {
              self.responseCallback[i][0].call(self.responseCallback[i][1],
                self.urls);
            }
          }
        }
      }
    }, 500);
  }
};