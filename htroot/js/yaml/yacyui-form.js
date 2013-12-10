/*jslint browser:true */
/*global YaCyUi:true, $:true, console:true */
"use strict";

YaCyUi.Form = YaCyUi.Form || {
  /** Shows the content of a toggleable or collapseble fieldset.
   * @param {jQuery} The fieldset element */
  showFieldsetContent: function(fieldset) {
    if (fieldset.hasClass('toggleable') &&
      fieldset.hasClass('ycu-toggle-hidden')) {
      YaCyUi.Form.ToggleableFormSection.show(fieldset);
    } else if (fieldset.hasClass('collapsible') &&
      fieldset.hasClass('collapsed')) {
      YaCyUi.Form.CollapseableFieldset.show(fieldset);
    }
  },

  /** Show an element contained in toggleable or collapseble fieldsets.
   * @param {string} Element id */
  digOut: function(id) {
    var e = $('#' + id);
    var fieldset = e.parents('fieldset').andSelf();

    // show parents
    fieldset.each(function() {
      if (this.tagName.toLowerCase() == 'fieldset') {
        YaCyUi.Form.showFieldsetContent($(this));
      }
    });
  }
};

YaCyUi.Func.Form = YaCyUi.Func.Form || {
  loaded: false,

  /** Initialize all form functions. */
  init: function(finishCallback) {
    var modules = {}; // gather modules that need initialization time

    // collapsible fieldsets
    var collapseableFieldsets = $('fieldset.collapsible');
    if (collapseableFieldsets.length > 0) {
      modules.CollapseableFieldset =
        new YaCyUi.Func.Form.CollapseableFieldset(collapseableFieldsets, true);
    }

    // general objects
    modules.Validate = new YaCyUi.Func.Form.Validate();

    // form help
    var helpElements = $('fieldset.formSection > .formHelp');
    if (helpElements.length > 0) {
      YaCyUi.Form.SectionHelp =
        new YaCyUi.Func.Form.SectionHelp(helpElements);
    }

    // form hints
    var hints = $('.formHint');
    if (hints.length > 0) {
      modules.Hints = new YaCyUi.Func.Form.Hints(hints);
    }

    // toggleable form sections - run after hints, to getdynamic help items
    var toggleableFormSections = $('fieldset.toggleable');
    if (toggleableFormSections.length > 0) {
      modules.ToggleableFormSection =
        new YaCyUi.Func.Form.ToggleableFormSection(toggleableFormSections);
    }

    // auto resizing of textareas
    var textareas = $('textarea');
    if (textareas.length > 0) {
      YaCyUi.Form.ResizeableTextarea =
        new YaCyUi.Func.Form.ResizeableTextarea();
      YaCyUi.Form.ResizeableTextarea.autoResize(textareas);
    }

    // form elements (checkbox/radio) that enable/disable other elements
    var toggleableElements = $('button, fieldset, input, select, textarea')
      .filter('[data-toggle-id]');
    if (toggleableElements.length > 0) {
      modules.ToggleableFormElement =
        new YaCyUi.Func.Form.ToggleableFormElement(toggleableElements);
    }

    YaCyUi.Form.Data = new YaCyUi.Func.Form.Data();

    YaCyUi.Form.Button = new YaCyUi.Func.Form.Button();

    // init jQuery UI spinner elements
    $('.spinner').each(function() {
      $(this).spinner({
        min: typeof $(this).data('min') === 'undefined' ? null : $(this).data('min'),
        max: typeof $(this).data('max') === 'undefined' ? null : $(this).data('max'),
      });
    });

    // set checkAll button action for tables with checkboxes
    $('table th input[type="checkbox"][data-action~="checkAllToggle"]').on('change', function() {
      YaCyUi.Tools.checkAllBoxes($(this).closest('table'), $(this).is(':checked'));
    });

    // wait until all modules that need initialization are loaded
    var t = window.setInterval(function() {
      var module;
      for (module in modules) {
        if (!modules[module].loaded) {
          return;
        } else {
          console.debug("YaCyUi.Form.Module: " + module + " loaded");
        }
      }
      // done loading
      window.clearInterval(t);

      // add all initialized modules to namespace
      for (module in modules) {
        YaCyUi.Form[module] = modules[module];
      }

      console.debug("YaCyUi.Form: finished loading");
      YaCyUi.Func.Form.loaded = true;
    }, 125);
  }
};

/** Make form fieldsets collapseable.
 * @param {jQuery} Fieldset elements
 * @param {boolean} If true fieldsets are initially collapsed */
YaCyUi.Func.Form.CollapseableFieldset = YaCyUi.Func.Form.CollapseableFieldset ||
  function(fieldsets, initialHidden) {
    var self = this;
    this.loaded = false;

    /** Add collapse click handler to fieldset legend.
     * @param {jQuery} Elements (fieldsets) to collapse
     * @param {boolean} If false, elements are not initial hidden.
     * (default: true) */

    function addHandler(fieldsets, hide) {
      hide = typeof hide !== 'boolean' ? true : hide;

      fieldsets.each(function() {
        var fieldset = $(this);
        fieldset
          .children('legend')
          .on('click', function() {
            self.toggle(fieldset);
          });
      });

      if (hide) {
        fieldsets
          .addClass('collapsed')
          .children().not('legend').hide()
          .promise().done(function() {
            self.loaded = true; // init done
          });
      } else {
        self.loaded = true; // init done
      }
    }

    // init
    addHandler(fieldsets, initialHidden);
};
YaCyUi.Func.Form.CollapseableFieldset.prototype = {
  /** Shows a fieldset.
   * @param {jQuery} fieldset element */
  show: function(fieldset) {
    fieldset
      .removeClass('collapsed')
      .children().not('legend, .hidden')
      .slideDown('slow');
    YaCyUi.DataStore.set(fieldset, {
      space: 'toggle',
      data: {
        state: 'visible'
      }
    });
  },

  /** Hides a fieldset.
   * @param {jQuery} fieldset element */
  hide: function(fieldset) {
    fieldset
      .addClass('collapsed')
      .children().not('legend')
      .slideUp('slow');
    YaCyUi.DataStore.set(fieldset, {
      space: 'toggle',
      data: {
        state: 'hidden'
      }
    });
  },

  /** Toggles a fieldset.
   * @param {jQuery} fieldset element */
  toggle: function(fieldset) {
    fieldset.hasClass('collapsed') ? this.show(fieldset) : this.hide(fieldset);
  }
};

