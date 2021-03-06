package com.example.hundun.socketdemo;

import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MainActivity extends Activity{

    private final String HOST_IP="192.168.4.1";
    private final int HOST_PORT=2121;

    public static final String URI = "content://sms/";

    private Thread commandThread;

    //UI
    Button btn_accept;
    Button btn_modeOn;
    Button btn_modeOff;
    Button btn_modeAnalog;
    EditText txtCommand;
    EditText txtEcho;
    Button btn_coffee;

    //abstract

    //cmd
    final String CMD_ON="-MODE on";
    final String CMD_OFF="-MODE off";
    final String CMD_ANALOG="-MODE heat";
    final String CMD_TIMING_ON="-MODE timing_on";
    final String CMD_TIMING_OFF="-MODE timing_off";

    final String SMS_COFFEE="coffee";
    final String CMD_SET_TIME="-TIMING ";

    final int COMMIT_SPACE_DELAY=500;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_accept = (Button) findViewById(R.id.btn_accept);
        btn_modeOn = (Button) findViewById(R.id.btn_modeOn);
        btn_modeOff = (Button) findViewById(R.id.btn_modeOff);
        btn_modeAnalog = (Button) findViewById(R.id.btn_modeAnalog);
        txtCommand=(EditText) findViewById(R.id.txtCommand);
        txtEcho=(EditText) findViewById(R.id.txtEcho);
        btn_coffee= (Button) findViewById(R.id.btn_coffee);

        //注册内容观察者
        SMSContentObserver smsContentObserver =
                new SMSContentObserver(new Handler(),this);

        this.getContentResolver().registerContentObserver
                (Uri.parse(URI), true, smsContentObserver);

        //回调
        smsContentObserver.setOnReceivedMessageListener(new SMSContentObserver.MessageListener() {
            @Override
            public void OnReceived(String message) {
                txtEcho.append("sms:"+message+'\n');
                if(message.equals(CMD_ON)||message.equals(CMD_OFF)||message.equals(CMD_ANALOG)){
                    txtCommand.setText(message);
                    commitCommand();
                }
                else if(message.equals(SMS_COFFEE)){
                    orderCoffee();
                }
            }
        });

        btn_accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitCommand();
            }});
        btn_modeOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtCommand.setText(CMD_ON);
            }});
        btn_modeOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtCommand.setText(CMD_OFF);
            }});
        btn_modeAnalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtCommand.setText(CMD_ANALOG);
            }});
        btn_coffee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderCoffee();
            }});

        //Toast.makeText(getApplicationContext(), "init",Toast.LENGTH_SHORT).show();
    }

    private void commitCommand(){
        //wait last command finish
        try
        {
            if(commandThread!=null)
                commandThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        //wait Arduino handler finish
        new Handler().postDelayed(new Runnable(){
            public void run() {
            }
        }, COMMIT_SPACE_DELAY);

        String command=txtCommand.getText().toString();
        commandThread=new MyThread(command,txtEcho);
        commandThread.start();
    }

    private void orderCoffee(){
        //step 1,set on
        txtCommand.setText(CMD_ON);
        commitCommand();
        //step 2,set timing off
        txtCommand.setText(CMD_TIMING_OFF);
        commitCommand();
        //step 3,set time for cooking coffee(150sec)
        txtCommand.setText(CMD_SET_TIME+String.valueOf(150));
        commitCommand();
    }

    private String acceptServer(String command) throws IOException {
        String echo;
        //1.创建客户端Socket，指定服务器地址和端口
        Socket socket = new Socket(HOST_IP, HOST_PORT);
        //2.获取输出流，向服务器端发送信息
        OutputStream os = socket.getOutputStream();//字节输出流
        PrintWriter pw = new PrintWriter(os);//将输出流包装为打印流

        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //获取客户端的IP地址
        //InetAddress address = InetAddress.getLocalHost();
        //String ip = address.getHostAddress();
        pw.write(command);
        pw.flush();

        try{
            //从服务器端接收数据有个时间限制（系统自设，也可以自己设置），超过了这个时间，便会抛出该异常
            echo= br.readLine();
            //txtCommand.append("\n"+echo);
            //Toast.makeText(MainActivity.this, echo,Toast.LENGTH_SHORT).show();
        }catch(SocketTimeoutException e){
            echo="Time out, No response";
            //txtCommand.append("\n"+"Time out, No response");
            //Toast.makeText(MainActivity.this, "Time out, No response",Toast.LENGTH_SHORT).show();
        }

        if(socket != null){
            //如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
            socket.shutdownOutput();//关闭输出流
            socket.close(); //只关闭socket，其关联的输入输出流也会被关闭
        }

        return "echo："+echo;

    }


    public class MyThread extends Thread
    {
        private String command;
        EditText txtEcho;

        public MyThread(String command,EditText txtEcho)
        {
            this.command=command;
            this.txtEcho=txtEcho;
            //Thread thread =this;
            //thread.run();
        }

        @Override
        public void run() {
            try {
                String echo=acceptServer(command);
                txtEcho.append("\n"+echo);

            } catch (Exception e) {
                e.printStackTrace();
                //Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }




}
