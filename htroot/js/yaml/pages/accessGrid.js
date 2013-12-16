/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  if ($('#accessPicture').length > 0) {
    console.debug('load anim..');
    new YaCyUi.Tools.PictureAnimation({
      imageStub: 'AccessPicture_p.png?width=1024&amp;height=576&amp;colorback=FFFFFF&amp;colortext=AAAAAA&amp;colorgrid=333333&amp;colordot=33CC33&amp;colorline=555555&amp;ct=5000&amp;coronaangle=',
      element: $('#accessPicture')
    });
  }
};