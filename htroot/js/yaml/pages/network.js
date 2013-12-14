/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.conf = {
  imageStub: 'NetworkPicture.png?width=1024&height=720&bgcolor=FFFFFF&ct=15000&coronaangle=',
  imageArray: [],
  imageAnimIndex: 0,
  imageLoadIndex: 0,
  imageCycles: 0
};

YaCyPage.initAnimationPhase = function(phase, handle) {
  var angle = phase * 60;
  YaCyPage.conf.imageArray[phase] = new Image(1024, 720);
  YaCyPage.conf.imageArray[phase].src = YaCyPage.conf.imageStub + angle + "&amp;handle=" + handle;
  console.debug('phase', phase, YaCyPage.conf.imageArray[phase].src);
};

YaCyPage.initAnimation = function() {
  var handle = new Date().getTime();
  for (var i = 0; i < 6; i++) {
    YaCyPage.initAnimationPhase(i, handle);
  }
};

YaCyPage.doAnimation = function() {
  console.debug('doAnimation');

  YaCyPage.conf.networkPicture[0].src = YaCyPage.conf.imageArray[YaCyPage.conf.imageAnimIndex].src;
  YaCyPage.conf.imageAnimIndex++;
  if (YaCyPage.conf.imageAnimIndex == 6) {
    YaCyPage.conf.imageAnimIndex = 0;
  }
  YaCyPage.conf.imageCycles++;
  if (YaCyPage.conf.imageCycles == 25) {
    YaCyPage.initAnimationPhase(YaCyPage.conf.imageLoadIndex, new Date().getTime());
    YaCyPage.conf.imageLoadIndex++;
    if (YaCyPage.conf.imageLoadIndex == 6) {
      YaCyPage.conf.imageLoadIndex = 0;
    }
    YaCyPage.conf.imageCycles = 0;
  }
  setTimeout(function() {
    YaCyPage.doAnimation()
  }, 100);
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.conf.networkPicture = $('#networkPicture');
  if (YaCyPage.conf.networkPicture.length > 0) {
    console.debug('run..');
    setTimeout(function() {
      YaCyPage.initAnimation()
    }, 100);
    setTimeout(function() {
      YaCyPage.doAnimation()
    }, 1000);
  }
};