/** Toggle form sections by enabling/disableing.
 * @param {jQuery} form section elements (<dl/>) */
YaCyUi.Func.Form.ToggleableFormSection = YaCyUi.Func.Form.ToggleableFormSection ||
  function(formSections) {
    var self = this;
    this.loaded = false;
    var sectionsToHideCount = 0;
    var text = {
      configure: 'configure',
      activateAndConfigure: 'activate &amp; configure',
      revert: 'use defaults',
      turnOff: 'deactivate'
    };

    function wrapContent(formSection) {
      var toggleType = formSection.data('toggle-type');
      var content = formSection.children('.content');

      content.children().wrapAll('<div class="ycu-toggle content hidden"></div>');

      var buttons;
      if (toggleType !== undefined && toggleType == 'activate') {
        buttons = '<button class="on icon-config">' + text.activateAndConfigure +
          '</button><button class="off hidden icon-stop">' +
          text.turnOff + '</button>';
      } else {
        buttons = '<button class="on icon-config">' + text.configure +
          '</button><button class="off hidden icon-revert">' +
          text.revert + '</button>';
      }

      var toggles = content.prepend(
        '<div class="ycu-toggle-controls">' + buttons +
        '</div>').children('.ycu-toggle-controls').find('button');

      toggles.each(function() {
        var action = $(this).hasClass('on') ? 'show' : 'hide';
        $(this).on('click', function(evObj) {
          evObj.preventDefault();
          self[action](formSection);
        });
      });
      self.hide(formSection, 0);
    }

    function init() {
      sectionsToHideCount = formSections.length;
      formSections.each(function() {
        wrapContent($(this));
      });

      // loader check
      var t = window.setInterval(function() {
        if (self.sectionsToHideCount > 0) {
          return;
        }
        window.clearInterval(t);
        self.loaded = true;
      }, 125);
    }

    init();
};
YaCyUi.Func.Form.ToggleableFormSection.prototype = {
  /** Shows a form section.
   * @param {jQuery} Formsection element (<dl/>) */
  show: function(formSection) {
    var content = formSection.find('.ycu-toggle');
    var toggles = formSection.find('.ycu-toggle-controls').children('button');
    formSection
      .addClass('ycu-toggle-visible')
      .removeClass('ycu-toggle-hidden');
    toggles.each(function() {
      $(this).hasClass('on') ? $(this).hide() : $(this).show();
    });
    content.slideDown('slow', function() {
      $(this).removeClass('hidden');
      YaCyUi.DataStore.set(content.parent(), {
        space: 'toggle',
        data: {
          state: 'visible'
        }
      });
      var header = formSection.children('legend');
      // show tooltip, if any
      if (typeof header.data('uiTooltip') !== 'undefined') {
        header.tooltip("option", "disabled", false);
      }
      // enable contained form elements
      var inputs = content.find('input, textarea, select');
      inputs.prop('disabled', false);
      YaCyUi.Event.trigger('toggle-section-elements', ['enable', inputs]);
    });
  },

  /** Hides a form section.
   * @param {jQuery} Formsection element (<dl/>)
   * @param {mixed} Duration parameter for jQueries hide() function. (default:
   * slow) */
  hide: function(formSection, speed) {
    var self = this;
    speed = typeof speed === 'undefined' ? 'slow' : speed;
    var content = formSection.find('.ycu-toggle');
    var toggles = formSection.find('.ycu-toggle-controls').children('button');

    content.slideUp(speed, function() {
      toggles.each(function() {
        $(this).hasClass('on') ? $(this).show() : $(this).hide();
      });
      formSection
        .addClass('ycu-toggle-hidden')
        .removeClass('ycu-toggle-visible');

      // disable contained form elements
      var inputs = content.find('input, textarea, select');
      inputs.prop('disabled', true);
      YaCyUi.Event.trigger('toggle-section-elements', ['disable', inputs]);

      YaCyUi.DataStore.set(content.parent(), {
        space: 'toggle',
        data: {
          state: 'hidden'
        }
      });
      var header = formSection.children('legend');
      // hide tooltip, if any
      if (typeof header.data('uiTooltip') !== 'undefined') {
        header.tooltip("option", "disabled", true);
      }

      if (!self.loaded) {
        self.sectionsToHideCount--;
      }
    });
  }
};

/** Help for form sections.
 * @param {jQuery} help elements */
YaCyUi.Func.Form.SectionHelp = YaCyUi.Func.Form.SectionHelp ||
  function(formHelpElements) {
    var helpMsg = 'Click to get help for this item.';

    formHelpElements.each(function() {
      var helpElement;
      var header = $(this).parent();
      var section = header.children('legend');

      section
        .addClass('hasHelp')
        .attr('title', helpMsg)
        .on('click', function() {
          if (YaCyUi.DataStore.get(
            header.children('.content'), 'toggle', 'state') != 'hidden') {
            // don't show help dialog, if content is hidden
            helpElement.dialog('open');
          }
        })
        .tooltip();
      helpElement = $(this).dialog({
        closeOnEscape: true,
        autoOpen: false,
        autoResize: true,
        modal: true,
        dialogClass: 'ycu-ext-hint-dlg',
        title: section.text(),
        buttons: {
          Ok: function() {
            $(this).dialog('close');
          }
        }
      });
    });
};

