YaCyPage.Parts = {};

/** Initialize the page validators. */
YaCyPage.addValidators = function(toggles) {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: toggles || $('#submitCrawlStart'),
    display: $('#ycu-error-count'),
    onload: true,
    showLink: false
  });
  validator
    .addElement($('#crawlingURL'), {
      validators: [{
        type: 'notEmpty',
        error: 'empty'
      }, {
        type: 'url',
        error: 'invalid'
      }, {
        type: 'urlProtocol',
        protocols: ['https?'],
        error: 'protocol'
      }]
    });
};