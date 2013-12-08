/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitCrawl'),
    display: $('#ycu-error-count'),
    onload: true
  }).addElement($('#crawlingURL'), {
    validators: [{
      type: 'notEmpty',
      error: 'empty'
    }, {
      type: 'url',
      error: 'invalid'
    }, {
      type: 'urlProtocol',
      protocols: ['https?', 'file', 'ftp', 'smb'],
      error: 'protocol'
    }]
  }).addElement($('#crawlingDomMaxPages'), {
    validators: [{
      type: 'range',
      min: $('#crawlingDomMaxPages').data('min'),
      max: $('#crawlingDomMaxPages').data('max')
    }]
  });

  $('#startPoint').find('button[data-id="robotsAndStructure"]').on('click',
    function(evObj) {
      evObj.preventDefault();
      $(this).prop('disabled', true);
      YaCyUi.Form.Button.switchIcon($(this), 'icon-loader');
      YaCyUi.Form.Button.switchText($(this), 'Loading..');
      YaCyPage.CrawlStart.getPagesInfo(function() {
        var btn = $('#startPoint').find('button[data-id="robotsAndStructure"]');
        // hide buttons..pagesInfoLoaded
        $('#startPoint').find('*[data-id="getSiteData"]').hide();
        // ..and reset
        btn.prop('disabled', false);
        YaCyUi.Form.Button.switchIcon(btn);
        YaCyUi.Form.Button.switchText(btn);
      });
    });

  YaCyUi.Event.handle('validation-state', function(evObj, type, elements) {
    if (elements[0].id == 'crawlingURL') {
      $('#startPointDetails').hide('slow');
      if (type == 'valid') {
        $('#startPoint')
          .find('button[data-id="robotsAndStructure"]').show();
        $('#startPoint').find('*[data-id="getSiteData"]').show();
      } else {
        $('#startPointSelect').hide();
      }
    }
  });

  //new YaCyPage.Parts.FormControl();
  YaCyPage.CrawlStart = new YaCyPage.Func.CrawlStart();
};