// --> npm install express --save
// --> npm install ejs --save

var express = require('express');
var app = express();
var bodyParser = require('body-parser');
var fs = require('fs');

app.set('view engine', 'ejs');
app.engine('html', require('ejs').renderFile);

////////////////////////////////////////////////////////////////////////////////////////////////////////

var app_uuid, inputData;
app.post("/post", (req, res) => {
   req.on("data", (data) => {
     inputData = JSON.parse(data);
     app_uuid= inputData.UUID;
     //console.log("UUID : "+inputData.UUID);
   });
});

app.get("/users", (req, res) => {
  res.render(__dirname+'/public/index.html',{          // to index.html
    'uuid' : app_uuid
  });
});

////////////////////////////////////////////////////////////////////////////////////////////////////////
app.listen(3000, function(){
  console.log("Express server has started on port 3000");
});
