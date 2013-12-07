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
  }

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
  }

  this.getUrlResults = function() {
    return this.urlResult;
  }

  // init
  self.addInteractionHandler();
  return self;
};

YaCyPage.Report = {
  text: {
    editButton: 'Change',
    fixButton: 'Fix errors',
    changeDefaultsButton: 'Configure',
    confBookmarkTile: 'Bookmark title',
    useDefaultConfig: 'use default settings',
    emptyInput: 'left empty'
  },
  // cached elements
  e: null,
  visible: false,

  handeSpecialElements: function(e, id, section, data) {
    switch (id) {
      case 'reloadoldage':
      case 'deleteoldage':
        if (!e.is(':checked')) {
          return;
        }
        var text;
        if (id == 'deleteoldage') {
          text = 'If older than ' +
            $('#deleteIfOlderNumber').children('option:selected').text() +
            ' ' + $('#deleteIfOlderUnit').children('option:selected').text();
        } else {
          text = 'If older than ' +
            $('#reloadIfOlderNumber').children('option:selected').text() +
            ' ' + $('#reloadIfOlderUnit').children('option:selected').text();
        }

        var title = e.closest('fieldset').children('legend').text();
        data.content.append('<h1>' + title + '</h1>');
        data.entries.append('<dt class="noData">' + text + '</dt><dd>&nbsp;</dd>');
        break;
      case 'startPointSelectBox':
        if (YaCyUi.DataStore.get($('#crawlingURL'), 'validation', 'valid') &&
          YaCyPage.CrawlStart.startType == 'single') {
          var label = section.find('label[for="' + id + '"]');
          data.entries.append('<dt>' + label.text() + '</dt><dd>' + e.children('option:selected').text() + '</dd>');
        }
        break;
    }
  },

  parseSection: function(section, data) {
    section.find('input:not([type="button"]), select').not(':disabled')
      .not('[data-review="skip"]').each(function() {
        var id = this.id;
        var label;
        if ($(this).parent()[0].tagName.toLowerCase() == 'label') {
          label = $(this).parent().text().trim();
        } else {
          label = section.find('label[for="' + id + '"]').text().trim();
        }

        if ($(this).data('review') == 'custom') {
          YaCyPage.Report.handeSpecialElements($(this), id, section, data);
        } else if (this.tagName.toLowerCase() == 'select') {
          data.entries.append('<dt>' + label + '</dt><dd>' + $(this).children('option:selected').text() + '</dd>');
        } else if ($(this).attr('type').toLowerCase() == 'text') {
          var value = $(this).val().trim();
          // validable & valid?
          var validationState = YaCyUi.DataStore.get($(this), 'validation', 'valid');
          if (validationState !== null && data.isValid) {
            data.isValid = validationState;
          }
          // content
          if (value.length === 0) {
            data.entries.append('<dt>' + label +
              '</dt><dd class="empty">' + YaCyPage.Report.text.emptyInput + '</dd>');
          } else {
            data.entries.append('<dt>' + label + '</dt><dd>' + value + '</dd>');
          }
        } else if ($(this).is(':checked') === true) {
          data.entries.append('<dt class="noData">' + label + '</dt><dd>&nbsp;</dd>');
        }
      });
  },

  section: function(section) {
    var data = {
      content: $('<td>'),
      entries: $('<dl>'),
      isValid: true
    };
    var useDefaults = false;

    if (section.data('review') != 'skip-title') {
      data.content.append('<h1>' + section.children('legend').text() + '</h1>');
    }

    // handle special sections
    if (section.data('id') == 'startPoint') {
      if (YaCyUi.DataStore.get($('#crawlingURL'), 'validation', 'valid')) {
        data.entries.append('<dt>' + YaCyPage.Report.text.confBookmarkTile +
          '</dt><dd>' + YaCyPage.CrawlStart.bookmarkTitle + '</dd>');
        var urlResult = YaCyPage.parts.startPoint.getUrlResults();
        if (urlResult.failed > 0 || urlResult.unknown > 0) {
          data.entries.append('<dt><s class="sym sym-warning"></s>Robots</dt>' +
            '<dd>You are only allowed to crawl ' +
            (urlResult.checked - urlResult.failed) + ' out of ' + urlResult.count +
            '<abbr>URLs (' + urlResult.unknown + ' are unknown)</abbr>.</dd>');
        }
      } else {
        data.isValid = false;
      }
    } else if (section.is('.toggleable.ycu-toggle-hidden')) {
      data.entries.append('<dt class="notConfigured">' +
        YaCyPage.Report.text.useDefaultConfig + '</dt><dd>&nbsp;</dd>')
        .addClass('notConfigured');
      data.content.addClass('notConfigured');
      useDefaults = true;
    } else {
      YaCyPage.Report.parseSection(section, data);
      if (data.entries.children().length === 0) {
        data.content = null;
      }
    }

    if (data.content !== null) {
      return {
        content: data.content.append(data.entries),
        defaults: useDefaults,
        valid: data.isValid
      };
    }
    return null;
  },

  group: function(content, group) {
    content.append('<tr class="group"><td colspan="2">' + group.children('legend').text() + '</td></tr>');
    var editButton = $('<button class="icon-edit">' +
      YaCyPage.Report.text.editButton + '</button>');
    var fixButton = $('<button class="error icon-fix">' +
      YaCyPage.Report.text.fixButton + '</button>');
    var changeDefaultsButton = $('<button class="icon-config">' + YaCyPage.Report.text.changeDefaultsButton + '</button>');

    group.children('fieldset.formSection').each(function() {
      $(this).uniqueId();
      var groupId = this.id;
      var sectionContent = YaCyPage.Report.section($(this));

      if (sectionContent !== null) {
        var button;

        if (sectionContent.defaults === true) {
          button = changeDefaultsButton.clone();
        } else if (sectionContent.valid === true) {
          button = editButton.clone();
        } else {
          button = fixButton.clone();
        }

        button.data('group-ref', groupId);

        content.append(
          $('<tr>')
          .append($('<td/>').html(button))
          .append(sectionContent.content)
        );
      }
    });
    return content;
  },

  generate: function() {
    var report = $('<table><colgroup><col class="button"/><col class="content"/></colgroup><tbody></tbody></table>');
    var content = report.children('tbody');

    // cache elements
    if (YaCyPage.Report.e === null) {
      YaCyPage.Report.e = {
        review: $('#formReview'),
        formGroups: $('fieldset.formGroup')
      };
    }

    report.on('click', 'button', function(evObj) {
      evObj.preventDefault();
      YaCyPage.Report.hide($(this).data('group-ref'));
    });

    YaCyPage.Report.e.review.children('.reviewContent').empty();

    YaCyPage.Report.e.formGroups.each(function() {
      content = YaCyPage.Report.group(content, $(this));
    });

    YaCyPage.Report.e.review.children('.reviewContent').append(report);
    // show report
    YaCyPage.Report.show();
  },

  show: function(callback) {
    $('#crawler').children().not($('#formControl'))
      .fadeOut('slow').promise().done(function() {
        $('#formControl').find('button[data-id="check"]').hide();
        $('#formControl').find('button[data-id="edit"]').show();
        YaCyPage.Report.e.review.fadeIn('slow').promise().done(function() {
          location.hash = "#" + YaCyPage.Report.e.review.attr('id');
          YaCyPage.Report.visible = true;
          if (typeof callback === 'function') {
            callback();
          }
        });
      });
  },

  hide: function(jumpTo, callback) {
    YaCyPage.Report.e.review.fadeOut('slow', function() {
      $('#crawler').children()
        .not(YaCyPage.Report.e.review).fadeIn('slow')
        .promise().done(function() {
          $('#formControl').find('button[data-id="check"]').show();
          $('#formControl').find('button[data-id="edit"]').hide();
          if (typeof jumpTo !== 'undefined' || jumpTo !== null) {
            YaCyUi.Form.digOut(jumpTo);
            location.hash = "#" + jumpTo;
          }
          YaCyPage.Report.visible = false;
          if (typeof callback === 'function') {
            callback();
          }
        });
    });
  },

  toggleDefaultsVisible: function() {
    var header = YaCyPage.Report.e.review.find('.reviewHeader');
    var content = YaCyPage.Report.e.review.find('.reviewContent');
    var state = header.data('defaultsVisible');
    var show;

    if (typeof state === 'undefined' || state === true) {
      header.find('button[data-id="showDefaults"]').show();
      header.find('button[data-id="hideDefaults"]').hide();
      header.data('defaultsVisible', false);
      show = false;
    } else {
      header.find('button[data-id="hideDefaults"]').show();
      header.find('button[data-id="showDefaults"]').hide();
      header.data('defaultsVisible', true);
      show = true;
    }

    if (show) {
      content.find('tr').show();
    } else {
      content.find('td.notConfigured').each(function() {
        var row = $(this).parents('tr');
        var head = row.prevUntil('tr.group').prev('tr').last();
        row.hide();
        if (head.nextUntil('tr.group').filter(':visible').length == 0) {
          head.hide();
        }
      });
    }
  }
};

