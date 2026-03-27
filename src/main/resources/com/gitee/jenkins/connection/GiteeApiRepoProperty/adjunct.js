Behaviour.specify("BUTTON.gitee-add", "gitee-add", 0, function(e) {
    e.onclick = function (evt) {
        let repo = document.getElementById("textBoxAreaRepo").value;
        let owner = document.getElementById("textBoxAreaOwner").value;

        let newOption = document.createElement("option");
        let text = repo + " " + owner;
        newOption.value = text;
        newOption.innerText = text;

        for (let elem of document.getElementsByClassName("selectElement")) {
            elem.appendChild(newOption);
        }
        property.addRepoOwner(repo, owner);
        evt.preventDefault();
    };
});