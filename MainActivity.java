package com.example.androidnodejshttphttps;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity  {
    public SharedPreferences FIRSTPre;                   //처음실행여부 저장할 SharedPreferences 선언
    private SharedPreferences.Editor FIRSTPreEdit;

    private SharedPreferences UUIDPre;                     //UUID값 저장할 SharedPreferences 선언
    private SharedPreferences.Editor UUIDPreEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FIRSTPre = getSharedPreferences("isFirst", MainActivity.MODE_PRIVATE); //최초 실행 여부 확인 위해 getSharedPreferences 사용
        FIRSTPreEdit = FIRSTPre.edit();

        UUIDPre = getSharedPreferences("setting", 0);                    //UUID값 저장 위해 getSharedPreferences 사용
        UUIDPreEdit = UUIDPre.edit();                                                   //0 == SharedPreferences 읽기, 쓰기 가능

        boolean first = FIRSTPre.getBoolean("isFirst", false);
        if(first==false){                                                              //최초 실행시
            FIRSTPreEdit.putBoolean("isFirst",true);
            UUIDPreEdit.putString("UUIDValue", UUIDTest());                            //UUID값
            UUIDPreEdit.commit();                                                      //저장
            FIRSTPreEdit.commit();
        }

        String result = UUIDPre.getString("UUIDValue", "0");            //getSharedPreferences에서 불러옴
        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();      //TOAST로 표시, 확인용
        new JSONTask().execute("http://YOURSERVER:3000/post");//AsyncTask 시작시킴     //uuid를 렉스로 보낼 수단 ////#######################################
    }

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

    public static String UUIDTest() {                                                   //UUID 생성
        UUID one = UUID.randomUUID();
        System.out.println("UUID One: " + one.toString());
        return one.toString();
    }
}
