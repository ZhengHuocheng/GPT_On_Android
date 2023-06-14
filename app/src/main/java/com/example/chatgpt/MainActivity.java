package com.example.chatgpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welocome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //设置recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v)->{
            String question = messageEditText.getText().toString().trim();
//            Toast.makeText(this,question, Toast.LENGTH_SHORT).show();
            addToChat(question,Message.SENT_BY_ME);
            messageEditText.setText("");//发送后清空输入文本栏
            callAPI(question);
            welcomeTextView.setVisibility(View.GONE);
        });
    }
//添加到消息列表使得我们可以点击、滑动到对应位置
    void addToChat(String message,String sentBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response){
        //出现响应时，去除“稍等显示”
        messageList.remove(messageList.size()-1);
        //请求的响应作为机器人的回答
        addToChat(response,Message.SENT_BY_BOT);

    }

//调用API方法
    void callAPI(String question){
        //完成所有OKHTTP设置
        //返回响应需要时间
        messageList.add(new Message("请稍等...",Message.SENT_BY_BOT));
        JSONObject jsonBody = new JSONObject();//为json主体传输所有openai要求的参数
        try {
            jsonBody.put("model","text-davinci-003");
            jsonBody.put("prompt",question);
            jsonBody.put("max_tokens",4000);//最大token设置4000，所以收到的消息会很长
            jsonBody.put("temperature",0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //收集media type信息
        RequestBody body = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization","Bearer ")//用户授权API秘钥与发送标头
                .post(body)
                .build();
//得到响应，进行回调函数内容
        client.newCall(request).enqueue(new Callback(){
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    //从响应的json文件中读取回答信息
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }else{
                    addResponse("on Response Failed to load response due to " + response.body().string());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //处理响应失败
                addResponse("on Failure Failed to load response due to " + e.getMessage());
            }
        });

    }

}