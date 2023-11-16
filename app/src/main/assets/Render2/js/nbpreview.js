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

    root.largeLog = function(content, chunkSize = 4000) {
        for (let i = 0; i < content.length; i += chunkSize) {
            const chunk = content.substring(i, Math.min(i + chunkSize, content.length));
            console.log(chunk);
            }
        }


    let dataChunks = [];

    root.addDataChunk = function(chunk){
        dataChunks.push(chunk);
    }

    root.processData = function () {
            let fullData = dataChunks.join('');
            //largeLog(fullData); // Example: logging the full data
            dataChunks = []; // Clear the chunks array
            var parsed = JSON.parse(fullData);
            render_notebook(parsed);
            notebook.style.display = "block";
            $holder.style.fontSize = ".5em";
        }

}).call(this);
