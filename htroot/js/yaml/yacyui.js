/*jslint browser:true */
/*global YaCyUi:true, $:true, console:true */
"use strict";

var YaCyUi = YaCyUi || {}; // UI namespace
var YaCyPage = YaCyPage || {}; // Single page namespace
YaCyPage.validationDelay = 700; // time (in ms) to wait before validation

YaCyUi.error = function (source, message) {
  var err = new Error();
  err.name = 'YaCyUi: ' + source + ' - Error';
  err.message = message;
  throw(err);
};

/** Function space for setting up the UI API. */
YaCyUi.Func = {};

/** Wrapper for jQueries event framework. */
YaCyUi.Event = {
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
YaCyUi.DataStore = {
  prefix: 'ycu',

  /** Get the data attached to an element.
    * @param {jQuery} Element
    * @param {String} Namespace
    * @param {String} The key for wich the value should be retrieved. If
    *   omitted, the whole namespace content will be returned. */
  get: function(jObj, space, key) {
    var data = jObj.data(YaCyUi.DataStore.prefix);

    if (typeof data === 'undefined' || !(space in data)) {
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
    triggerEvent = typeof triggerEvent !== 'boolean' ?
    true : triggerEvent;

    // do nothing, if no namespace was given
    if (!('space' in conf)) {
      return;
    }

    // does element have data set?
    if (typeof data === 'undefined') {
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

YaCyUi.init = function() {
  var modules = {};

  var devInfoBtn = $('<button>hide</button>');
  devInfoBtn.on('click', function() {
    $(this).parent().remove();
  });
  $('#devInfo').prepend(devInfoBtn).prepend('<u><strong>Developer notice:</strong></u>&nbsp;');

  // Create buttonsets
  $('.buttonSet').each(function() {
    $(this).buttonset();
  });

  // create main menu
  YaCyUi.MainMenu = new YaCyUi.Func.MainMenu();

  // init form element functions, if forms are available
  if ($('form').size() > 0) {
    modules.form = YaCyUi.Func.Form;
    modules.form.init();
  }

  // API bubbles
  var apiInfo = $('#api').children('div');
  if (apiInfo.size() > 0) {
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

  // initialize page script when all modules are loaded (if any)
  if (typeof YaCyPage.init === 'function') {
    var t = setInterval(function() {
      for (var module in modules) {
        if (!modules[module].loaded) {
          return;
        }
      }
      clearInterval(t);
      delete YaCyUi.Func; // unload function space
      YaCyPage.init();
    }, 125);
  } else {
    delete YaCyUi.Func; // unload function space
  }
};