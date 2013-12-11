/*jslint browser:true */
/*global YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

if (typeof YaCyUi !== 'undefined') {
  console.debug('YaCyUi already loaded!');
}

var YaCyUi = YaCyUi || {}; // UI namespace
var YaCyPage = YaCyPage || {}; // Single page namespace
YaCyPage.validationDelay = 700; // time (in ms) to wait before validation

YaCyUi.error = YaCyUi.error || function(source, message, obj) {
  if (console) {
    console.error('YaCyUi: ' + source + ': ' + message, obj);
  }
  var err = new Error();
  err.name = 'YaCyUi: ' + source + ' - Error';
  err.message = message;
  throw (err);
};

/** Function space for setting up the UI API. */
YaCyUi.Func = YaCyUi.Func || {};

/** Wrapper for jQueries event framework. */
YaCyUi.Event = YaCyUi.Event || {
  /** Trigger an document wide event.
   * @param {string} Event name, will be prefixed with 'ycu-'
   * @param {array} Event data */
  trigger: function(eventName, eventData) {
    $(document).trigger('ycu-' + eventName, eventData);
  },

  handle: function(eventName, func) {
    $(document).on('ycu-' + eventName, func);
  }
};

/** Custom wrapper for jQueries data() functions . */
YaCyUi.DataStore = YaCyUi.DataStore || {
  prefix: 'ycu',

  /** Get the data attached to an element.
   * @param {jQuery} Element
   * @param {String} Namespace
   * @param {String} The key for wich the value should be retrieved. If
   *   omitted, the whole namespace content will be returned. */
  get: function(jObj, space, key) {
    var data = jObj.data(YaCyUi.DataStore.prefix);

    if (typeof data === 'undefined' || data === null || !(space in data)) {
      return null;
    }

    if (typeof key === 'undefined' || key === null) {
      return data[space];
    }
    if (typeof data[space][key] !== 'undefined') {
      return data[space][key];
    }

    // nothing found
    return null;
  },

  /** Attach data to an element.
   * @param {jQuery} Element
   * @param {Object} Data to set
   *   space {String}: Namespace
   *   data {Object}: Key/values to set
   * @param {boolean} If true a change event is triggered. (default: true) */
  set: function(jObj, conf, triggerEvent) {
    var data = jObj.data(YaCyUi.DataStore.prefix);
    triggerEvent = typeof triggerEvent !== 'boolean' ? true : triggerEvent;

    // do nothing, if no namespace was given
    if (!('space' in conf)) {
      return;
    }

    // does element have data set?
    if (typeof data === 'undefined' || data === null) {
      // create data space if none
      data = {};
    }

    // namespace already in data?
    if (!(conf.space in data)) {
      // create space in data
      data[conf.space] = {};
    }
    $.extend(data[conf.space], conf.data);

    // set new data
    jObj.data(YaCyUi.DataStore.prefix, data);

    if (triggerEvent) {
      YaCyUi.Event.trigger('data-change', [conf.space, jObj, data[conf.space]]);
    }
  },

  /** Deletes data attached to an element.
   * @param {jQuery} Element
   * @param {Key} Namespace
   * @param {Key} The key of the entry to delete, or a space seperated
   *   list of keys. If omitted, the whole namespace will be deleted. */
  del: function(jObj, space, key) {
    var data = jObj.data(YaCyUi.DataStore.prefix);
    var changed = false;

    if (typeof data === 'undefined' || !(space in data)) {
      return;
    }

    if (typeof key === 'undefined' || key === null) {
      delete data[space];
    } else {
      if (key.trim().length === 0) {
        return;
      }

      var keys = key.split(' ');
      var keyIdx = keys.length;
      while (keyIdx--) {
        delete data[space][keys[keyIdx]];
      }
    }

    jObj.data(YaCyUi.DataStore.prefix, data);
  }
};

YaCyUi.Messages = YaCyUi.Messages || {
  init: function() {
    var messages = $('.ycu-message');
    var closeBtn = $('<span class="closeBtn"></span>');
    if (messages.length > 0) {
      messages.each(function() {
        var msgItem = $(this);
        msgItem.prepend(closeBtn.clone().on('click', function() {
          msgItem.slideUp('slow');
        }));
        if (msgItem.is('.ycu-message-error')) {
          msgItem.prepend('<span class="type">ERROR:</span>');
        } else if (msgItem.is('.ycu-message-warning')) {
          msgItem.prepend('<span class="type">WARNING:</span>');
        } else {
          msgItem.prepend('<span class="type">&nbsp;</span>');
        }
        msgItem.appendTo('#ycu-messages');
      });

      $('#ycu-messages').show();
      // jump to messages
      location.hash = '#ycu-messages';
    }
  }
};