/** Hints for form elements. */
YaCyUi.Func.Form.Hints = YaCyUi.Func.Form.Hints || function(hintElements) {
  var self = this;
  this.loaded = false;

  function init() {
    YaCyUi.Event.handle('data-change', function(ev, space, formElement, data) {
      if (space === 'hints') {
        self.private.handleDataEvent(self, formElement, data);
      }
    });

    hintElements.each(function() {
      var prev = $(this).prevAll('input[type="text"], input[type="password"], textarea').first();
      var hintElement = $(this);
      hintElement.append('<div class="clear"></div>')
        .prepend('<div class="tip"><i class="fa fa-caret-up"></i></div>');
      if (prev.length > 0) {
        YaCyUi.DataStore.set(prev, {
          space: 'hints',
          data: {
            element: hintElement
          }
        });
        prev.on('focus', function() {
          YaCyUi.DataStore.set(prev, {
            space: 'hints',
            data: {
              help: 'auto',
              show: true
            }
          });
        }).on('blur', function() {
          YaCyUi.DataStore.set(prev, {
            space: 'hints',
            data: {
              show: false
            }
          });
        });
      }
    }).promise().done(function() {
      self.loaded = true;
    });
  }

  init();
};
YaCyUi.Func.Form.Hints.prototype = {
  // private function
  private: {
    handleDataEvent: function(scope, formElement, data) {
      switch (data.show) {
        case true:
          YaCyUi.Form.Hints.show(formElement);
          break;
        case false:
          YaCyUi.Form.Hints.hide(formElement, {
            triggerEvent: false
          });
          break;
        default:
          scope.update(formElement);
          break;
      }
    },

    /** Toggle hint elements based on validation data.
     * @param {jQuery} Form element
     * @return {int} Number of hints that will be shown */
    set: function(formElement) {
      var data = YaCyUi.DataStore.get(formElement, 'hints');
      var hideDelay = 10;
      var hint = YaCyUi.DataStore.get(formElement, 'hints', 'element');
      var state = { // previous hint states
        ok: false,
        warning: false,
        help: false,
        error: false
      };

      // check if element has hints defined
      if (data === null || hint.length === 0) {
        return 0;
      }

      var e = { // gather hint elements
        ok: hint.children('.ok'),
        warning: hint.children('.warning'),
        help: hint.children('.help'),
        error: hint.children('.error')
      };

      // show/hide a specific hint
      var setHint = function(name) {
        if (e[name].length > 0 && name in data) {
          if (data[name] === true) { // only one message to show
            show.push([e[name].get(0), state[name]]);
            state[name] = true;
          } else { // specific message
            e[name].each(function() {
              if ($(this).data('id') === data[name]) {
                show.push([this, state[name]]);
                state[name] = true;
              } else {
                hide.push(this);
              }
            });
          }
        } else {
          hide.push(e[name]);
          state[name] = false;
        }
      };

      // check if previous state was saved
      if ('_' in data && 'hints' in data._) {
        state = data._.hints;
      }

      var hide = [];
      var show = [];

      // error
      setHint('error');
      // ok
      setHint('ok');
      // warning
      setHint('warning');
      // help
      if (e.help.length > 0 && 'help' in data) {
        switch (data.help) {
          case true:
            show.push([e.help, state.help]);
            state.help = true;
            break;
          case false:
            hide.push(e.help);
            state.help = false;
            break;
          case 'auto':
            if (YaCyUi.Form.Validate.isValid(formElement) === true) {
              hide.push(e.help);
              state.help = false;
            } else {
              show.push([e.help, state.help]);
              state.help = true;
            }
            break;
        }
      } else {
        hide.push(e.help);
        state.help = false;
      }

      // set elements CSS classes according to hints
      var addClass = '';
      var removeClass = '';
      for (var key in state) {
        if (state[key]) {
          addClass += ' ' + key;
        } else {
          removeClass += ' ' + key;
        }
      }
      hint.addClass(addClass.trim());
      hint.removeClass(removeClass.trim());

      // smoothness: hide all first (animated), then show new
      $(hide).map(function() {
        return $(this).toArray();
      }).stop().slideUp(hideDelay).promise().done(function() {
        $.each(show, function(i, e) {
          var jObj = $(e[0]);
          e[1] === true ? jObj.show() : jObj.slideDown(350);
        });
      });

      show.length > 0 ? hint.removeClass('none') : hint.addClass('none');
      YaCyUi.DataStore.set(formElement, {
        space: 'hints',
        data: {
          _: {
            hints: state
          }
        }
      }, false);

      return show.length;
    }
  },

  /** Update the hints with new values. These updates will only be visible,
   * if the hint area is already shown.
   * @param {jQuery} Form elements */
  update: function(formElements) {
    formElements.each(function() {
      if ($(this).next('.formHint').is(':visible')) {
        YaCyUi.Form.Hints.show($(this));
      }
    });
  },

  /** Shows the hint area, if there's anything to show.
   * @param {jQuery} Form elements */
  show: function(formElements) {
    var self = this;
    formElements.each(function() {
      if (self.private.set($(this)) > 0) {
        var leftOffset = $(this).offset().left;
        YaCyUi.DataStore.get($(this), 'hints', 'element').slideDown('fast', function() {
          var offset = leftOffset - $(this).offset().left;
          if (offset > 0) {
            $(this).find('.error, .help, .ok, .warning').css('min-width', offset + 'px');
            if ($(this).offset().left < leftOffset) {
              $(this).find('.tip').css('margin-left', offset + 'px');
            }
          }
        });
      }
    });
  },

  /** Hides the hint area. If there are any warnings or erros they will stay
   * visible. Any other messages will be hidden. This can be overridden by
   * setting the force parameter.
   * @param {jQuery} Form elements
   * @param {object}:
   *   force {Boolean}: If true, the hint area will be forced to be hidden.
   *   (default: false)
   *   triggerEvent {Boolean}: If true data change events are triggered
   *   (default: true) */
  hide: function(formElements, param) {
    var self = this;
    param = param || {};
    param.force = 'force' in param ? param.force : false;
    param.triggerEvent = 'triggerEvent' in param ? param.triggerEvent : true;
    var elementData = {
      space: 'hints',
      data: {
        help: false,
        ok: false
      }
    };

    formElements.each(function() {
      YaCyUi.DataStore.set($(this), elementData, param.triggerEvent);
      if (self.private.set($(this)) > 0 && param.force !== true) {
        // there are still errors / warnings set
        return;
      }
      YaCyUi.DataStore.get($(this), 'hints', 'element').slideUp('slow');
    });
  }
};

YaCyUi.Func.Form.ResizeableTextarea = YaCyUi.Func.Form.ResizeableTextarea ||
  function() {};
