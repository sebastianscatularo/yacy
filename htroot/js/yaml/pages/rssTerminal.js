/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.conf = {
  colorClasses: [
    'peernews', 'remotesearch', 'localsearch', 'remoteindexing',
    'localindexing', 'dhtreceive', 'dhtsend', 'proxy'
  ],
  content: null, // set on init
  contentCursor: null, // set on init
  lastWait: null,
  maxLines: 20, // how many lines to display
  maxTime: 10000, // time that should be wasted until everything is scrolled
  maxWait: 500,
  maxWidth: 90,
  minWait: 50, // if this is too short, the CPU will eat all performance
  height: null,
  queue: [], // message queue
  requestCount: 0,
  set: '',
  tab: "&nbsp;&nbsp;",
  waitStepping: 2.5
};

YaCyPage.parseParameters = function() {
  var parameters = YaCyUi.Tools.getUrlParameters();

  YaCyPage.conf.maxWidth = parameters.maxwidth || YaCyPage.conf.maxWidth;
  YaCyPage.conf.minWait = parameters.minwait || YaCyPage.conf.minWait;
  YaCyPage.conf.maxWait = parameters.maxwait || YaCyPage.conf.maxWait;
  YaCyPage.conf.set = parameters.set || YaCyPage.conf.set;

  // check maximum lines that can be displayed
  var lastEntry;
  while (typeof lastEntry === 'undefined' ||
    (lastEntry.position().top + lastEntry.height()) < YaCyPage.conf.height) {
    YaCyPage.conf.content.append('<span>&nbsp;</span>');
    lastEntry = YaCyPage.conf.content.find('span').last();
    YaCyPage.conf.maxLines++;
  }
  YaCyPage.conf.content.empty();

  if (parameters.width) {
    $('#feedBox').css('width', parameters.width);
  } else {
    $('#feedBox').css('width', YaCyPage.conf.maxWidth);
  }
  if (parameters.height) {
    $('#feedBox').css('height', parameters.height);
  } else {
    $('#feedBox').css('height', YaCyPage.conf.maxLines + 'em');
  }
  if (parameters.background) {
    $('#feedBox').css('background-color', '#' + parameters.background);
  }
  if (parameters.color) {
    $('#feedBox').css('color', '#' + parameters.color);
  }
};

YaCyPage.addLine = function(line) {
  var lineType = line.split(':')[0].toLowerCase();
  if ($.inArray(lineType, YaCyPage.conf.colorClasses)) {
    YaCyPage.conf.queue.push('<span class="' + lineType + '">' + line + '</span>');
  } else {
    YaCyPage.conf.queue.push('<span>' + line + '</span>');
  }
};

YaCyPage.deQueue = function() {
  var time;
  if (YaCyPage.conf.queue.length > 0) {
    time = YaCyPage.conf.maxTime /
      (YaCyPage.conf.queue.length - YaCyPage.conf.maxLines);

    $(YaCyPage.conf.queue.shift()).insertBefore(YaCyPage.conf.contentCursor);

    // remove older entries
    var lastEntry = YaCyPage.conf.content.find('span').last();
    if (lastEntry.size() > 0) {
      var lastEntryPos = lastEntry.position().top + lastEntry.height();
      while (lastEntryPos > YaCyPage.conf.height) {
        YaCyPage.conf.content.find('span').first().remove();
        try {
          lastEntry = YaCyPage.conf.content.find('span').last();
          lastEntryPos = lastEntry.position().top + lastEntry.height();
        } catch (e) {
          break;
        }
      }
    }
  } else {
    time = YaCyPage.conf.lastWait + YaCyPage.conf.waitStepping;
  }

  if (time < YaCyPage.conf.minWait) {
    time = YaCyPage.conf.minWait;
  }
  if (time > YaCyPage.conf.maxWait) {
    time = YaCyPage.conf.maxWait;
  }

  if (time < YaCyPage.conf.lastWait) {
    time = (time + YaCyPage.conf.maxLines * YaCyPage.conf.lastWait) /
      (YaCyPage.conf.maxLines + 1);
  }

  YaCyPage.conf.lastWait = time;
  window.setTimeout(function() {
    YaCyPage.deQueue();
  }, time);
};

/** Callback function for YaCyPage.loadRss. */
YaCyPage.updateRSS = function(rssObj) {
  for (var i = 0; i < rssObj.items.length; i++) {
    var title = rssObj.items[i].data.title;
    if (title.length > 0) {
      var link = rssObj.items[i].data.link;
      if (link.length > 0) {
        YaCyPage.addLine(title + ":" + link);
      } else {
        YaCyPage.addLine(title);
      }
    }
    var description = rssObj.items[i].data.description;
    if (description.length > 0) {
      YaCyPage.addLine(YaCyPage.conf.tab + description);
    }
  }
};

YaCyPage.loadRSS = function() {
  YaCyPage.Func.RSS2.get('/api/feed.rss', 'GET', {
    count: 80,
    set: YaCyPage.conf.set,
    requestCount: YaCyPage.conf.requestCount,
    time: (new Date()).getTime()
  }, YaCyPage.updateRSS);
  YaCyPage.conf.requestCount++;
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.conf.content = $('#feedBox');
  YaCyPage.conf.lastWait = YaCyPage.conf.maxWait;

  YaCyPage.parseParameters();

  YaCyPage.conf.height = YaCyPage.conf.content.height();
  YaCyPage.conf.content.append('<span class="cursor"></span>');
  YaCyPage.conf.contentCursor = $('#feedBox').find('.cursor');

  window.setInterval(function() {
    YaCyPage.loadRSS();
  }, 10000);

  YaCyPage.loadRSS();
  YaCyPage.deQueue();
};