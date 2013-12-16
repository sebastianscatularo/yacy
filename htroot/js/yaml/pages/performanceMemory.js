/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.refreshInterval = null;

YaCyPage.reloadGraph = function() {
  document.images["graph"].src = "PerformanceGraph.png?nopeers=&amp;time=" + (new Date()).getTime();
};

YaCyPage.animateGraph = function() {
  YaCyPage.refreshInterval = window.setInterval(function() {
    YaCyPage.reloadGraph();
  }, 1000);
};

/** Initialize the page. */
YaCyPage.init = function() {
  if ($('#autoReload').prop('checked')) {
    YaCyPage.animateGraph();
  }

  $('#autoReload').on('change', function() {
    if ($(this).is(':checked')) {
      if (!YaCyPage.refreshInterval) {
        YaCyPage.animateGraph();
      }
    } else if (YaCyPage.refreshInterval) {
      window.clearInterval(YaCyPage.refreshInterval);
      YaCyPage.refreshInterval = null;
    }
  });
};