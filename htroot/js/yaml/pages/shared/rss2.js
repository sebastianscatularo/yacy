/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
YaCyPage.Func = YaCyPage.Func || {};
YaCyPage.Func.RSS2 = YaCyPage.Func.RSS2 || {};

/** Parse an RSS-Channel Item.
 * @param {jQuery} RSS-Channel Item XML wrapped in jQuery object
 */
YaCyPage.Func.RSS2.Item = function(rssItem) {
  var elements = [
    'title', 'link', 'description', 'author', 'comments', 'pubDate'
  ];
  this.data = {};

  for (var i = 0; i < elements.length; i++) {
    var element = rssItem.find(elements[i]).first();
    if (element.length > 0) {
      this.data[elements[i]] = element.text();
    }
  }

  var eCategory = rssItem.find('category').first();
  if (eCategory.length === 0) {
    this.data.category = null;
  } else {
    this.data.category = {
      domain: eCategory.attr('domain'),
      value: eCategory.text()
    };
  }

  var eEnclosure = rssItem.find('enclosure').first();
  if (eEnclosure.length === 0) {
    this.data.enclosure = null;
  } else {
    this.data.enclosure = {
      url: eEnclosure.attr('url'),
      length: eEnclosure.attr('length'),
      type: eEnclosure.attr('type')
    };
  }

  var eGuid = rssItem.find('guid').first();
  if (eEnclosure.length === 0) {
    this.data.guid = null;
  } else {
    this.data.guid = {
      isPermaLink: eGuid.attr('isPermaLink'),
      value: eGuid.text()
    };
  }

  var eSource = rssItem.find('source').first();
  if (eSource.length === 0) {
    this.data.source = null;
  } else {
    this.data.guid = {
      url: eSource.attr('url'),
      value: eSource.text()
    };
  }

  return this;
};

/** Parse an RSS Channel entry.
 * @param {jQuery} RSS-Channel XML wrapped in jQuery object
 */
YaCyPage.Func.RSS2.Channel = function(rssXml) {
  var self = this;
  this.items = []; // rss channel items
  this.image = null;

  rssXml.find('channel').first().find('item').each(function() {
    self.items.push(new YaCyPage.Func.RSS2.Item($(this)));
  });

  var eImage = rssXml.find('image').first();
  if (eImage.length > 0) {
    var imgAttr = ['url', 'title', 'link', 'width', 'height', 'description'];
    for (var i = 0; i < imgAttr.length; i++) {
      var value = eImage.attr(imgAttr[i]);
      if (value) {
        this.image[imgAttr[i]] = value;
      }
    }
  }
};

/**
 * @param {String} URL to request
 * @param {String} Request method (GET/POST)
 * @param {Objet} URL parameters
 * @param {funtion} Callback function
 * @param {Object} Scope for callback function. (optional)
 */
YaCyPage.Func.RSS2.get = function(url, method, data, callback, scope) {
  $.ajax({
    method: method,
    url: url,
    data: data,
    cache: false
  }).done(
    function(response) {
      var rss = YaCyPage.Func.RSS2.process(response);
      if (typeof callback === 'function') {
        if (typeof scope !== 'undefined') {
          callback.call(scope, rss);
        } else {
          callback(rss);
        }
      }
    }
  );
};

YaCyPage.Func.RSS2.process = function(data) {
  return new YaCyPage.Func.RSS2.Channel($(data));
};