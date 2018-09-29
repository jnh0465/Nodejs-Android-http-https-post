# Nodejs-Android-http-https-post
___
##최종목표
-
`원래목표 :     
` 
<br>
- [x] post로 nodejs 서버에서 데이터 받아오기
- [x] https 연결 구현 **_180922**
 
`애로사항 : `   
~~~
http는 여러 의미서 통신이 간편한데 https로 데이터를 옮기는게 너무 힘들었다ㅠㅠ 
처음보는 SSL에 인증서에.. 결국은 인증서가 필요치 않은 방법을 찾아 적용했지만, 관련자료가 너무 부족해 진행하기가 어려웠다
get으로 서버에서 안드로이드로 보내는 방식은 수월했지만 post로 안드로이드에서 서버로 데이터를 옮기는 과정이 어려웠다...

+) 
어플 처음 설치시 안드로이드 내에서 uuid가 생성되고 계속 uuid값을 가지고 있는다. mainactivity로 진입할 시
toast로 uuid를 확인하고(확인용) http또는 https 서버로 uuid값을 보내주어 통신되는지 확인하였다.
~~~
___
#180914-16 uuid 생성 후 SharedPreferences에 저장
-
`어플 최초 실행시 UUIDTest()로 uuid생성한 후 SharedPreferences에 값 저장`
~~~java
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener { // 네비게이션 드로어
    public SharedPreferences ISFIRSTPre;                   //처음실행여부 저장할 SharedPreferences 선언
    private SharedPreferences.Editor ISFIRSTPreEdit;

    private SharedPreferences UUIDPre;                     //UUID값 저장할 SharedPreferences 선언
    private SharedPreferences.Editor UUIDPreEdit;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ISFIRSTPre = getSharedPreferences("isFirst2", MainActivity.MODE_PRIVATE); //최초 실행 여부 확인 위해 getSharedPreferences 사용
        ISFIRSTPreEdit = ISFIRSTPre.edit();

        UUIDPre = getSharedPreferences("setting", 0);                                   //UUID값 저장 위해 getSharedPreferences 사용
        UUIDPreEdit = UUIDPre.edit();                                                   //0 == SharedPreferences 읽기, 쓰기 가능

        boolean first = ISFIRSTPre.getBoolean("isFirst2", false);
        if(first==false){                                                              //최초 실행시
            ISFIRSTPreEdit.putBoolean("isFirst2",true);
            UUIDPreEdit.putString("UUIDValue", UUIDTest());                            //UUID값
            UUIDPreEdit.commit();                                                      //저장
            ISFIRSTPreEdit.commit();
        }
        
        String result = UUIDPre.getString("UUIDValue", "0");            //getSharedPreferences에서 불러옴
        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();      //TOAST로 표시, 확인용
        new JSONTask().execute("http://YOURSERVER:3000/post");                         //#########3uuid를 렉스로 보낼 수단
~~~
`uuid 생성하는 UUIDTest()`
~~~java
    public static String UUIDTest() {                                                   //UUID 생성
        UUID one = UUID.randomUUID();
        System.out.println("UUID One: " + one.toString());
        return one.toString();
    }
~~~
`Android mainActivity에서 uuid toast로 띄우기`      
<img src="https://user-images.githubusercontent.com/38582562/46141038-c8369e00-c28d-11e8-9d4c-55ccde5424ad.jpg" width="40%"> 
___
#180918-21 http/https 서버로 post
-
`http/https 서버로 보내는 코드`
~~~java
 public class JSONTask extends AsyncTask<String, String, String> {                  //uuid를 서버로 던져주는 코드
        @Override
        protected String doInBackground(String... urls) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("UUID",  UUIDPre.getString("UUIDValue", "0"));  //UUIDPre라는 getSharedPreferences에 있는 UUIDValue을 가져옴 / 디폴트값은 0임

                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });

                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, new X509TrustManager[]{ new X509TrustManager(){
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }}, new SecureRandom());

                    HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                    URL url = new URL("http://YOURSERVER:3000/post"); //https://YOURSERVER/post

                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Cache-Control", "no-cache");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setRequestProperty("Accept", "text/html");
                    con.setDoOutput(true);
                    con.setDoInput(true);
                    con.connect();

                    OutputStream outStream = con.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));
                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();

                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while((line = reader.readLine()) != null){
                        buffer.append(line);
                    }
                    return buffer.toString();
                } catch (MalformedURLException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(reader != null){
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }
~~~
`nodejs에서 받는 코드(app.js)`
~~~node.js
var app_uuid, inputData;
app.post("/post", (req, res) => {
   req.on("data", (data) => {
     inputData = JSON.parse(data);
     app_uuid= inputData.UUID;
     //console.log("UUID : "+inputData.UUID);
   });
});

app.get("/users", (req, res) => {        
  res.render(__dirname+'/public/index.html',{     
    'uuid' : app_uuid
  });
});
~~~
`nodejs에서 받는 코드(index.html)`
~~~html
 <p> uuid : <%= uuid%><br></p>
~~~
`http`      
<img src="https://user-images.githubusercontent.com/38582562/46142385-c4a51600-c291-11e8-8a98-39d1f742cde3.PNG" width="60%"><br><br>
`https`      
<img src="https://user-images.githubusercontent.com/38582562/46142386-c4a51600-c291-11e8-862b-9f970a255a60.PNG" width="60%"><br><br>
