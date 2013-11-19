YaCyPage.EventHandler = function() {
  this.handleArrowKeys = function(evObj) {
    switch (evObj.keyCode) {
      case 9:
      case 33:
        window.location.href = document.getElementById("nextpage").href;
        break;
      case 34:
        window.location.href = document.getElementById("prevpage").href;
        break;
    }
  };
};

/** Initialize the page. */
YaCyPage.init = function() {
  YaCyPage.eventHandler = new YaCyPage.EventHandler();

  // handle navigational keys
  $(document).on('keydown', YaCyPage.eventHandler.handleArrowKeys);
};