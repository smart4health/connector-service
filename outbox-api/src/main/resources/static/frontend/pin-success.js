document.addEventListener("DOMContentLoaded", function() {
  var div = document.getElementById("success-redirect-url");
  window.top.location.href = div.getAttribute("data-redirect");
});