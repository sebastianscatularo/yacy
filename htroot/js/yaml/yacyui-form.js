/*jslint browser:true */
/*global YaCyUi:true, $:true, console:true */
"use strict";

YaCyUi.Form = {
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

YaCyUi.Func.Form = {
  loaded: false,

  /** Initialize all form functions. */
  init: function(finishCallback) {
    var modules = {}; // gather modules that need initialization time

    // collapsible fieldsets
    var collapseableFieldsets = $('fieldset.collapsible');
    if (collapseableFieldsets.size() > 0) {
      modules.CollapseableFieldset =
        new YaCyUi.Func.Form.CollapseableFieldset(collapseableFieldsets, true);
    }

    // general objects
    modules.Validate = new YaCyUi.Func.Form.Validate();

    // form help
    var helpElements = $('fieldset.formSection > .formHelp');
    if (helpElements.size() > 0) {
      YaCyUi.Form.SectionHelp =
        new YaCyUi.Func.Form.SectionHelp(helpElements);
    }

    // form hints
    var hints = $('.formHint');
    if (hints.size() > 0) {
      modules.Hints = new YaCyUi.Func.Form.Hints(hints);
    }

    // toggleable form sections - run after hints, to getdynamic help items
    var toggleableFormSections = $('fieldset.toggleable');
    if (toggleableFormSections.size() >0){
      modules.ToggleableFormSection =
        new YaCyUi.Func.Form.ToggleableFormSection(toggleableFormSections);
    }

    // auto resizing of textareas
    var textareas = $('textarea');
    if (textareas.size() > 0) {
      YaCyUi.Form.ResizeableTextarea =
        new YaCyUi.Func.Form.ResizeableTextarea();
      YaCyUi.Form.ResizeableTextarea.autoResize(textareas);
    }

    // form elements (checkbox/radio) that enable/disable other elements
    var toggleableElements = $('input, select, textarea, button')
      .filter('[data-toggle-id]');
    if (toggleableElements.size() > 0) {
      modules.ToggleableFormElement =
        new YaCyUi.Func.Form.ToggleableFormElement(toggleableElements);
    }

    YaCyUi.Form.Data = new YaCyUi.Func.Form.Data();

    YaCyUi.Form.Button = new YaCyUi.Func.Form.Button();

    // init jQuery UI spinner elements
    $('.spinner').spinner();

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

      // destroy self
      delete YaCyUi.Func.Form;
    }, 125);
  }
};

/** Make form fieldsets collapseable.
  * @param {jQuery} Fieldset elements
  * @param {boolean} If true fieldsets are initially collapsed */
YaCyUi.Func.Form.CollapseableFieldset = function(fieldsets, initialHidden) {
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
      .show('slow');
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
      .hide('slow');
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
    fieldset.hasClass('collapsed') ?  this.show(fieldset) : this.hide(fieldset);
  }
};

/** Toggle form sections by enabling/disableing.
  * @param {jQuery} form section elements (<dl/>) */