YaCyUi.Func.Form.ResizeableTextarea.prototype = {
  /** Resize a textarea to it's content.
   * based on: http://stackoverflow.com/questions/2948230/auto-expand-a-textarea-using-jquery
   * @param {jQuery} Textarea elements to resize.
   * @param {int} Maximum height of the textarea (optional) */
  resize: function(textareas, maxHeight) {
    textareas.each(function() {
      var oHeight = $(this).outerHeight();
      var cHeight = this.scrollHeight +
        parseFloat($(this).css("borderTopWidth")) +
        parseFloat($(this).css("borderBottomWidth"));

      while (oHeight < cHeight) {
        if (maxHeight == $(this).height()) {
          break;
        }
        $(this).height($(this).height() + 1);
        oHeight = $(this).outerHeight();
        cHeight = this.scrollHeight +
          parseFloat($(this).css("borderTopWidth")) +
          parseFloat($(this).css("borderBottomWidth"));
      }
    });
  },

  /** Make textfields auto-reize upon adding content.
   * @param {jQuery}: Elements to attach the auto-resize hander to
   * @param {Object}:
   *   maxHeight {int}: Maximum height of the textarea (optional)
   *   initial {boolean}: Do initial resizing (optional, default: true)
   *   maxHeight {int}: Maximum height of the textareas (default: 150) */
  autoResize: function(textareas, param) {
    param = param || {};
    param.initial = param.initial || true;
    param.maxHeight = param.maxHeight || 150;
    textareas.each(function() {
      if (param.initial) {
        YaCyUi.Form.ResizeableTextarea.resize($(this), param.maxHeight);
      }
      $(this).on('keyup', function() {
        YaCyUi.Form.ResizeableTextarea.resize($(this), param.maxHeight);
      });
    });
  }
};

YaCyUi.Func.Form.Validate = YaCyUi.Func.Form.Validate || function() {
  var self = this;
  this.loaded = false;
  this.dialog = null;

  function addValidationDialog() {
    var dialogHtml = '<div class="hidden" id="ycu-validation-dialog"></div>';
    $('body').append(dialogHtml);
    self.dialog = $('#ycu-validation-dialog').dialog({
      closeOnEscape: true,
      autoOpen: false,
      autoResize: true,
      modal: true,
      title: 'Validation results',
      buttons: {
        'Show errors': function() {
          $(this).dialog('close');
          self.private.dialogCallback.call(self, 'show');
        },
        Cancel: function() {
          $(this).dialog('close');
          self.private.dialogCallback.call(self, 'cancel');
        }
      }
    });
    self.loaded = true;
  }

  addValidationDialog();
};
YaCyUi.Func.Form.Validate.prototype = {
  // private functions
  private: {
    // selector for validable elements
    validableElements: 'textarea, input[type="text"]',

    /** Set elements as being valid.
     * @param {jQuery} Elements
     * @param {boolean} If true, set the elements data also. (default:
     * true) */
    setValid: function(formElements, setData) {
      setData = typeof setData !== 'boolean' ? true : setData;
      formElements.each(function() {
        // special case for spinner widget
        var parent = $(this).parent();
        if (parent.hasClass('ui-widget')) {
          parent.removeClass('invalid').addClass('valid');
        } else {
          $(this).removeClass('invalid').addClass('valid');
        }
        if (setData) {
          YaCyUi.DataStore.set($(this), {
            space: 'validation',
            data: {
              valid: true
            }
          });
        }
      });
      YaCyUi.Event.trigger('validation-state', ['valid', formElements]);
    },

    /** Set elements as being invalid.
     * @param {jQuery} Elements
     * @param {boolean} If true, set the elements data also. (default:
     * true) */
    setInvalid: function(formElements, setData) {
      setData = typeof setData !== 'boolean' ? true : setData;
      formElements.each(function() {
        // special case for spinner widget
        var parent = $(this).parent();
        if (parent.hasClass('ui-widget')) {
          parent.removeClass('valid').addClass('invalid');
        } else {
          $(this).removeClass('valid').addClass('invalid');
        }
        if (setData) {
          YaCyUi.DataStore.set($(this), {
            space: 'validation',
            data: {
              valid: false
            }
          });
        }
      });
      YaCyUi.Event.trigger('validation-state', ['invalid', formElements]);
    },

    /** Set elements as being not validated.
     * @param {jQuery} Elements
     * @param {boolean} If true, set the elements data also. (default:
     * true) */
    setNone: function(formElements, setData) {
      setData = typeof setData !== 'boolean' ? true : setData;
      formElements.each(function() {
        $(this).removeClass('valid invalid');
        if (setData) {
          YaCyUi.DataStore.set($(this), {
            space: 'validation',
            data: {
              valid: null
            }
          });
        }
      });
      YaCyUi.Event.trigger('validation-state', ['none', formElements]);
    },

    /** Sets the state for a single form element.
     * @param {jQuery} Target form element
     * @param {object} States object */
    setState: function(formElement, states) {
      var useHints = (typeof YaCyUi.Form.Hints !== 'undefined');
      var clear;

      if (useHints && 'hints' in states) {
        clear = 'clear' in states.hints ? states.hints.clear : false;
        if (clear === true) {
          YaCyUi.DataStore.del(formElement, 'hints', 'error ok show warning');
        }
        YaCyUi.DataStore.set(formElement, {
          space: 'hints',
          data: states.hints
        });
      }

      // validation
      if ('validation' in states && 'valid' in states.validation) {
        switch (states.validation.valid) {
          case true:
            this.private.setValid(formElement);
            break;
          case false:
            this.private.setInvalid(formElement);
            break;
          case null:
            this.private.setNone(formElement);
            break;
        }
      }
    },

    /** Callback function for validation results dialog. */
    dialogCallback: function(type) {
      if (type == 'show') {
        var position = null;
        var offset;
        for (var i = 0; i < this.elements.length; i++) {
          this.elements[i].uniqueId();
          var id = this.elements[i].attr('id');
          YaCyUi.Form.digOut(id);
          // jump to topmost element
          offset = this.elements[i].offset();
          if (position === null || offset.top < position) {
            position = offset.top;
            location.hash = '#' + id;
          }
        }
      }
    }
  },

  /** Add a validator to the given form element.
   * @param {jQuery} Form elements
   * @param {object}
   *   func {function} A validator function returning an object applicable
   *   for setting the elements hint and validation state. The function will
   *   be called with a jQuery object containing the validated element and
   *   the triggering event-object, or null if it was triggered on-load.
   *   delay {int} milliseconds to delay the validation (default: 700)
   *   events {String} space delimited events to trigger the validation.
   *   (default: focus, keyup)
   *   onload {boolean} Immediatly validate (default: false)
   *   scope {object} Scope for callback function
   */
  addValidator: function(formElements, param) {
    var validators = [];
    var self = this;

    if (!('func' in param) || typeof param.func !== 'function') {
      YaCyUi.error('YaCyUi.Form.Validate.addValidator',
        "Invalid or no validation function specified.");
    }
    if (!('events' in param)) {
      param.events = 'focus keyup';
    }
    if (!('delay' in param)) {
      param.delay = 700;
    }
    if (!('onload' in param)) {
      param.onload = false;
    }

    formElements.each(function() {
      var e = $(this);

      e.uniqueId(); // add an id attribute, if not already there
      YaCyUi.DataStore.set(e, {
        space: 'validation',
        data: {
          func: true
        }
      });
      self.private.setNone(e);

      e.on(param.events, function(evObj) {
        // check if key is changing input
        var skip = !YaCyUi.Tools.isTextChangingKeyEvent(evObj);

        if (!skip) {
          var timeout = e.data('validationTimeout');

          if (typeof timeout !== 'undefined') {
            window.clearTimeout(timeout);
          }
          timeout = window.setTimeout(function() {
            var state;
            if ('scope' in param) {
              state = param.func.call(param.scope, e, evObj);
            } else {
              state = param.func(e, evObj);
            }
            if (typeof state === 'object' && state !== null) {
              self.private.setState.call(self, e, state);
            }
          }, param.delay);
          e.data('validationTimeout', timeout);
        }
      });

      if (param.onload === true) {
        var state;
        if ('scope' in param) {
          state = param.func.call(param.scope, e, null);
        } else {
          state = param.func(e, null);
        }
        if (typeof state === 'object' && state !== null) {
          self.private.setState.call(self, e, state);
        }
      }
    });
  },

  /** Test if an element/all elements is/are valid.
   * @param {jQuery} Elements to test
   * @param {boolean} If true, not validated elements will be recognized
   *   as being invalid. (default: true)
   * @return {boolean} True if all given elements are valid, false
   *   otherwise. */
  isValid: function(formElements, strict) {
    strict = typeof strict !== 'boolean' ? true : strict;
    var allValid = true;
    formElements.each(function() {
      var state = YaCyUi.DataStore.get($(this), 'validation', 'valid');
      if (state === false || (state === null && strict)) {
        allValid = false;
        return null; // break loop
      }
    });
    return allValid;
  },

  /** Check, if an element is validable.
   * @param {jQuery} The element to test
   * @return {boolean} True, if validable */
  isValidable: function(formElement) {
    return YaCyUi.DataStore.get(formElement, 'validation', 'func') === true;
  },

  /** Show a validation results dialog.
   * @param {array} Array of elements (jQuery objects) to inlude in results.
   * Only those elements with errors will be handled. */
  showResultsDialog: function(elements) {
    this.dialog.empty();
    this.elements = [];
    var labels = [];
    var moreErrors = 0;
    for (var i = 0; i < elements.length; i++) {
      var element = elements[i];
      if (!YaCyUi.Form.Validate.isValid(element)) {
        var id = element.attr('id');
        var label = $('label[for="' + id + '"]');
        this.elements.push(element);
        if (label.length > 0) {
          labels.push(label.text());
        } else {
          moreErrors++;
        }
      }
    }
    if (labels.length > 0) {
      var labelList = $('<ul/>');
      this.dialog.append('<div>The following elements contain invalid values:</div><br/>');
      for (var j = 0; j < labels.length; j++) {
        labelList.append('<li>' + labels[j] + '</li>');
      }
      this.dialog.append(labelList);
      if (moreErrors > 0) {
        this.dialog.append('<div>And ' + moreErrors + ' more.</div>');
      }
    } else {
      if (moreErrors > 0) {
        this.dialog.append('<div>There are ' + moreErrors + ' errors.</div>');
      }
    }

    this.dialog.dialog('open');
  }
};

