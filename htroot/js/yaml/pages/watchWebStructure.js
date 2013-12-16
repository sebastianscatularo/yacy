/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

YaCyPage.conf = {
  image: 'WebStructurePicture_p.png?',
  data: {
    depth: null,
    time: null,
    nodes: null
  }
};

YaCyPage.changeImage = function() {
  var imageSrc = YaCyPage.conf.image;

  $('#configForm input[data-param="true"]').each(function() {
    if (/^color/.test(this.id)) {
      YaCyPage.conf.data[this.id] = $(this).val().replace(/^#/, '');
    } else {
      YaCyPage.conf.data[this.id] = $(this).val();
    }
  });
  for (var key in YaCyPage.conf.data) {
    var value = YaCyPage.conf.data[key];
    imageSrc += key + '=' + value + '&amp;';
  }

  imageSrc = imageSrc.replace(/&amp;$/, '');
  console.debug(imageSrc);
  $('#webPicture')[0].src = imageSrc;
};

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    onload: true
  }).addElement($('#depth, #nodes, #width, #height'), {
    onload: true,
    validators: [{
      type: 'notEmpty'
    }, {
      type: 'range',
      min: 0
    }]
  }).addElement($('#time'), {
    validators: [{
      type: 'notEmpty'
    }, {
      type: 'range',
      min: -1
    }]
  });

  // listen to all spinner changes
  $('#configForm input.spinner').spinner({
    stop: function(event, ui) {
      YaCyPage.changeImage();
    }
  });

  // listen to all color picker changes
  $('#configForm input[name^="color"]').minicolors({
    changeDelay: 500,
    change: function(hex, opacity) {
      YaCyPage.changeImage();
    }
  });

  YaCyPage.changeImage();
};