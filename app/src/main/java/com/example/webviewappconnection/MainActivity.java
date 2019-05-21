package com.example.webviewappconnection;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private HashMap<String, String> appHashmap = new HashMap<>();
    private String unLunchStr = "";
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        // アプリリスト取得
        appHashmap = getPackages();

        setWebView();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // クリック時の処理
                String json = appHashmap.keySet().toString();
                json = json.replace("[","[\"");
                json = json.replaceAll(", ","\",\"");
                json = json.replace("]","\"]");

                webView.loadUrl("javascript:setAppBtn('"+json+"');");
                webView.loadUrl("javascript:setMsg('"+unLunchStr+"');");
            }
        });
    }

    /**
     * 【Android】WebView内のJavaScriptやHTMLと相互連携する方法
     * http://web-terminal.blogspot.com/2013/05/androidwebviewjavascripthtml.html
     */
    private void setWebView(){
        //http://web-terminal.blogspot.com/2013/05/androidwebviewjavascripthtml.html
        webView = findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/index.html");
        webView.getSettings().setJavaScriptEnabled(true);
        // リンクがクリックされたイベントを拾う
        webView.setWebViewClient( new WebViewClient(){
            public boolean shouldOverrideUrlLoading( WebView view_, String url_ ){
                String request_string = url_;
                if(request_string.startsWith("app://app_")){
                    setApp(request_string);
                    return true;
                }else if(request_string.startsWith("app://system_")){
                    setSystem(request_string);
                    return true;
                }else if(request_string.startsWith("app://msg_")){
                    textView.setText(request_string);
                    return true;
                }else if(request_string.startsWith("http")){
                    textView.setText(request_string);
                }
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                // jsを実行して、タイトルを取得
                webView.loadUrl("javascript:location.href = 'app://msg_' + document.title;");
                // タイトルを取得したいだけなら、こちらの方がよい
                //textView.setText("Title:"+webView.getTitle());
                super.onPageFinished(view, url);
            }

        });
    }

    private void setApp(String str){
        String packageName = str.substring("app://app_".length());
        String className = appHashmap.get(packageName);
        textView.setText(packageName +":"+className);
        if(className != null){
            Intent intent = new Intent();
            intent.setClassName(packageName,className);
            startActivity(intent);
        }
    }

    /**
     * 端末の音量をコードから変更する
     * http://kojiko-android.hatenablog.com/entry/2017/02/10/023647
     * @param str
     */
    private void setSystem(String str){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int nowVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        if(str.equals("app://system_audioup")){
            nowVol ++;
        }else if(str.equals("app://system_audiodown")){
            nowVol --;
        }
        nowVol = Math.max(0,Math.min(maxVol,nowVol));
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM,nowVol,AudioManager.FLAG_SHOW_UI);
        textView.setText(str+" volume:"+nowVol);
    }

    /**
     * Android他のアプリを立ち上げるための手順
     * https://qiita.com/xu1718191411/items/25faefe055ebb315d041
     * @return パッケージ名:クラス名
     */
    private HashMap<String, String> getPackages(){
        PackageManager pm = getPackageManager();
        List<PackageInfo> pckInfoList = pm.getInstalledPackages(
                PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        HashMap<String, String> result = new HashMap<String, String>();
        unLunchStr = "起動不可能パッケージ名：";
        for(PackageInfo pckInfo : pckInfoList){
            // 起動可能なパッケージ
            if(pm.getLaunchIntentForPackage(pckInfo.packageName) != null){
                String packageName = pckInfo.packageName;
                String className = pm.getLaunchIntentForPackage(pckInfo.packageName).getComponent().getClassName()+"";
                result.put(packageName, className);
            }else{
                // 起動不可能なパッケージ
                unLunchStr += pckInfo.packageName+",";
            }
        }
        return result;
    }

    /**
     * 閲覧履歴を前後に移動
     * https://www.javadrive.jp/android/webview/index3.html
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()){
                webView.goBack();
            }
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
