/*jslint browser:true */
/*global YaCyUi:true, $:true, console:true */

YaCyUi.Func.MainMenu = function() {
  var self = this;
  this.e = {
    subContent: $('#navSub').children('.wrap').children('.content')
  };
  this.subMenuToggles;
  this.levelsShown = 0;
  this.toggleUp = $('<i class="fa fa-caret-up"></i>').hide();
  this.toggleDown = $('<i class="fa fa-caret-down"></i>');

  /** Attach click handler to toplevel entries and menu toggle. */

  function addInteractionHandler() {
    var pattern = /#$/;
    self.subMenuToggles = $('#nav').find('li > span');

    self.private.attachSubMenuHandler.call(self,
      $('#nav ul.main > li > span'), 1, true);

    self.toggleUp.on('click', function() {
      self.hideSubContent.call(self, true);
    });

    self.toggleDown.on('click', function() {
      self.showSubContent.call(self, true);
    });

    $('#menuToggle').append(self.toggleUp).append(self.toggleDown);
  }

  /** Create the breadcrumb path and mark active menu entries. */

  function createBreadCrumbAndMarkActive() {
    var current = null;
    var urlExp = '.*' + location.pathname + '.*';
    var candidates = [];

    $('#nav .main a[href!="#"]').each(function() {
      if (this.href.match(urlExp)) {
        if (location.search.length === 0) {
          // plain page
          current = $(this);
          return false; // break loop
        } else {
          // with query string
          candidates.push($(this));
        }
      }
    });

    if (current === null) {
      if (candidates.length === 1) {
        current = candidates[0];
      } else {
        var currentMatches = 0;
        var matchesCount = 0;
        var urlQuery = location.search.substr(1).split('&');
        for (var i = 0; i < candidates.length; i++) {
          var candidate = candidates[i];
          currentMatches = 0;
          for (var j = 0; j < urlQuery.length; j++) {
            var queryItem = urlQuery[j];
            if (candidate.attr('href').match('.*' + queryItem + '.*')) {
              currentMatches++;
            }
            if (currentMatches > matchesCount) {
              current = candidate;
            }
          }
        }
      }
    }

    if (current === null) { // nothing found
      return;
    }

    var chain = [];
    var parentEl = current.parent();
    var currentTagName;

    // reverse tree traversal
    while (parentEl.length > 0) {
      currentTagName = parentEl[0].tagName.toLowerCase();
      if (currentTagName === 'nav') {
        // hit the upper bound
        break;
      } else if (currentTagName === 'li') {
        chain.push(parentEl);
        parentEl.addClass('active');
      }
      parentEl = parentEl.parent();
    }
    chain.reverse();

    var breadCrumb = $('<div>');
    var level = 0;
    for (var i = 0; i < chain.length; i++) {
      var e = chain[i];
      var text = e.children('span, a').text().trim();
      if (text.length > 0) {
        if (i === 0) {
          e.addClass('active'); // root entry
          self.showSubMenu(e, (i + 1), true);
        } else {
          self.showSubMenu(chain[i], (i + 1), true);
          e.addClass('active');
        }
        breadCrumb.append('<span>' + text + '</span>');
      }
    }
    $('#breadCrumb').html(breadCrumb);
  }

  addInteractionHandler();
  createBreadCrumbAndMarkActive();
  $('#menuToggle').show();
};
YaCyUi.Func.MainMenu.prototype = {
  private: {
    attachSubMenuHandler: function(entries, level, clear) {
      var self = this;
      clear = typeof clear !== 'boolean' ? false : clear;

      entries.on('click', function(evObj) {
        evObj.preventDefault();

        if ($(this).parent('li').hasClass('active')) {
          if ($('#navSub').is(':visible') && level == 1) {
            self.hideSubContent();
          } else {
            self.showSubContent();
          }
          return;
        }

        if (clear) {
          self.private.clearSubContent.call(self);
        }
        self.showSubMenu.call(self, $(this).parent('li'), level);
        self.showSubContent();
      });

      if (level > 1) {
        entries.each(function() {
          $(this).parent('li').addClass('hasSub');
        });
      }
    },

    clearSubContent: function() {
      this.levelsShown = 0;
      this.e.subContent.empty();
      this.subMenuToggles.each(function() {
        $(this).parent('li').removeClass('active');
      });
    },
  },

  hideSubContent: function(animate) {
    var self = this;
    animate = typeof animate === 'boolean' ? animate : false;

    if (animate) {
      $('#navSub').slideUp('slow', function() {
        self.toggleDown.show();
        self.toggleUp.hide();
      });
    } else {
      self.toggleDown.show();
      self.toggleUp.hide();
      $('#navSub').hide();
    }
  },

  showSubContent: function(animate) {
    var self = this;
    animate = typeof animate === 'boolean' ? animate : false;

    if (animate) {
      $('#navSub').slideDown('slow', function() {
        self.toggleDown.hide();
        self.toggleUp.show();
      });
    } else {
      self.toggleDown.hide();
      self.toggleUp.show();
      $('#navSub').show();
    }
  },

  hideSubMenu: function() {
    $('#navSub').hide();
  },

  showSubMenu: function(entry, level, keepActive) {
    // clone menu content
    var content = entry.children('ul').clone().show();
    // calculate if we should hide something
    var levelsToHide = this.levelsShown - level + 1;
    keepActive = typeof keepActive !== 'boolean' ? false : keepActive;

    if (content.length === 0) {
      return;
    }

    if (this.levelsShown == level) {
      // remove last level
      this.e.subContent.children('.menuLevel-' + level).remove();
      this.levelsShown--;
    } else if (this.levelsShown > level) {
      // remove multiple levels
      for (var i = 0; i < levelsToHide; i++) {
        this.e.subContent.children('.menuLevel-' + (this.levelsShown - i)).remove();
      }
      this.levelsShown -= levelsToHide;
    }

    if (!keepActive && level - 1 > 0) {
      // unset previous active element
      this.e.subContent.children('.menuLevel-' + (level - 1)).find('.active')
        .removeClass('active');
      content.find('.active').removeClass('active');
    }

    entry.addClass('active');
    this.levelsShown++;

    // attach handler to new entries
    this.private.attachSubMenuHandler.call(this,
      content.find('li > span'), level + 1);

    // allow proper css styling
    content.find('a').parent('li').addClass('hasLink');

    // create content..
    content = $('<div class="menuLevel-' + level + '"></div>').html(content);
    // ..and append it
    this.e.subContent.append(content);
    // set color class
    $('#navSub').removeClass(function(index, css) {
      return (css.match(/\blevel-\S+/g) || []).join(' ');
    }).addClass('level-' + level);
  }
};