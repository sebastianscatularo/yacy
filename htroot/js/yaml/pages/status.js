/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.banner = {
  textColor: '000000',
  bgColor: 'DEE6F3',
  borderColor: 'DEE6F3'
};

YaCyPage.loadBanner = function() {
  console.debug('loadBanner');
  $('#YaCyPage.banner.mg').attr('src', '/YaCyPage.banner.png?textcolor=' +
    YaCyPage.banner.textColor + '&bgcolor=' + YaCyPage.banner.bgColor +
    '&bordercolor=' + YaCyPage.banner.borderColor +
    '&time=' + (new Date()).getTime());
};

YaCyPage.reloadGraph = function() {
  console.debug('reloadGraph');
  $('#performanceGraphImg').attr('src',
    'PerformanceGraph.png?nomem=&amp;time=' + (new Date()).getTime());
};

/** Initialize the page. */
YaCyPage.init = function() {
  window.setInterval(function() {
    YaCyPage.loadBanner();
  }, 20000);
  window.setInterval(function() {
    YaCyPage.reloadGraph();
  }, 8000);
};