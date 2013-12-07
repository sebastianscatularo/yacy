YaCyPage.Parts = {};

/** Initialize the page. */
YaCyPage.init = function() {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitCrawlStart'),
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
        type: 'urlProtocol',
        protocols: ['https?'],
        error: 'protocol'
      }, {
        type: 'url',
        error: 'invalid'
      }]
    });
};