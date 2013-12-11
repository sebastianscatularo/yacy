/*jslint browser:true */
/*global YaCyUi:true, YaCyPage:true, $:true, jQuery:true, console:true */
"use strict";
/** Initialize the page. */
YaCyPage.init = function() {
  var validator = new YaCyUi.Form.Validator({
    toggle: $('#submitSettings'),
    display: $('#ycu-error-count'),
    showLink: true,
    onload: true
  });

  if ($('#settingsServerAccess').length > 0) {
    validator.addElement($('#staticIP'), {
      validators: [{
        type: 'ipv4'
      }]
    });
  } else if ($('#settingsProxyAccess').length > 0) {
    validator.addElement($('#port'), {
      validators: [{
        type: 'func',
        func: function(data) {
          switch (YaCyUi.Form.ValidatorFunc.range(data, 0, 65535)) {
            case 0: // in range
              return true;
              break;
            case -1: // NAN
              // ip (with port)?
              if (YaCyUi.Form.ValidatorFunc.ipv4(data, data.indexOf(':') > -1 ? true : false)) {
                return true;
              } else {
                var parts = data.split(':');
                // name/interface + port?
                if (parts.length == 2 && YaCyUi.Form.ValidatorFunc.range(parts[1], 0, 65535) === 0) {
                  return true;
                }
                return false;
              }
              break;
            default: // empty or out of range
              return false;
              break;
          }
        }
      }]
    });
  } else if ($('#settingsCrawler').length > 0) {
    validator.addElement($('#crawlerClientTimeout'), {
      validators: [{
        type: 'range',
        min: 0,
        tooLow: 'range'
      }]
    }).addElement($('#crawlerHttpMaxFileSize, #crawlerFtpMaxFileSize, #crawlerSmbMaxFileSize, #crawlerFileMaxFileSize'), {
      validators: [{
        type: 'range',
        min: -1,
        tooLow: 'range'
      }]
    });
  } else if ($('#settingsProxy').length > 0) {
    validator.addElement($('#host'), {
      validators: [{
        type: 'ipv4'
      }]
    }).addElement($('#port'), {
      validators: [{
        type: 'range',
        min: 0,
        max: 65535,
        tooLow: 'range',
        tooHigh: 'range'
      }]
    }).addElement($('#user'), {
      validators: [{
        type: 'notEmpty'
      }]
    });
  } else if ($('#settingsSeed').length > 0) {
    $('#submitUploadMethod').prop('disabled', true);
    $('#method').on('change', function() {
      $('#submitUploadMethod').prop('disabled', false).show();
    });

    var validatorFtp = new YaCyUi.Form.Validator({
      toggle: $('#submitSettingsFtp'),
      display: $('#ycu-error-count-ftp'),
      onload: true
    }).addElement($('#ftpServer, #ftpPath, #ftpUser, #ftpPwd'), {
      validators: [{
        type: 'notEmpty'
      }]
    });

    var validatorScp = new YaCyUi.Form.Validator({
      toggle: $('#submitSettingsScp'),
      display: $('#ycu-error-count-scp'),
      onload: true
    }).addElement($('#scpServer, #scpPath, #scpUser, #scpPwd'), {
      validators: [{
        type: 'notEmpty'
      }]
    }).addElement($('#scpPort'), {
      validators: [{
        type: 'range',
        min: 0,
        max: 65535,
        tooLow: 'range',
        tooHigh: 'range'
      }]
    });

    var validatorFs = new YaCyUi.Form.Validator({
      toggle: $('#submitSettingsFs'),
      display: $('#ycu-error-count-fs'),
      showLink: false,
      onload: true
    }).addElement($('#fsPath'), {
      validators: [{
        type: 'notEmpty'
      }]
    });

    validator.addElement($('#url'), {
      validators: [{
        type: 'notEmpty',
        error: 'empty'
      }, {
        type: 'url',
        error: 'invalid'
      }, {
        type: 'urlProtocol',
        protocols: ['https?', 'ftp'],
        error: 'protocol'
      }]
    });
  } else if ($('#settingsMessageForwarding').length > 0) {
    validator.addElement($('#fwdCmd, #fwdTo'), {
      validators: [{
        type: 'notEmpty'
      }]
    });
  }
};