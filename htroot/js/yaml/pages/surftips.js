/*jslint browser:true */
/*global YaCyPage:true, YaCyUi:true, $:true, jQuery:true, console:true */
"use strict";

/** Initialize the page. */
YaCyPage.init = function() {
  $('#searchResults a.vote').on('click', function(evObj) {
    evObj.preventDefault();

    var queryParams = YaCyUi.Tools.getUrlParameters(this.href);
    var baseUrl = this.href.split('?')[0];

    if ($(this).data('id') == 'positiveVote') {
      queryParams.comment = window.prompt("Please enter a comment to your link recommendation. (Your Vote is also considered without a comment.)", "");
    }

    $.ajax({
      type: 'GET',
      url: baseUrl,
      data: queryParams
    });

    var voteElements = $(this).parent().find('a.vote');
    voteElements.off();
    voteElements.fadeOut('slow', function() {
      voteElements.remove();
    });
  });
};