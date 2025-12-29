Behaviour.specify("BUTTON.gitee-generate", "gitee-generate", 0, function (e) {
    e.onclick = function (evt) {
        document.getElementById('giteeSecretToken').value = [...Array(32)].map(() => Math.floor(Math.random() * 16).toString(16)).join('');
        evt.preventDefault();
    };
});

Behaviour.specify("BUTTON.gitee-clear", "gitee-clear", 0, function (e) {
    e.onclick = function (evt) {
        document.getElementById('giteeSecretToken').value = "";
        evt.preventDefault();
    };
});