YaCyUi.Func.Form.ToggleableFormElement = YaCyUi.Func.Form.ToggleableFormElement ||
  function(toggleableElements) {
    var self = this;
    this.loaded = false;

    /** Enable a collection of elements and their associated labels.
     * @param {jQuery} Elements to enable */

    function toggleOn(elementsArray) {
      $.each(elementsArray, function(i, v) {
        var e = $(this);
        // toggle element..
        e.prop('disabled', false);
        // ..set focus if we should..
        if (e.data('toggle-focus') === true) {
          e.focus();
        }
        // ..and re-enable the associated label
        $('label[for="' + this.id + '"]').removeClass('disabled');
      });
    }

    /** Disable a collection of elements and their associated labels.
     * @param {jQuery} Elements to disable */

    function toggleOff(elementsArray) {
      $.each(elementsArray, function(i, v) {
        var e = $(this);
        // toggle element..
        e.prop('disabled', true);
        // ..and label
        $('label[for="' + this.id + '"]').addClass('disabled');
      });
    }

    function init() {
      var toggles = { // plain toggles go here
        groups: {} // toggles in groups go here
      };

      toggleableElements.each(function() {
        var toggleElementId = $(this).data('toggle-id');
        var toggleElement = $('#' + toggleElementId);
        var toggleElementGroup = toggleElement.attr('name').trim();

        if (toggleElement.length <= 0 || (toggleElement.attr('type') != 'radio' &&
          toggleElement.attr('type') != 'checkbox')) {
          return; // process next
        }

        if (toggleElementGroup.length > 0) {
          // toggle is part of a group
          if (!(toggleElementGroup in toggles.groups)) {
            // group is not already there
            toggles.groups[toggleElementGroup] = {};
            toggles.groups[toggleElementGroup][toggleElementId] =
              []; // elements to toggle
          } else if (!(toggleElementId in toggles.groups[toggleElementGroup])) {
            // group already seen, but not the toggle element
            toggles.groups[toggleElementGroup][toggleElementId] =
              []; // elements to toggle
          }
          toggles.groups[toggleElementGroup][toggleElementId].push($(this));
        } else {
          // single element, not part of a group
          if (!(toggleElementId in toggles)) {
            toggles[toggleElementId] = []; // elements to toggle
          }
          toggles[toggleElementId].push($(this));
          // attach handler
          toggleElement.on('change', function() {
            $(this).is(':checked') ? toggleOn(toggles[this.id]) :
              toggleOff(toggles[this.id]);
          });
        }

        // set current state
        toggleElement.is(':checked') ? toggleOn($(this)) : toggleOff($(this));
      });

      // attach a global event handler for all elements within an group
      $.each(toggles.groups, function(group, toggles) {
        $('input[name="' + group + '"]').on('change', function() {
          for (var toggleId in toggles) {
            if (toggleId == this.id && $(this).is(':checked')) {
              toggleOn(toggles[toggleId]);
            } else {
              toggleOff(toggles[toggleId]);
            }
          }
        });
      });

      self.loaded = true;
    }

    init();
};

