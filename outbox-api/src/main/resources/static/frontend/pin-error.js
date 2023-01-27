document.addEventListener("DOMContentLoaded", function() {
  var div = document.getElementById("redirect-url");
  window.top.location.href = div.getAttribute("data-redirect");
});