YaCyUi.modules = YaCyUi.modules || {};
YaCyUi.init = function() {
  // temporary developer info controls
  if ($('#devInfo').length > 0) {
    var devInfoBtn = $('<button style="float:right;">hide</button>');
    devInfoBtn.on('click', function() {
      $(this).parent().remove();
    });
    var data = $('#devInfo').data();
    if ('done' in data) {
      $('#devInfo').prepend('<br/>  <strong>DONE:</strong> ' + data.done.replace(' ', ', '));
    }
    if ('skip' in data) {
      $('#devInfo').prepend('<br/>  <strong>SKIP:</strong> ' + data.skip.replace(' ', ', '));
    }
    if ('todo' in data) {
      $('#devInfo').prepend('<br/>  <strong>TODO:</strong> ' + data.todo.replace(' ', ', '));
    }
    if ('skip' in data || 'done' in data || 'todo' in data) {
      $('#devInfo').prepend('<br/><strong>STATUS</strong>')
    }
    $('#devInfo').prepend('<u><strong>Developer notice:</strong></u><br/>').prepend(devInfoBtn);
    $('#devInfo').append('<div class="clear"></div>');
  }

  // create main menu, if loaded
  if ('MainMenu' in YaCyUi.Func) {
    YaCyUi.MainMenu = YaCyUi.MainMenu || new YaCyUi.Func.MainMenu();
  }

  // init form element functions, if forms are available
  if ($('form').length > 0 && typeof YaCyUi.modules.form === 'undefined') {
    YaCyUi.modules.form = YaCyUi.Func.Form;
    YaCyUi.modules.form.init();
  }

  // API bubbles: more info dialog
  var apiInfo = $('#api').children('div');
  if (apiInfo.length > 0) {
    var apiBubble = $('#api').children('img');
    apiBubble.addClass('hasInfo');
    var apiInfoDialog = apiInfo.dialog({
      closeOnEscape: true,
      autoOpen: false,
      autoResize: true,
      modal: true,
      title: 'YaCy API',
      buttons: {
        Ok: function() {
          $(this).dialog('close');
        }
      }
    });
    apiBubble.on('click', function() {
      apiInfoDialog.dialog('open');
    });
  }

  // make tables sortable, if any and script is loaded
  if (jQuery().tablesorter) {
    // custom table data extraction for date sorting - TODO: is this needed?
    var parseData = function(node) {
      var datePattern = /(\d{2}).(\d{2}).(\d{4})\s(\d{2}:\d{2}:\d{2})/;
      var dateReplace = '$3-$2-$1 $4';
      var nodeData = node.innerHTML;
      if (nodeData.match(datePattern)) {
        return nodeData.replace(datePattern, dateReplace);
      }
      return nodeData;
    };
    var sortTables = $('table.sortable');
    if (sortTables.length > 0) {
      sortTables.each(function() {
        var headers = {};
        // exclude checkAll columns and those explicitly not to be sorted
        $(this).find('th.noSort, th input[data-action~="checkAllToggle"]').each(
          function() {
            // set noSort for checkAll columns
            if (this.tagName.toLowerCase() == 'input') {
              $(this).parent('th').addClass('noSort');
            }
            headers[$(this).index()] = {
              sorter: false
            };
          });
        // setup sorter
        $(this).tablesorter({
          headers: headers,
          textExtraction: parseData
        });
      });
    }
  }

  // initialize page script when all modules are loaded (if any)
  var t = setInterval(function() {
    for (var module in YaCyUi.modules) {
      if (!YaCyUi.modules[module].loaded) {
        return;
      }
    }
    clearInterval(t);
    delete YaCyUi.Func; // unload function space
    if (typeof YaCyPage.init === 'function') {
      console.debug("YaCyUi.init: initializing page");
      YaCyPage.init();
    }
    YaCyUi.initFinish(); // finalize init
  }, 125);
};

YaCyUi.initFinish = function() {
  // move important messages to top
  YaCyUi.Messages.init();

  // show footer, if it has content
  if ($('footer').children().length > 0) {
    $('footer').show();
    $('main').css('margin-bottom', $('footer').height() + 'px');
  }

  // symbols content, actual styles are set via CSS
  // TODO: ugly! replace this
  $('#formResults').find('p.error').prepend('<s class="sym sym-error" title="Error"></s>');
  $('#formResults').find('p.warning').prepend('<s class="sym sym-warning" title="Warning"></s>');
  $('#formResults').find('p.hint').prepend('<s class="sym sym-hint" title="Hint"></s>');
  var symbols = $('main s.sym');
  symbols.filter('.sym-error, .sym-warning, .sym-hint').html('<i></i><i></i>');

  // resize iframes to show the full content
  $('iframe.autoSize').each(function() {
    $(this).load(function() {
      var height = $(this.contentWindow.document).height();
      console.debug("height", $(this.contentWindow.document).find('#feedBox').attr('style'), height);
      $(this).css({
        height: height
      });
    });
  });

  // finished loading
  YaCyUi.loaded = true;
};