YaCyUi.Func.Form.Data = YaCyUi.Func.Form.Data || function() {
  function getFormData(form) {
    var data = {};

    // find all input elements
    form.find('input:not([type="button"])').each(function() {
      var e = $(this);
      switch (this.type.toLowerCase()) {
        case 'checkbox':
        case 'radio':
          if (e.is(':checked')) {
            data[this.id] = true;
          } else {
            data[this.id] = false;
          }
          break;
        case 'text':
          var defaultValue = this.placeholder;
          var value = e.val().trim();
          if (typeof defaultValue !== 'undefined' && defaultValue === value) {
            data[this.id] = null;
          } else {
            data[this.id] = e.val().trim();
          }
          break;
      }
    });

    // find textareas
    form.find('textarea').each(function() {
      var e = $(this);
      data[this.id] = e.val().trim();
    });

    // exclude toggleable elements
    form.find('fieldset.toggleable').each(function() {
      var fieldset = $(this);
      var fieldToggleId = fieldset.data('toggle-fieldid');
      if (typeof fieldToggleId !== 'undefined') {
        if (fieldset.is('.ycu-toggle-hidden')) {
          data[fieldToggleId] = false;
          // unset inputs inside toggled element
          fieldset.find('input:not([type="button"])').each(function() {
            data[this.id] = null;
          });
        } else {
          data[fieldToggleId] = true;
        }
      }
    });
    return data;
  }

  /** Get the data for a form.
   * @param {jQuery} The form element */
  this.getData = function(form) {
    return getFormData(form);
  };

  /** Get the data for a form, cutted down to only those entries needed for
   * submission. Also all texts are URI encoded.
   * @param {jQuery} The form element */
  this.getSubmitData = function(form) {
    var data = getFormData(form);
    for (var key in data) {
      var val = data[key];
      if (val === false || val === null) {
        delete data[key];
      } else if (val === true) {
        data[key] = 1;
      } else {
        data[key] = encodeURIComponent(data[key]);
      }
    }
    return data;
  };

  /** Submit a form with all current data. */
  this.submit = function(form) {
    var submitUrl = form.attr('action');
    var data = this.getSubmitData(form);
    var url = '';
    for (var key in data) {
      url += key + "=" + data[key] + "&";
    }
    url = submitUrl + '?' + url.substring(0, url.length - 1);
    console.debug("submit URL", url);
  };
};

YaCyUi.Func.Form.Button = YaCyUi.Func.Form.Button || function() {};
YaCyUi.Func.Form.Button.prototype = {
  /** Switch between button icons.
   * @param {jQuery} The button element
   * @param {string} New icon class to set. If omitted the old icon will be set
   * again. */
  switchIcon: function(element, toName) {
    var classes = element.attr('class').split(' ');
    var iconName = '';
    for (var i = 0; i < classes.length; i++) {
      if ((/^icon-/).test(classes[i])) {
        iconName = classes[i];
        break;
      }
    }
    if (typeof toName !== 'string') {
      var oldIconName = YaCyUi.DataStore.get(element, 'buttonSwitch', 'icon');
      if (oldIconName !== null) {
        element.addClass(oldIconName).removeClass(iconName);
      }
    } else {
      YaCyUi.DataStore.set(element, {
        space: 'buttonSwitch',
        data: {
          icon: iconName
        }
      });
      element.removeClass(iconName).addClass(toName);
    }
  },

  /** Switch between button texts.
   * @param {jQuery} The button element
   * @param {string} New text to set. If omitted the old text will be set
   * again. */
  switchText: function(element, newText) {
    if (typeof newText !== 'string') {
      var oldText = YaCyUi.DataStore.get(element, 'buttonSwitch', 'text');
      if (oldText !== null) {
        element.text(oldText);
      }
    } else {
      YaCyUi.DataStore.set(element, {
        space: 'buttonSwitch',
        data: {
          text: element.text()
        }
      });
      element.text(newText);
    }
  }
};

