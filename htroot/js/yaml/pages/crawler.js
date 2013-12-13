/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";
YaCyPage.conf = {
  countInterval: null,
  refreshInterval: 2,
  statusLoaded: true,
  wait: 0,
  webPictureIdx: 0
};

YaCyPage.countdown = function() {
  if (YaCyPage.conf.statusLoaded) {
    YaCyPage.conf.wait--;
    if (YaCyPage.conf.wait <= 0) {
      YaCyPage.refresh();
    }
  }
};

YaCyPage.handleStatus = function(response) {
  var xml = $(response);
  var eStatus = xml.find('status').first();

  var currentPPM = eStatus.find('ppm').text();
  $('#ppmNum').empty().text(currentPPM);
  $('#ppmBar').attr('max', $('#customPPM').val()).attr('value', currentPPM);

  var trafficCrawler = eStatus.find('traffic').find('crawler').text();
  trafficCrawler = Math.round((trafficCrawler) / 1024 / 10.24);
  $('#trafficCrawler').empty().text(trafficCrawler + ' / 100');

  var dbSize = eStatus.find('dbsize');
  $('#urlPublicTextSize').text(dbSize.find('urlpublictext').text());
  $('#urlPublicTextSegmentCount').text(dbSize.find('urlpublictextSegmentCount').text());
  $('#webgraphSize').text(dbSize.find('webgraph').text());
  $('#webgraphSegmentCount').text(dbSize.find('webgraphSegmentCount').text());
  $('#citationSize').text(dbSize.find('citation').text());
  $('#citationSegmentCount').text(dbSize.find('citationSegmentCount').text());
  $('#rwiPublicTextSize').text(dbSize.find('rwipublictext').text());
  $('#rwiPublicTextSegmentCount').text(dbSize.find('rwipublictextSegmentCount').text());

  var postprocessing = eStatus.find('postprocessing');
  var postprocessingTime = {
    elapsed: postprocessing.find('elapsedTime').text(),
    remaining: postprocessing.find('remainingTime').text()
  };
  $('#postprocessingStatus').text(postprocessing.find('status').text());
  $('#postprocessingCollection').text(postprocessing.find('collectionRemainingCount').text());
  $('#postprocessingWebgraph').text(postprocessing.find('webgraphRemainingCount').text());
  $('#postprocessingRemainingTimeMinutes').text(postprocessing.find('remainingTimeMinutes').text());
  $('#postprocessingRemainingTimeSeconds').text(postprocessing.find('remainingTimeSeconds').text());
  var postprocessingTimePerc = 100 * postprocessingTime.elapsed /
    (postprocessingTime.elapsed + postprocessingTime.remaining);
  $('#postprocessingBar').attr('max', postprocessingTimePerc);

  $('#load').text(eStatus.find('load').text());

  var loaderQueue = eStatus.find('loaderqueue');
  $('#loaderQueueSize').text(loaderQueue.find('size').text());
  $('#loaderQueueMax').text(loaderQueue.find('max').text());

  var localCrawlerQueue = eStatus.find('localcrawlerqueue');
  $('#localCrawlerQueueSize').text(localCrawlerQueue.find('size').text());
  YaCyPage.setQueueState('localCrawler', localCrawlerQueue.find('state').text());

  var limitCrawlerQueue = eStatus.find('limitcrawlerqueue');
  $('#limitCrawlerQueueSize').text(limitCrawlerQueue.find('size').text());
  YaCyPage.setQueueState('limitCrawler', limitCrawlerQueue.find('state').text());

  var remoteCrawlerQueue = eStatus.find('remotecrawlerqueue');
  $('#remoteCrawlerQueueSize').text(remoteCrawlerQueue.find('size').text());
  YaCyPage.setQueueState('remoteCrawler', remoteCrawlerQueue.find('state').text());

  var noLoadCrawlerQueue = eStatus.find('noloadcrawlerqueue');
  $('#noLoadCrawlerQueueSize').text(noLoadCrawlerQueue.find('size').text());
  YaCyPage.setQueueState('noLoadCrawler', noLoadCrawlerQueue.find('state').text());

  YaCyPage.conf.statusLoaded = true;
};

