/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";
YaCyPage.Parts = {};

YaCyPage.Parts.StartPoint = function() {
  var self = this;
  var root = $('#startPoint');
  this.urlResult = null;

  // cache some elements
  this.e = {
    btnBar: root.find('*[data-id="getSiteData"]'),
    btnRobotsAndStructure: root.find('button[data-id="robotsAndStructure"]'),
    btnRobots: root.find('button[data-id="robots"]')
  };

  this.pagesInfoLoaded = function(urls) {
    // hide buttons..
    this.e.btnBar.hide();
    // ..and reset
    this.e.btnRobotsAndStructure.prop('disabled', false);
    YaCyUi.Form.Button.switchIcon(this.e.btnRobotsAndStructure);
    YaCyUi.Form.Button.switchText(this.e.btnRobotsAndStructure);
    this.e.btnRobots.prop('disabled', false);
    YaCyUi.Form.Button.switchIcon(this.e.btnRobots);
    YaCyUi.Form.Button.switchText(this.e.btnRobots);

    this.urlResult = urls;
  };

  this.addInteractionHandler = function() {
    // crawl start point buttons
    self.e.btnRobots.prop('disabled', false).click(function(evObj) {
      evObj.preventDefault();
      YaCyPage.CrawlStart.getPagesInfo(self.pagesInfoLoaded, self);
    });
    self.e.btnRobotsAndStructure.prop('disabled', false).click(function(evObj) {
      evObj.preventDefault();
      $(this).prop('disabled', true);
      YaCyUi.Form.Button.switchIcon($(this), 'icon-loader');
      YaCyUi.Form.Button.switchText($(this), 'Loading..');
      YaCyPage.CrawlStart.getPagesInfo(self.pagesInfoLoaded, self);
    });
  };

  this.resetUrlResults = function() {
    this.urlResult = null;
  };

  this.getUrlResults = function() {
    return this.urlResult;
  };

  // init
  self.addInteractionHandler();
  return self;
};

YaCyPage.Parts.FormControl = function() {
  var self = this;
  this.e = {
    btnSubmit: $('#formControl').find('button[type="submit"]')
  };

  this.addInteractionHandler = function() {
    var btnCheck = $('#formControl').find('button[data-id="check"]');

    btnCheck.prop('disabled', false).on('click', function(evObj) {
      evObj.preventDefault();
      // check if we need to load startPoint details first
      if (!YaCyPage.CrawlStart.dataLoaded &&
        YaCyUi.DataStore.get($('#crawlingURL'), 'validation', 'valid')) {
        self.e.btnSubmit.prop('disabled', true);
        YaCyUi.Form.Button.switchIcon(btnCheck, 'icon-loader');
        btnCheck.prop('disabled', true);
        if (YaCyPage.CrawlStart.dataLoading) {
          console.debug("urls still validating..");
          YaCyPage.CrawlStart.addRuntimeCallback(function() {
            YaCyUi.Form.Button.switchIcon(btnCheck);
            btnCheck.prop('disabled', false);
            self.e.btnSubmit.prop('disabled', false);
            YaCyPage.Report.generate();
            console.debug("urls validated.");
          }, self);
        } else {
          YaCyPage.CrawlStart.getPagesInfo(function(urls) {
            YaCyPage.parts.startPoint.pagesInfoLoaded(urls);
            YaCyUi.Form.Button.switchIcon(btnCheck);
            btnCheck.prop('disabled', false);
            self.e.btnSubmit.prop('disabled', false);
            YaCyPage.Report.generate();
          }, self);
        }
      } else {
        YaCyPage.Report.generate();
      }
    });

    $('#formControl').find('button[data-id="edit"]').on('click', function(evObj) {
      evObj.preventDefault();
      YaCyPage.Report.hide('crawler');
    });

    self.e.btnSubmit.on('click', function(evObj) {
      evObj.preventDefault();
      YaCyUi.Form.Data.submit($('#crawlerForm'));
    });
  };

  // init
  self.addInteractionHandler();
};

/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitCrawlStart'),
    display: $('#ycu-error-count'),
    onload: true
  }).addElement($('#crawlingURL, #mustmatch, #ipMustmatch, #countryMustMatchList'), {
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }]
  }).addElement($('#indexmustmatch, #indexcontentmustmatch'), {
    validators: [{
      type: 'notEmpty',
      failType: 'warning'
    }]
  }).addElement($('#crawlingURL'), {
    validators: [{
      type: 'url',
      error: 'invalid'
    }, {
      type: 'urlProtocol',
      protocols: ['https?', 'file', 'ftp', 'smb'],
      error: 'protocol'
    }]
  }).addElement($('#countryMustMatchList'), {
    validators: [{
      type: 'regEx',
      exp: /^([a-z]{2},)+[a-z]{2}$/i,
      error: 'invalid'
    }]
  }).addElement($('#crawlingDepth'), {
    validators: [{
      type: 'range',
      min: $('#crawlingDepth').data('min'),
      max: $('#crawlingDepth').data('max'),
      error: 'range'
    }]
  }).addElement($('#crawlingDomMaxPages'), {
    validators: [{
      type: 'range',
      min: $('#crawlingDomMaxPages').data('min'),
      max: $('#crawlingDomMaxPages').data('max')
    }]
  }).addElement($('#mustnotmatch, #ipMustnotmatch, #indexmustnotmatch, #indexcontentmustnotmatch'), {
    validators: [{
      type: 'regEx',
      exp: /^\.\*$/,
      invert: true,
      failType: 'warning'
    }]
  });

  YaCyUi.Event.handle('validation-state', function(evObj, type, elements) {
    if (elements[0].id == 'crawlingURL') {
      $('#startPointDetails').hide('slow');
      $('#startPointSelect').hide('slow');
      YaCyPage.parts.startPoint.resetUrlResults();
      if (type == 'valid') {
        var btnRobots = $('#startPoint').find('button[data-id="robots"]');
        var btnRobotsAndStructure = $('#startPoint')
          .find('button[data-id="robotsAndStructure"]');
        var content = YaCyUi.Tools.cleanStringArray($(elements[0]).val().split('\n'));
        if (content.length > 1) {
          // URL list
          btnRobots.show();
          btnRobotsAndStructure.hide();
        } else {
          // single URL
          btnRobots.hide();
          btnRobotsAndStructure.show();
        }
        $('#startPoint').find('*[data-id="getSiteData"]').show();
      } else {
        $('#startPoint').find('*[data-id="getSiteData"]').hide();
      }
    }
  });

  new YaCyPage.Parts.FormControl();
  YaCyPage.CrawlStart = new YaCyPage.Func.CrawlStart();
  // init parts and store validable ones
  YaCyPage.parts = {
    startPoint: new YaCyPage.Parts.StartPoint()
  };
};