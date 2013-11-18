YaCyPage.Func = YaCyPage.Func || {};

YaCyPage.Func.CrawlStart = function() {
  var self = this;
  this.dataLoaded = false;
  // status count for callback URL checks
  var root = $('#startPoint');
  // state holding for url list checking
  this.urls = {
    checked: 0,
    count: 0,
    failed: 0
  };
  // elements cache
  this.e = {
    crawlingURL: $('#crawlingURL'),
    startPointDetails: $('#startPointDetails'),
    btnBar: root.find('*[data-id="getSiteData"]')
  };
  this.e.data = {
    bookmarkTitle: this.e.startPointDetails.find('dd[data-id="bookmarkTitle"]'),
    robotsAllowed: this.e.startPointDetails.find('dd[data-id="robotsAllowed"]')
  };
  // text strings
  this.text = {
    robotsAllowed: 'yes',
    robotsDisallowed: 'no - you can not start a crawl for this URL',
    startSingleUrl: 'Single URL',
    startUrlBatch: 'URLs batch'
  };
  this.bookmarkTitle = ''; // bookmark title for URL(s)
  this.startType = 'single'; // may be single or list, based on start urls

  function init() {
    // check for optional elements
    var startPointSelect = $('#startPointSelect');
    var siteList = self.e.startPointDetails.find('dd[data-id="siteList"]');
    var linkList = self.e.startPointDetails.find('dd[data-id="linkList"]');
    if (startPointSelect.size() > 0) {
      self.e.startPointSelect = startPointSelect;
      self.e.startPointSelectBox = $('#startPointSelectBox');
    }
    if (siteList.size() > 0) {
      self.e.data.siteList = siteList;
    }
    if (linkList.size() > 0) {
      self.e.data.linkList = linkList;
    }
  };

  init();
};
YaCyPage.Func.CrawlStart.prototype = {
  private : {
    handleResponse: {
      /** Handle the JSON response from getpageinfo.json for a list of URLs
        * @param {object} JSON response object */
      list: function(response, url) {
        this.urls.checked++;

        // title
        if (this.urls.checked == 1) {
          this.bookmarkTitle = this.text.startUrlBatch + ' ' +
            this.getDateString();
          e = this.e.data.bookmarkTitle;
          e.removeClass("ycu-data-false")
            .text(this.bookmarkTitle);
        }

        // robots
        e = this.e.data.robotsAllowed.children('ul');
        var htmlClass = '';
        if (response.robots == 0) {
          htmlClass = 'ycu-data-false';
          this.url.failed++;
        } else {
          htmlClass = 'ycu-data-true';
        }
        e.append('<li class="' + htmlClass + '"><a href="' + url + '">' + url
          + '</a></li>');
        this.e.startPointDetails.show('slow');
        this.e.startPointDetails.show('slow');
      },

      /** Handle the JSON response from getpageinfo.json for a single URL
        * @param {object} JSON response object */
      single: function(response) {
        this.urls.checked++;

        if (!('item' in response)) {
          return;
        }
        response = response.item;

        var e, data;
        var crawlAllowed = true;

        // title
        e = this.e.data.bookmarkTitle;
        data = response.title.trim();
        if (data.length > 0) {
          e.removeClass("ycu-data-false").text(data);
        } else {
          data = this.text.startSingleUrl + ' ' + this.getDateString();
          e.addClass("ycu-data-false").text(data);
        }
        this.bookmarkTitle = data;

        // robots
        e = this.e.data.robotsAllowed;
        if (response.robots != 0) {
          e.removeClass("ycu-data-false")
            .addClass("ycu-data-true")
            .text(this.text.robotsAllowed);
        } else {
          e.addClass("ycu-data-false")
            .removeClass("ycu-data-true")
            .text(this.text.robotsDisallowed);
          crawlAllowed = false;
        }

        // only if optional elements are defined
        if ('startPointSelect' in this.e || 'siteList' in this.e.data ||
            'linkList' in this.e.data) {
          var option;
          var hasStartPointSelect = 'startPointSelect' in this.e;

          // site-list
          data = response.sitemap.trim();
          // only if startPointSelect exists
          if (hasStartPointSelect) {
            option = this.e.startPointSelectBox.find('option[data-id="siteList"]');
            data.length > 0 ? option.prop('disabled', false) :
            option.prop('disabled', true);
          }
          if ('siteList' in this.e.data) {
            e = this.e.data.siteList;
            this.private.listUrls(data, e);
          }

          // link-list
          data = response.links.trim();
          if (hasStartPointSelect) {
            option = this.e.startPointSelectBox.find('option[data-id="linkList"]');
            data.length > 0 ? option.prop('disabled', false) :
            option.prop('disabled', true);
          }
          if ('linkList' in this.e.data) {
            e = this.e.data.linkList;
            this.private.listUrls(data, e);
          }

          if (crawlAllowed && hasStartPointSelect) {
            this.e.startPointSelect.show('slow');
          }
        }

        this.e.startPointDetails.show('slow');
      }
    },

    /** List URLs checked in handleResponse.list. */
    listUrls: function(data, e) {
      if (data.length > 0) {
        var list = data.split(' ');
        var listElement = e
          .empty()
          .removeClass("ycu-data-false")
          .addClass("ycu-data-list")
          .append('<ul></ul>')
          .children('ul');
        for (var i=0; i<list.length; i++) {
          listElement.append('<li><a href="' + list[i] + '">' + list[i] + '</a></li>');
        }
      } else {
        e.addClass("ycu-data-false")
          .removeClass("ycu-data")
          .text('none');
      }
    }
  },

  /** Empty out the response data elements. */
  emptyData: function() {
    for (var key in this.e.data) {
      this.e.data[key].empty()
        .removeClass('ycu-data ycu-data-false ycu-data-true');
    }
    // only if startPointSelect exists
    if ('startPointSelect' in this.e) {
      // reset startpoint selection
      this.e.startPointSelectBox.children('option').each(function() {
        if (typeof $(this).attr('selected') === 'undefined') {
          $(this).prop('selected', false);
        } else {
          $(this).prop('selected', true);
        }
      });
    }
    this.bookmarkTitle = '';
    this.dataLoaded = false;
  },

  /** Create a String from the current date. */
  getDateString: function() {
    var d = new Date();
    return '(' + d.getFullYear().toString() + '-' + d.getMonth().toString()
      + '-' + d.getDate().toString() + ' ' + d.getHours().toString()
      + ':' + d.getMinutes().toString() + ')';
  },

  /** Load informations for a given list of pages.
    * @param {function} Callback to call after all pages infos are loaded.
    * @param {object} Optional scope for callback */
  getPagesInfo: function(callback, scope) {
    var self = this;
    var urls; // url entered by user
    var content = this.e.crawlingURL.val().trim();

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
      this.e.startPointDetails.find('*[data-id]').show();
      YaCyUi.Tools.checkCrawlUrl(urls[0], this.private.handleResponse.single, this);
    } else {
      this.e.startPointDetails.find('*[data-id]').hide();
      this.e.startPointDetails.find('*[data-id="bookmarkTitle"]').show();
      this.e.startPointDetails.find('*[data-id="robotsAllowed"]').show();

      this.e.data.robotsAllowed
        .addClass('ycu-data-list')
        .empty()
        .append('<ul></ul>');

      for (var i=0; i<this.urls.count; i++) {
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
          self.e.btnBar.hide();
          //self.responseDone(self.responseCallback);
          if (typeof scope === 'undefined') {
            self.responseCallback();
          } else {
            self.responseCallback.call(scope);
          }

        }
      }, 500);
    }
  }
};