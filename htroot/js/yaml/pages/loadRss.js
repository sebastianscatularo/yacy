YaCyPage.Parts = {};

YaCyPage.Parts.StartPoint = function() {
  var self = this;

  this.addInteractionHandler = function() {
    // validator
    YaCyUi.Form.Validate.addValidator($('#crawlingURL'), {
      func: new YaCyUi.Form.Validate.Validators.url().validate,
      delay: YaCyPage.validationDelay,
      onload: true
    });
  };

  this.addInteractionHandler();
};

YaCyPage.Parts.FormControl = function() {
  var self = this;
  this.errorCount = 0;
  this.e = {
    btnSubmit: $('fieldset.formControl').find('button[type="submit"]')
  };

  this.e.btnSubmit
    .attr('title', 'Please first enter a start-point for your crawl.')
    .tooltip()
    .prop('disabled', false);
};

YaCyPage.EventHandler = function() {
  this.handle = function(evObj, type, elements) {
    // disable submit, if no valid crawlingUrl is present
    if (!YaCyUi.Form.Validate.isValid($('#crawlingURL'))) {
      YaCyPage.parts.formControl.e.btnSubmit
        .prop('disabled', true)
        .tooltip("option", "disabled", false);
    } else {
      YaCyPage.parts.formControl.e.btnSubmit
        .prop('disabled', false)
        .tooltip("option", "disabled", true);
    }
  }
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.parts = {
    formControl: new YaCyPage.Parts.FormControl()
  };

  YaCyPage.eventHandler = new YaCyPage.EventHandler();

  YaCyUi.Event.handle('validation-state', function(evObj, type, elements) {
    YaCyPage.eventHandler.handle(evObj, type, elements);
  });

  // init parts
  YaCyPage.parts.startPoint = new YaCyPage.Parts.StartPoint();
};