YaCyUi.Form.ValidatorFunc = YaCyUi.Form.ValidatorFunc || {
  /** Test if given string length is in range.
   * @param {string} String to test
   * @param {number} Minimum length
   * @param {number} Maximum length (optional)
   * @return {number} 0, if in range, 1 if too short, 2 if too large
   */
  length: function(data, min, max) {
    if (data.length < min) {
      return 1;
    }
    if (typeof max !== 'undefined' && data.length > max) {
      return 2;
    }
    return 0;
  },

  notEmpty: function(data) {
    if (typeof data === 'string') {
      return data.trim().length === 0 ? false : true;
    }
    return data.length === 0 ? false : true;
  },

  number: function(data) {
    return data.length > 0 ? !isNaN(data) : false;
  },

  /** Test if given number is in range.
   * @param {string} String to test
   * @param {number} Minimum value
   * @param {number} Maximum number (optional)
   * @param {boolean} If true, invert this range () default false
   * @return {number} 0, if in range, -1 if NaN, -2 if empty, 1 if too low,
   * 2 if too high, 3 if invert is used and value is in range
   */
  range: function(data, min, max, invert) {
    invert = invert || false;
    if (isNaN(data)) {
      return -1;
    }
    if (data.trim().length === 0) {
      return -2;
    }
    if (invert) {
      if (data > min && data < max) {
        return 3;
      }
    } else {
      if (data < min) {
        return 1;
      }
      if (typeof max !== 'undefined' && data > max) {
        return 2;
      }
    }
    return 0;
  },

  /** Test against a custom regular expression
   * @param {string} String to test
   * @param {regex} Regular expression
   * @param {boolean} If true, invert the matching (not matching is true)
   * @return {boolean} True if matched, false otherwise
   */
  regEx: function(data, regEx, invert) {
    invert = invert || false;
    return invert ? !regEx.test(data.trim()) : regEx.test(data.trim());
  },

  /** Check for same value on current element and input element referenced by id.
   * @param {string} String to test
   * @param {string} Id of element whose value should be tested against
   */
  same: function(data, elementId) {
    console.debug('SAME', data, elementId);
    return data.trim() == $(elementId).val().trim();
  },

  /** Check if the given string(s) look like URLs. This simply checks for spaces
   * in URLs, because URL validation is error prone.
   * @param {string, array} Strings to test
   */
  url: function(data) {
    data = YaCyUi.Tools.toArray(data);
    var regEx = /\s/;
    var valid = true;
    for (var i = 0; i < data.length; i++) {
      var dataItem = data[i].trim();
      var parts = dataItem.split('://');
      if (regEx.test(dataItem) || parts.length < 2 || parts[1].length === 0) {
        valid = false;
        break;
      }
    }
    return valid;
  },

  /** Check the given URLs for valid protocols.
   * @param {string, array} URLs to test
   * @param {string, array} Allowed protocols (e.g. ['https?', 'ftp'])
   */
  urlProtocol: function(data, protocols) {
    data = YaCyUi.Tools.toArray(data);
    protocols = YaCyUi.Tools.toArray(protocols);
    var regEx = new RegExp('^(' + protocols.join('|') + '):\/\/', 'i');
    var valid = true;

    for (var i = 0; i < data.length; i++) {
      if (!regEx.test(data[i].trim())) {
        valid = false;
        break;
      }
    }
    return valid;
  }
};
YaCyUi.Form.ValidatorElement = YaCyUi.Form.ValidatorElement ||
  function(element, setup, validator) {
    var self = this;
    this.element = element;
    this.validators = [];
    this.isValidated = false;
    this.isValid = false;
    this.isDisabled = false;
    this.validator = validator;

    function init(setup) {
      YaCyUi.Event.handle('toggle-section-elements', function(ev, state, elements) {
        var eId = self.element[0].id;
        for (var i = 0; i < elements.length; i++) {
          if (elements[i].id == eId) {
            if (state == 'disable') {
              self.isDisabled = true;
            } else {
              self.isDisabled = false;
            }
            self.validator.validate();
            break;
          }
        }
      });

      // set validators
      if ('validators' in setup) {
        self.addValidators(setup.validators);
      }
      // custom value getter function
      if ('get' in setup) {
        self.setGetter(setup.get);
      }
    }

    init(setup);
};
YaCyUi.Form.ValidatorElement.prototype = {
  private: {
    /** Parse results of simple validators. */
    parseResult: function(result, validator) {
      if (!result) {
        return {
          type: validator.failType || 'error',
          data: validator.error || true,
          stopExec: validator.stopExec || false
        };
      }
      return true;
    }
  },

  addValidators: function(validators) {
    for (var i = 0; i < validators.length; i++) {
      this.addValidator(validators[i]);
    }
  },

  addValidator: function(validator) {
    var self = this;
    switch (validator.type) {
      case 'length':
        this.validators.push(function(data) {
          var result = YaCyUi.Form.ValidatorFunc.length(
            data, validator.min, validator.max);
          if (result !== 0) {
            var state = {
              type: validator.failType || 'error',
              stopExec: validator.stopExec || false
            };
            if (result === 1 && 'tooShort' in validator) {
              state.data = validator.tooShort;
            } else if (result === 2 && 'tooLong' in validator) {
              state.data = validator.tooLong;
            } else if ('error' in validator) {
              state.data = validator.error;
            } else {
              state.data = true;
            }
            return state;
          }
          return true;
        });
        break;
      case 'notEmpty':
        this.validators.push(function(data) {
          return self.private.parseResult(
            YaCyUi.Form.ValidatorFunc.notEmpty(data), validator);
        });
        break;
      case 'number':
        this.validators.push(function(data) {
          return self.private.parseResult(
            YaCyUi.Form.ValidatorFunc.number(data), validator);
        });
        break;
      case 'same':
        this.validators.push(function(data) {
          return self.private.parseResult(
            YaCyUi.Form.ValidatorFunc.same(data, validator.element), validator);
        });
        break;
      case 'range':
        this.validators.push(function(data) {
          var result = YaCyUi.Form.ValidatorFunc.range(data, validator.min, validator.max,
            validator.invert);
          if (result !== 0) {
            var state = {
              type: validator.failType || 'error',
              stopExec: validator.stopExec || false
            };
            if (result === -1 && 'nan' in validator) {
              state.data = validator.nan;
            } else if (result === -2 && 'empty' in validator) {
              state.data = validator.empty;
            } else if (result === 1 && 'tooLow' in validator) {
              state.data = validator.tooLow;
            } else if (result === 2 && 'tooHigh' in validator) {
              state.data = validator.tooHigh;
            } else if ('error' in validator) {
              state.data = validator.error;
            } else {
              state.data = true;
            }
            return state;
          }
          return true;
        });
        break;
      case 'regEx':
        this.validators.push(function(data) {
          return self.private.parseResult(
            YaCyUi.Form.ValidatorFunc.regEx(data, validator.exp,
              validator.invert), validator);
        });
        break;
      case 'url':
        this.validators.push(function(data) {
          return self.private.parseResult(
            YaCyUi.Form.ValidatorFunc.url(data), validator);
        });
        break;
      case 'urlProtocol':
        this.validators.push(function(data) {
          return self.private.parseResult(
            YaCyUi.Form.ValidatorFunc.urlProtocol(data, validator.protocols),
            validator);
        });
        break;
      default:
        YaCyUi.error('YaCyUi.Form.ValidatorElement:addValidator',
          'Unknown validator: "' + validator.type + '"');
        break;
    }
  },

  getElement: function() {
    return this.element;
  },

  setGetter: function(getterFunc) {
    console.debug('ValidatorElement:setGetter', getterFunc, ' set for ', this.element);
    this.getterFunc = getterFunc;
  },

  validate: function() {
    if (typeof this.getterFunc !== 'undefined') {
      console.debug('ValidatorElement:validate ', this.element, ' with custom getter func!');
      this.getterFunc(this.element);
    }
    var data = this.element.val();
    if (this.element[0].tagName.toLowerCase() == 'textarea') {
      data = YaCyUi.Tools.cleanStringArray(data.split('\n'));
    }
    var result;
    var state = null;
    for (var i = 0; i < this.validators.length; i++) {
      result = this.validators[i](data);
      if (result !== true) {
        var isError = result.type == 'error' ? true : false;
        var stopExec = result.stopExec || false;
        state = {
          hints: {
            show: true,
            clear: true,
            help: isError
          },
          validation: {
            valid: !isError
          }
        };
        if (typeof result === 'object' && 'type' in result) {
          if ('data' in result) {
            state.hints[result.type] = result.data;
          } else {
            state.hints[result.type] = true;
          }
          if (isError) {
            break;
          }
        } else {
          state.hints.error = true;
          break;
        }
        if (stopExec) {
          break;
        }
      }
    }
    this.isValidated = true;
    if (state !== null) {
      this.isValid = state.validation.valid;
      this.validator.validate();
      return state;
    }
    this.isValid = true;
    this.validator.validate();
    return {
      hints: {
        ok: true,
        clear: true,
        help: false,
        show: true
      },
      validation: {
        valid: true
      }
    };
  }
};

