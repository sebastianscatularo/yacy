YaCyPage.Parts = {};

YaCyPage.Parts.StartPoint = function() {
  var self = this;
  var root = $('#startPoint');
  this.e = {
    crawlingURL: $('#crawlingURL'),
    startPointDetails: $('#startPointDetails'),
    startPointSelect: $('#startPointSelect'),
    btnBar: root.find('*[data-id="getSiteData"]'),
    btnRobotsAndStructure: root.find('button[data-id="robotsAndStructure"]'),
  };
  var valid = false;

  this.pagesInfoLoaded = function() {
    // hide buttons..
    this.e.btnBar.hide();
    // ..and reset
    this.e.btnRobotsAndStructure.prop('disabled', false);
    YaCyUi.Form.Button.switchIcon(this.e.btnRobotsAndStructure);
    YaCyUi.Form.Button.switchText(this.e.btnRobotsAndStructure);
  };

  this.addInteractionHandler = function() {
    // validator
    YaCyUi.Form.Validate.addValidator(self.e.crawlingURL, {
      func: new YaCyUi.Form.Validate.Validators.url().validate,
      delay: YaCyPage.validationDelay,
      onload: true
    });

    // crawl start point button
    self.e.btnRobotsAndStructure.prop('disabled', false).click(function(evObj) {
      evObj.preventDefault();
      $(this).prop('disabled', true);
      YaCyUi.Form.Button.switchIcon($(this), 'icon-loader');
      YaCyUi.Form.Button.switchText($(this), 'Loading..');
      YaCyPage.CrawlStart.getPagesInfo(self.pagesInfoLoaded, self);
    });
  };

  this.setValid = function(state) {
    valid = state;
  }

  this.isValid = function() {
    return state;
  }

  this.addInteractionHandler();
  return this;
};

YaCyPage.Parts.FormControl = function() {
  var self = this;
  this.errorCount = 0;
  this.e = {
    btnSubmit: $('fieldset.formControl').find('button[type="submit"]')
  };
};

YaCyPage.EventHandler = function() {
  var self = this;
  this.e = {
    btnBar: $('#startPoint').find('*[data-id="getSiteData"]')
  };

  this.handle = function(evObj, type, elements) {
    if (type !== 'valid') {
      self.e.btnBar.hide();
    } else {
      self.e.btnBar.show();
    }
  };
};

/** Initialize the page. */
YaCyPage.init = function() {
  new YaCyPage.Parts.FormControl();
  YaCyPage.CrawlStart = new YaCyPage.Func.CrawlStart();

  // init parts and store validable ones
  YaCyPage.parts = {
    startPoint: new YaCyPage.Parts.StartPoint()
  }

  YaCyPage.eventHandler = new YaCyPage.EventHandler();

  YaCyUi.Event.handle('validation-state', function(evObj, type, elements) {
    YaCyPage.eventHandler.handle(evObj, type, elements);
  });
};