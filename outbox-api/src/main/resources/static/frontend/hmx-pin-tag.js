(function() {
  var params = parseQuery(queryString());

  if (!params.token) {
    throw new Error("Iframe cannot load without an encrypted token");
  }

  loadIframe(params.token);

  /**
   * 
   * @param {String} token 
   * @return {Void}
   */
  function loadIframe(token) {
    var iframe = document.getElementById("hmx-pin");
    if (!iframe) {
      throw new Error("HTML Element with id 'hmx-pin' must be present on page")
    }
  
    iframe.src = "/frontend/send-sms?token=" + token;
  }

  /**
   * @return {String}
   */
  function queryString() {
    var query = window.location.search.substring(1);
    if (!query) {
        throw new Error("Iframe cannot load without an encrypted token");
    }
    return query;
  }

  /**
   * 
   * @param {String} query
   * @return {Object} { token: String }
   */
  function parseQuery(query) {
    return query.split('&').reduce(function(acc, curr) {
      var queryTuple = curr.split('=');
      acc[queryTuple[0]] = queryTuple[1];
      return acc;
    }, {});
  }
})();