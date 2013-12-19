/*jslint browser:true */
/*global YaCyUi:true, $:true, console:true */
YaCyUi.Tools = YaCyUi.Tools || {
  /** Remove empty and faulty elements from a given array of strings.
   * @param {Array} Array to clean up
   * @return {Array} Cleaned up array */
  cleanStringArray: function(arr) {
    var cleanArr = [];
    for (var i = 0; i < arr.length; i++) {
      if (arr[i] && arr[i].trim().length > 0) {
        cleanArr.push(arr[i]);
      }
    }
    return cleanArr;
  },

  /** Convert the given strin to an array.
   * @param {String} The string to convert
   * @param {RegExp} Expression to split the string, or null if no splitting
   * is desired. (default windows & linux line endings)
   * @return {String} The given string as array
   */
  toArray: function(data, splitAt) {
    splitAt = typeof splitAt === 'undefined' ? /[^\r\n]+/g : splitAt;
    console.debug('toArray', data, splitAt);
    if (typeof data === 'string') {
      if (data.trim().length === 0) {
        return [];
      }
      if (splitAt === null) {
        return [data];
      } else {
        console.debug('toArray', data, splitAt, data.match(splitAt) || data);
        return data.match(splitAt) || data;
      }
    }
    return data;
  },

  checkCrawlUrl: function(urlString, callback, scope) {
    $.ajax({
      url: '/api/getpageinfo.json?actions=title,robots&url=' + urlString
    }).done(function(data) {
      if (typeof callback === 'function') {
        if (typeof scope === 'undefined') {
          callback(data, urlString);
        } else {
          callback.call(scope, data, urlString);
        }
      }
    });
  },

  /** Check/Uncheck all checkboxes in the given elements.
   * @param {jQuery} The elements that contain checkboxes
   * @param {boolean} True for checked, false for unchecked state.
   * (default: true) */
  checkAllBoxes: function(elements, state) {
    state = typeof state === 'boolean' ? state : true;
    elements.find('input[type="checkbox"]').prop('checked', state);
  },

  /** Get the URL parametera of the current URL.
   * @return {Object} Key, value pairs of the URL parameters.
   */
  getUrlParameters: function(url) {
    var query;
    if (typeof url !== 'undefined') {
      query = url.substring(url.indexOf("?") + 1);
    } else {
      query = window.location.search.substring(1);
    }
    var vars = query.split("&");
    var parameters = {};

    for (var i = 0; i < vars.length; i++) {
      var pair = vars[i].split("=");
      parameters[pair[0]] = pair[1];
    }
    return parameters;
  },

  /** Open a new window.
   * @param {string} url to open
   * @param {string} title of new window
   * @param {object} configuration for new window, use default, if undefined
   */
  openWindow: function(url, title, conf) {
    conf = conf || {
      toolbar: 0,
      scrollbars: 1,
      location: 0,
      status: 0,
      menubar: 0,
      resizable: 1,
      width: 640,
      height: 480
    };
    var confStr = '';
    for (var key in conf) {
      confStr += key + '=' + conf[key] + ',';
    }
    confStr = confStr.replace(/,$/, '');
    window.open(url, title, confStr);
  }
};

/**
 * @prama {object} configuration:
 * width {number}: width of the image
 * height {number}: height of the image
 * imageStub {string}: base file of the image
 * element {jQuery}: html element to operate on
 */
YaCyUi.Tools.PictureAnimation = function(conf) {
  var imageArray = [];
  var imageAnimIndex = 0;
  var imageCycles = 0;
  var imageLoadIndex = 0;
  conf.container = conf.element.parent();

  function init() {
    var handle = new Date().getTime();
    for (var i = 0; i < 6; i++) {
      conf.element.before(conf.element.clone().removeAttr('id').attr('data-index', i));
      imageArray.push(conf.container.find('img[data-index="' + i + '"]').first());
      initPhase(i, handle);
    }
    conf.element.remove();
    doAnimation();
  }

  function initPhase(phase, handle) {
    var angle = phase * 60;
    imageArray[phase][0].src = conf.imageStub + angle + "&amp;handle=" + handle;
  }

  function doAnimation() {
    for (var i = 0; i < imageArray.length; i++) {
      if (i == imageAnimIndex) {
        imageArray[i].show();
      } else {
        imageArray[i].hide();
      }
    }
    imageAnimIndex++;
    if (imageAnimIndex == 6) {
      imageAnimIndex = 0;
    }
    imageCycles++;
    if (imageCycles == 25) {
      initPhase(imageLoadIndex, new Date().getTime());
      imageLoadIndex++;
      if (imageLoadIndex == 6) {
        imageLoadIndex = 0;
      }
      imageCycles = 0;
    }
    setTimeout(function() {
      doAnimation()
    }, 100);
  };

  init();
};