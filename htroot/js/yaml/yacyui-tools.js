/*jslint browser:true */
/*global YaCyUi:true, $:true, console:true */

YaCyUi.Tools = {
  /** Remove empty and faulty elements from a given array of strings.
    * @param {Array} Array to clean up
    * @return {Array} Cleaned up array */
  cleanStringArray: function(arr) {
    var cleanArr = [];
    for(var i = 0; i<arr.length; i++){
      if (arr[i] && arr[i].trim().length > 0){
        cleanArr.push(arr[i]);
      }
    }
    return cleanArr;
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

  isTextChangingKeyEvent: function(evObj) {
    // filter out non printable keys on key events
    if (evObj.type == 'keyup' || evObj.type == 'keydown' ||
        evObj.type == 'keypress') {

      var code = evObj.which;
      if (typeof code == "number" && code > 0) {
        if (evObj.ctrlKey || evObj.metaKey || evObj.altKey) {
          return false;
        }
        var skipCodes = [9,13,16,18,20,27,33,34,35,36,37,38,39,40,45,111,112,113,114,115,116,117,118,119,120,121,122,123,144];
        if (skipCodes.indexOf(code) > -1) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
};

YaCyUi.Tools.Validation = {
  /** Test if given URL(s) are valid for the crawler. The test is very basic, as
    * it only checks the protocol and some disallowed characters.
    * @param {String or Array} URL(s) to test
    * @return {boolean} False if any URL does not match */
  isCrawlerUrl: function(obj) {
    var urlArr;
    var pattern = /^(ftp:|file:|https?:|smb:)\/\/[^ "]+$/i;

    if(typeof obj === 'string' ) {
      if (obj.trim().length === 0) {
        console.debug("zero -> false");
        return false;
      }
      urlArr = [obj];
    } else {
      urlArr = obj;
    }

    for (var i = 0; i < urlArr.length; i++) {
      if (pattern.test(urlArr[i]) === false) {
        return false;
      }
    }
    return true;
  },

  simpleUrlValidator: function(attachTo, eventHandler) {
    YaCyUi.Event.handle('validation-state', function(evObj, type, elements) {
      eventHandler(evObj, type, elements);
    });

    YaCyUi.Form.Validate.addValidator(attachTo, {
      func: new YaCyUi.Form.Validate.Validators.url().validate,
      delay: YaCyPage.validationDelay,
      onload: true
    });
  }
};