/** Initialize the page validators. */
YaCyPage.init = function(toggles) {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: toggles || $('#submitSave'),
    display: $('#ycuErrorCount'),
    onload: true,
    showLink: false
  });
  validator.addElement($('#acceptCrawlLimit'), {
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'range',
      min: $('#acceptCrawlLimit').data('min'),
      max: $('#acceptCrawlLimit').data('max'),
      error: 'range'
    }]
  });
};