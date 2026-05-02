Behaviour.specify("BUTTON.gitee-add", "gitee-add", 0, function(e) {
    e.onclick = function (evt) {
        let repo = document.getElementById("textBoxAreaRepo").value;
        let owner = document.getElementById("textBoxAreaOwner").value;
        let regex = /[\\\\\/\s]+/;
        
        if (repo && owner && !regex.test(repo) && !regex.test(owner)) {
            let newOption = document.createElement("option");
            let text = repo + " " + owner;
            newOption.value = text;
            newOption.innerText = text;

            for (let elem of document.getElementsByClassName("selectElement")) {
                elem.appendChild(newOption.cloneNode(true));
            }
            property.addRepoOwner(repo, owner);
        }
        evt.preventDefault();
    };
});

Behaviour.specify("BUTTON.gitee-remove", "gitee-remove", 0, function(e) {
    e.onclick = function (evt) {
        let selected = document.getElementById("selected").value;
        property.removeRepoOwner(selected, function (func) {
            let isRemoved = func.responseObject();
            if (isRemoved) {
                for (let elem of document.getElementsByClassName("selectElement")) {
                    elem.childNodes.forEach(function (currentValue) {
                        if (currentValue.value === selected) {
                            elem.removeChild(currentValue);
                        }
                    });
                }
            }
        });
        evt.preventDefault();
    };
});

Behaviour.specify("BUTTON.gitee-remove-all", "gitee-remove-all", 0, function(e) {
    e.onclick = function (evt) {
        property.removeAllRepoOwners(function (func) {
            let isCleared = func.responseObject();
            if (isCleared) {
                for (let elem of document.getElementsByClassName("selectElement")) {
                    while (elem.lastChild) {
                        elem.removeChild(elem.lastChild);
                    }
                }
            }
        });
        evt.preventDefault();
    };
});
