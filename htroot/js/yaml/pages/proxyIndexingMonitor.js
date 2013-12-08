/** Initialize the page validators. */
YaCyPage.init = function() {
  // init parts
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitSave'),
    display: $('#ycuErrorCount'),
    onload: true
  }).addElement($('#proxyPrefetchDepth'), {
    validators: [{
      type: 'notEmpty'
    }, {
      type: 'range',
      min: $('#proxyPrefetchDepth').data('min'),
      max: $('#proxyPrefetchDepth').data('max')
    }]
  }).addElement($('#HTCache_size'), {
    validators: [{
      type: 'notEmpty'
    }, {
      type: 'range',
      min: $('#HTCache_size').data('min'),
      max: $('#HTCache_size').data('max')
    }]
  });
};