YaCyPage.refresh = function() {
  YaCyPage.conf.wait = YaCyPage.conf.refreshInterval;
  YaCyPage.conf.statusLoaded = false;
  YaCyPage.requestStatus();
  YaCyPage.Func.RSS2.get('/api/feed.xml', 'POST', {
    count: 20,
    set: 'REMOTEINDEXING,LOCALINDEXING',
    time: (new Date()).getTime()
  }, YaCyPage.showRSS);
};

YaCyPage.requestStatus = function() {
  $.ajax({
    type: 'POST',
    url: '/api/status_p.xml',
    data: {
      html: ''
    }
  }).done(
    function(response) {
      YaCyPage.handleStatus(response);
    }
  );
};

YaCyPage.setQueueState = function(queue, state) {
  var eLink = $('#' + queue + 'StateLink');

  if (state == "paused") {
    eLink[0].href = "Crawler_p.html?continue=" + queue;
    eLink[0].title = "Continue this queue (" + state + ")";
    eLink.addClass('icon-control-run').removeClass('icon-control-pause');
  } else {
    eLink[0].href = "Crawler_p.html?pause=" + queue;
    eLink[0].title = "Pause this queue (" + state + ")";
    eLink.addClass('icon-control-pause').removeClass('icon-control-run');
  }
};

YaCyPage.showRSS = function(rssObj) {
  var crawlList = $('#crawlList');
  var itemsToShow = 50; // number of entries to show
  var entryTemplate = '<tr><td class="breakAll"><a href="ViewFile.html?action=info&amp;urlHash=%GUID%" target="_blank" title="%LINK%">%DESCRIPTION%</a></td><td class="breakAll"><a href="ViewFile.html?action=info&amp;urlHash=%GUID%" target="_blank" title="%LINK%">%LINK%</a></td></tr>';

  if (rssObj.items.length > 0 && crawlList.length > 0) {
    var tBody = crawlList.find('tbody');
    if (crawlList.find('tr').length > itemsToShow) {
      tBody.empty();
    }
    for (var i = 0; i < rssObj.items.length; i++) {
      console.debug('rssObj', rssObj.items[i]);
      tBody.append(
        entryTemplate
        .replace('%GUID%', rssObj.items[i].data.guid ? rssObj.items[i].data.guid.value : '', 'g')
        .replace('%LINK%', encodeURI(rssObj.items[i].data.link), 'g')
        .replace('%DESCRIPTION%', rssObj.items[i].data.description, 'g')
      );
    }
  }
};

YaCyPage.doWebPictureAnimation = function(nextTimeout) {
  var accessPicture = $('#webPicture');
  YaCyPage.conf.webPictureIdx++;
  accessPicture.src = 'WebStructurePicture_p.png?host=#[hosts]#&amp;depth=4&amp;width=1024&amp;height=512&amp;nodes=600&amp;time=1000&amp;colortext=888888&amp;colorback=FFFFFF&amp;colordot0=1111BB&amp;colordota=11BB11&amp;colorline=222222&amp;colorlineend=333333&amp;idx=' + YaCyPage.conf.webPictureIdx;
  setTimeout(function() {
    YaCyPage.doWebPictureAnimation(nextTimeout > 3000 ? 3000 : nextTimeout * 1.2);
  }, nextTimeout);
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.refresh();
  YaCyPage.conf.countInterval = window.setInterval(function() {
    YaCyPage.countdown();
  }, 1000);

  var webPicture = $('#webPicture');
  if (webPicture.length > 0) {
    setTimeout(function() {
      YaCyPage.doWebPictureAnimation(500);
    }, 500);
  }

  var validator = new YaCyUi.Form.Validator({
    toggle: $('#crawlingPerformanceCustom')
  }).addElement($('#customPPM'), {
    onload: true,
    validators: [{
      type: 'notEmpty'
    }, {
      type: 'number'
    }, {
      type: 'range'
    }]
  });
};