YaCyUi.Func.Form.ToggleableFormSection = function(formSections) {
  var self = this;
  this.loaded = false;
  var sectionsToHideCount = 0;
  var text = {
    configure: 'configure',
    activateAndConfigure: 'activate &amp; configure',
    revert: 'use defaults',
    turnOff: 'deactivate'
  }

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
    sectionsToHideCount = formSections.size();
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
        toggles.each(function(){
        $(this).hasClass('on') ? $(this).hide() : $(this).show();
      });
    content.show('slow', function() {
      $(this).removeClass('hidden');
      YaCyUi.DataStore.set(content.parent(), {
        space: 'toggle',
        data: {
          state: 'visible'
        }
      });
      var header = formSection.children('legend');
      // show tooltip, if any
      if(typeof header.data('uiTooltip') !== 'undefined'){
        header.tooltip("option", "disabled", false);
      }
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

    content.hide(speed, function() {
      toggles.each(function(){
        $(this).hasClass('on') ? $(this).show() : $(this).hide();
      });
      formSection
        .addClass('ycu-toggle-hidden')
        .removeClass('ycu-toggle-visible');

      YaCyUi.DataStore.set(content.parent(), {
        space: 'toggle',
        data: {
          state: 'hidden'
        }
      });
      var header = formSection.children('legend');
      // hide tooltip, if any
      if(typeof header.data('uiTooltip') !== 'undefined'){
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
YaCyUi.Func.Form.SectionHelp = function(formHelpElements) {
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
YaCyUi.Func.Form.Hints = function(hintElements) {
  var self = this;
  this.loaded = false;

  function init() {
    YaCyUi.Event.handle('data-change', function(ev, space, formElement, data) {
      if (space === 'hints') {
        self.private.handleDataEvent(self, formElement, data);
      }
    });

    hintElements.each(function() {
      var prev = $(this).prev('input[type="text"], textarea');
      if (prev.size() > 0) {
        prev.on('focus', function() {
          YaCyUi.DataStore.set(prev, {
            space: 'hints',
            data: {
              help: 'auto',
              show: true
            }
          });
        });
        prev.on('blur', function() {
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
      switch(data.show) {
        case true:
          YaCyUi.Form.Hints.show(formElement);
          break;
        case false:
          YaCyUi.Form.Hints.hide(formElement, {triggerEvent: false});
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
      var hint = formElement.next('.formHint');
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
        if (e[name].size() > 0 && name in data) {
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
      if (e.help.size() > 0 && 'help' in data) {
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
      }).stop().hide(hideDelay).promise().done(function() {
        $.each(show, function(i, e) {
          var jObj = $(e[0]);
          e[1] === true ? jObj.show() : jObj.show(350);
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
        $(this).next('.formHint').show('slow');
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
      $(this).next('.formHint').hide('slow');
    });
  }
};

YaCyUi.Func.Form.ResizeableTextarea = function() {};
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

      while(oHeight < cHeight) {
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

YaCyUi.Func.Form.Validate = function() {
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
      var elementData = {
        space: 'validation',
        data: {
          valid: true
        }
      };
      formElements.each(function() {
        $(this).removeClass('invalid').addClass('valid');
        if (setData) {
          YaCyUi.DataStore.set($(this), elementData);
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
      var elementData = {
        space: 'validation',
        data: {
          valid: false
        }
      };
      formElements.each(function() {
        $(this).removeClass('valid').addClass('invalid');
        if (setData) {
          YaCyUi.DataStore.set($(this), elementData);
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
      var elementData = {
        space: 'validation',
        data: {
          valid: null
        }
      };
      formElements.each(function() {
        $(this).removeClass('valid invalid');
        if (setData) {
          YaCyUi.DataStore.set($(this), elementData);
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
        switch(states.validation.valid) {
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
        for (var i=0; i<this.elements.length; i++) {
          this.elements[i].uniqueId();
          var id = this.elements[i].attr('id');
          YaCyUi.Form.digOut(id);
          // jump to topmost element
          offset = this.elements[i].offset()
          if (position === null || offset.top < position) {
            position = offset.top;
            location.hash = '#' + id;
          }
        }
      }
    }
  },

  Validators: {
    private: {
      okState: {
        hints: {
          ok: true,
          clear: true,
          help: false,
          show: true
        },
        validation: {
          valid: true
        }
      },

      setFailState: function(stateObj) {
        stateObj.hints.show = true;
        stateObj.hints.clear = true;
        stateObj.validation = {
          valid: false
        };
      }
    },

    /** Check if the text input is non empty.
      * @param {object} Hints to set
      * @param {boolean} Validation state. (default: false) */
    notEmpty: function(hints, valid) {
      this.validate = function(jObj) {
        var state = {
          hints: hints
        };
        jObj.val().trim().length === 0 ?
          YaCyUi.Form.Validate.Validators.private.setFailState(state)
          : state = YaCyUi.Form.Validate.Validators.private.okState;
        if (typeof valid === 'boolean') {
          state.validation.valid = valid;
        }
        if (!state.validation.valid) {
          YaCyUi.Form.digOut(jObj.attr('id'));
        }
        return state;
      };
    },

    /** Check if the text input contains a valid URL. */
    url: function() {
      this.validate = function(jObj, evObj) {
        var content = jObj.val().trim();
        var state; // final elements state to return

        if (content.length === 0) {
          // empty
          state = {
            hints: {
              help: true,
              show: true,
              clear: true,
              error: 'empty'
            },
            validation: {
              valid: false
            }
          };
        } else if (YaCyUi.Tools.Validation.isCrawlerUrl(content)) {
          // correct entry
          state = {
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
        } else {
          // single entry, not a url
          state = {
            hints: {
              error: 'invalid',
              help: true,
              show: true,
              clear: true
            },
            validation: {
              valid: false
            }
          };
        }

        if (!jObj.is(':focus')) {
          state.hints.show = false;
        }

        return state;
      };
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
    *   onload {boolean} Immediatly validate (default: false)*/
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
            var state = param.func(e, evObj);
            if (typeof state === 'object' && state !== null) {
              self.private.setState.call(self, e, state);
            }
          }, param.delay);
          e.data('validationTimeout', timeout);
        }
      });

      if (param.onload === true) {
        var state = param.func(e, null);
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
    for (var i=0; i<elements.length; i++) {
      var element = elements[i];
      if (!YaCyUi.Form.Validate.isValid(element)) {
        var id = element.attr('id');
        var label = $('label[for="' + id + '"]');
        this.elements.push(element);
        if (label.size() > 0) {
          labels.push(label.text());
        } else {
          moreErrors++;
        }
      }
    }
    if (labels.length >  0) {
      var labelList = $('<ul/>');
      this.dialog.append('<div>The following elements contain invalid values:</div><br/>');
      for (var i=0; i<labels.length; i++) {
        labelList.append('<li>' + labels[i] + '</li>');
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

YaCyUi.Func.Form.ToggleableFormElement = function(toggleableElements) {
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

      if (toggleElement.size() <= 0 || (toggleElement.attr('type') != 'radio' &&
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
          toggleId == this.id ? toggleOn(toggles[toggleId]) :
            toggleOff(toggles[toggleId]);
        }
      });
    });

    self.loaded = true;
  }

  init();
};

YaCyUi.Func.Form.Data = function() {
  function getFormData(form) {
    var data = {};

    // find all input elements
    form.find('input:not([type="button"])').each(function() {
      var e = $(this);
      switch(this.type.toLowerCase()) {
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
  };

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
    console.debug("sbmit URL", url);
  };
};

YaCyUi.Func.Form.Button = function() {};
YaCyUi.Func.Form.Button.prototype = {
  /** Switch between button icons.
    * @param {jQuery} The button element
    * @param {string} New icon class to set. If omitted the old icon will be set
    * again. */
  switchIcon: function(element, toName) {
    var classes = element.attr('class').split(' ');
    var iconName = '';
    for (var i=0; i<classes.length; i++) {
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