YaCyPage.Parts.FormControl = function() {
  var self = this;
  this.e = {
    btnSubmit: $('#formControl').find('button[type="submit"]')
  };

  this.addInteractionHandler = function() {
    var btnCheck = $('#formControl').find('button[data-id="check"]');

    $('#formReview').find('button[data-id$="Defaults"]')
      .on('click', function(evObj) {
        evObj.preventDefault();
        YaCyPage.Report.toggleDefaultsVisible();
      });

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
      alert('ok - should submit now');
      YaCyUi.Form.Data.submit($('#crawler'));
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
  });
  validator
    .addElement($('#crawlingURL, #mustmatch, #ipMustmatch, #countryMustMatchList'), {
      validators: [{
        type: 'notEmpty',
        error: 'empty'
      }]
    })
    .addElement($('#indexmustmatch, #indexcontentmustmatch'), {
      validators: [{
        type: 'notEmpty',
        failType: 'warning'
      }]
    })
    .addElement($('#crawlingURL'), {
      validators: [{
        type: 'url',
        error: 'invalid'
      }, {
        type: 'urlProtocol',
        protocols: ['https?', 'file', 'ftp', 'smb'],
        error: 'protocol'
      }]
    })
    .addElement($('#countryMustMatchList'), {
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
        var btnRobotsAndStructure = $('#startPoint').
        find('button[data-id="robotsAndStructure"]');
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
  }
};