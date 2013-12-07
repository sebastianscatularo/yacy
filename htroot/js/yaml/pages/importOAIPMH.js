/** Initialize the page. */
YaCyPage.init = function(toggles) {
  // init parts
  var validatorSingle = new YaCyUi.Form.Validator({
    toggle: $('#submitSingleSource'),
    display: $('#ycuErrorCountSingle'),
    onload: true,
    showLink: false
  });
  validatorSingle.addElement($('#singleURL'), {
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

  var validatorAll = new YaCyUi.Form.Validator({
    toggle: $('#submitAllSource, #submitAllListSource'),
    display: $('#ycuErrorCountAll'),
    onload: true,
    showLink: false
  });
  validatorAll.addElement($('#allURL'), {
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