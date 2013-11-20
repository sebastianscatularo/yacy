var YaCyUi = YaCyUi || {}; // UI namespace
YaCyUi.Widgets = YaCyUi.Widgets || {}; // UI namespace

YaCyUi.Widgets.ProgressBar = function(element, maxValue) {
  var self = this;

  this.step = function(amount) {
    element.progressbar('value', element.progressbar('value') + amount);
  }

  this.show = function() {
    element.show();
  }

  this.hide = function() {
    element.hide('slow');
  }

  element.progressbar({
    max: maxValue,
    complete: function() {
      self.hide();
    }
  });
};