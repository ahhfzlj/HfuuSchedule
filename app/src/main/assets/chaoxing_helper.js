/**
 * 超星学习通登录态检测辅助脚本
 */
(function () {
  var result = {
    url: window.location.href,
    title: document.title || '',
    hasUserInfo: false,
    userName: ''
  };
  var nameEl = document.querySelector('.user-name, .name, #userName, .personalName');
  if (nameEl && nameEl.innerText) {
    result.userName = nameEl.innerText.trim();
    result.hasUserInfo = true;
  }
  return JSON.stringify(result);
})();
