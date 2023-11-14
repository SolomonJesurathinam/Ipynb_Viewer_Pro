(function () {
    var root = this;
    var $holder = document.querySelector("#notebook-holder");
    var notebook = document.getElementById("main");

    var render_notebook = function (ipynb) {
        var notebook = root.notebook = nb.parse(ipynb);
        while ($holder.hasChildNodes()) {
            $holder.removeChild($holder.lastChild);
        }
        $holder.appendChild(notebook.render());
        Prism.highlightAll();
    };

    root.processData = function (encodedData) {
            console.log(encodedData)
            var data = atob(encodedData); // Decode Base64 string
            console.log(data)
            var parsed = JSON.parse(data);
            render_notebook(parsed);
            notebook.style.display = "block";
            $holder.style.fontSize = ".5em";
        }

}).call(this);
