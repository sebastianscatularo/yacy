var YaCyUi = YaCyUi || {}; // UI namespace
YaCyUi.Widgets = YaCyUi.Widgets || {}; // UI namespace

YaCyUi.Widgets.ProgressBar = function(element, maxValue) {
  var self = this;
  console.debug("progress is", element);

  this.step = function(amount) {
    element.progressbar('value', element.progressbar('value') + amount);
    console.debug("progress set", element.progressbar('value'));
  }

  this.show = function() {
    element.show();
  }

  this.hide = function() {
    console.debug("progress hide", element.progressbar('value'));
    element.hide('slow');
  }

  element.progressbar({
    max: maxValue,
    complete: function() {
      self.hide();
    }
  });
  console.debug("progress init");
};