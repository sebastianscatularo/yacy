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

  toArray: function(data) {
    if (typeof data === 'string') {
      return [data];
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
  getUrlParameters: function() {
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    var parameters = {};

    for (var i = 0; i < vars.length; i++) {
      var pair = vars[i].split("=");
      parameters[pair[0]] = pair[1];
    }
    return parameters;
  }
};