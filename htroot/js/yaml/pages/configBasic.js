/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitConfig'),
    display: $('#ycu-error-count')
  }).addElement($('#peerName'), {
    onload: true,
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'regEx',
      exp: /^[a-zA-Z0-9\-_]+$/,
      error: 'characters'
    }, {
      type: 'length',
      min: 3,
      max: 80,
      error: 'length'
    }],
  })
    .addElement($('#peerPort'), {
      onload: true,
      validators: [{
        type: 'notEmpty',
        error: 'empty'
      }, {
        type: 'range',
        min: 0,
        max: 65535,
        nan: 'nan'
      }, {
        type: 'range',
        min: 0,
        max: 1023,
        invert: true,
        failType: 'warning'
      }]
    });
};