YaCyUi.Form.ValidatorCount = YaCyUi.Form.ValidatorCount || 0;
YaCyUi.Form.Validator = YaCyUi.Form.Validator || function(config) {
  var self = this;
  this.elements = {};
  this.config = config || {};
  this.invalidElements = [];
  this.count = ++YaCyUi.Form.ValidatorCount;

  if (typeof config.toggle !== 'undefined') {
    config.toggle.prop('disabled', false);
  }
  if (typeof this.config.display !== 'undefined') {
    var msg = '<p><span id="ycu-errors-message-' + this.count +
      '"></span>';
    var linked = false;
    if (typeof this.config.showLink === 'undefined' ||
      (typeof this.config.showLink === 'boolean' && this.config.showLink)) {
      msg = msg + ' <a href="#" id="ycu-errors-show-' +
        this.count + '">Show errors.</a>';
      linked = true;
    }
    msg = msg + '</p>';
    this.config.display.append(msg);
    if (linked) {
      $('#ycu-errors-show-' + this.count).on('click', function(evObj) {
        evObj.preventDefault();
        self.showErrors();
      });
    }
  }

  return this;
};
YaCyUi.Form.Validator.prototype = {
  /** Add elements to the validator.
   * @param {jQuery} Elements
   * @param {object} Validator configuration
   * @return {object} Self reference
   */
  addElement: function(element, setup) {
    if (typeof element === 'undefined' || element.length === 0) {
      YaCyUi.error('Form.Validator:addElement:', 'element is undefined/empty!', element);
      return null;
    }
    var self = this;
    element.each(function() {
      element.uniqueId(); // we need an id attribute set

      if (typeof self.elements[this.id] === 'undefined') {
        self.elements[this.id] = new YaCyUi.Form.ValidatorElement(
          $(this), setup, self);

        var onLoad = false;
        if (typeof setup.onload === 'boolean') {
          onLoad = setup.onload;
        }
        if (typeof self.config.onload === 'boolean') {
          onLoad = self.config.onload;
        }

        YaCyUi.Form.Validate.addValidator($(this), {
          func: self.elements[this.id].validate,
          scope: self.elements[this.id],
          onload: onLoad
        });
      } else if ('validators' in setup) {
        self.elements[this.id].addValidators(setup.validators);
      }
    });
    return this;
  },

  /** Validate all known elements. also updates the display area. */
  validate: function() {
    var result;
    result = this.getState();
    if (typeof this.config.toggle !== 'undefined') {
      this.config.toggle.prop('disabled', result[1] > 0 ? true : false);
    }
    if (typeof this.config.display !== 'undefined') {
      if (result[1] > 0) {
        $('#ycu-errors-message-' + this.count).html(
          '<s class="sym sym-warning"><i></i><i></i></s>' +
          'There ' + (result[1] > 1 ? 'are ' : 'is ') +
          result[1] + ' error' + (result[1] > 1 ? 's' : '') +
          ' that need' + (result[1] === 1 ? 's' : '') +
          ' to be fixed until you may proceed.');
        this.config.display.show();
      } else {
        this.config.display.hide();
      }
    }
  },

  /** Check if the given element is valid.
   * @param {jQuery} Element to test
   * @return {boolean} True, if valid, false otherwise
   */
  isValid: function(element) {
    this.getState();
    return $.inArray(element, this.invalidElements) > -1 ? false : true;
  },

  /** Get the validation status of all known elements.
   * @return {array} number of all elements, number of invalid elements, number
   * of valid elements
   */
  getState: function() {
    var validCount = 0;
    var invalidCount = 0;
    this.invalidElements = [];
    var element;
    for (var e in this.elements) {
      element = this.elements[e];
      if (!element.isDisabled) {
        if (!element.isValidated) {
          element.validate();
        }
        if (element.isValid) {
          validCount++;
        } else {
          invalidCount++;
          this.invalidElements.push(element.getElement());
        }
      }
    }

    return [(validCount + invalidCount), invalidCount, validCount];
  },

  showErrors: function() {
    var position = null;
    var topMostId, offset;
    this.getState();
    for (var i = 0; i < this.invalidElements.length; i++) {
      var id = this.invalidElements[i][0].id;
      YaCyUi.Form.digOut(id);
      // jump to topmost element
      offset = this.invalidElements[i].offset();
      if (position === null || offset.top < position) {
        position = offset.top;
        topMostId = id;
      }
    }
    location.hash = '#' + topMostId;
  }
};