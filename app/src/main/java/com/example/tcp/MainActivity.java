package com.example.tcp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * **/
public class MainActivity extends AppCompatActivity {


    private static  final int PORT=9999; //服务器端口
    private static List<Socket> mClientList=new ArrayList<>();//创建一个集合存放所有的客户端的Socket对象
    private ExecutorService executorService;//线程池

    ServerSocket serverSocket;//创建ServerSocket对象
    Socket clicksSocket;//连接通道,创建Socket对象.用来临时保存客户端连接的Socket对象
    InputStream inputStream;//创建输入数据流
    OutputStream outputStream;//创建输出数据流
    byte[] TcpSendData=new byte[1024];//用来缓存接收数据的变量
    int TcpSendDataLen=0;//发送的数据个数

    TextView tv_ip;//ip地址
    Button btn_connect;//连接
    TextView tv_voltage;//电压
    TextView tv_current;//电流
    TextView tv_power;//功率
    EditText et_setU;//U_SET
    EditText et_setI;//I_SET
    EditText et_ovp;//ovp
    EditText et_ocp;//ocp
    Button btn_send;//发送

    ImageButton btn_switch;//开关
    private boolean isOn=false;//用来判断是否是开机状态



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_ip=findViewById(R.id.tv_ip);
        tv_ip.setText(getLocalIpAddress());
        btn_connect=findViewById(R.id.btn_connect);
        tv_voltage=findViewById(R.id.tv_voltage);
        tv_current=findViewById(R.id.tv_current);
        tv_power=findViewById(R.id.tv_power);
        et_setU=findViewById(R.id.et_setU);
        et_setI=findViewById(R.id.et_setI);
        et_ovp=findViewById(R.id.et_ovp);
        et_ocp=findViewById(R.id.et_ocp);
        btn_switch=findViewById(R.id.btn_switch);
        btn_send=findViewById(R.id.btn_send);


        btn_connect.setOnClickListener(connectListener);
        btn_send.setOnClickListener(sendButtonListener);



    }


    //点击连接启动服务按钮监听事件
    private View.OnClickListener connectListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Toast.makeText(MainActivity.this,"启动服务器监听",Toast.LENGTH_SHORT).show();

            /**启动服务器监听线程**/
            ServerSocket_thread serverSocket_thread=new ServerSocket_thread();//实例化服务器监听线程
            serverSocket_thread.start();//开启服务器监听线程
        }
    };

    /**
     * 服务器监听线程
     */

   class ServerSocket_thread extends Thread{

       @Override
       public void run() {
           try {
               serverSocket=new ServerSocket(PORT);//设置监听服务器port端口
               executorService=Executors.newCachedThreadPool();//创建一个线程池
               Log.d("==========","开始");

               clicksSocket=null;

               while (true){
                   clicksSocket=serverSocket.accept();//使服务端处于监听状态,等待接受一个连接
                   mClientList.add(clicksSocket);//接收客户连接并添加到List中
                   //开启一个客户端线程
                   executorService.execute(new ThreadServer(clicksSocket));
                   if(clicksSocket.isConnected()){
                       Log.d("==========","连接成功");
                   }

                   //启动接收线程
                   Receive_Thread receive_thread=new Receive_Thread();
                   receive_thread.start();


                 inputStream=clicksSocket.getInputStream();//获取输入流
                    outputStream=clicksSocket.getOutputStream();//获取输出流
               }
           } catch (IOException e) {
               e.printStackTrace();
               return;
           }
       }

   }



    //每个客户端单独开启一个线程
    static class ThreadServer implements Runnable{
       private Socket client;
       private BufferedReader  bufferedReader;
       private PrintWriter printWriter;
       private String mStrMSG;

        public ThreadServer(Socket socket) {
            this.client = socket;
            try {
                bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                mStrMSG="客户端IP是:"+this.client.getInetAddress()+"来自设备:"+mClientList.size();
               // sendMessage();//发送消息给所有客户端
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                while ((mStrMSG=bufferedReader.readLine())!=null){
                    if(mStrMSG.trim().equals("exit")){//当一个客户端退出时
                        mClientList.remove(client);
                        bufferedReader.close();
                        mStrMSG="user:"+this.client.getInetAddress()+"exit total:"+mClientList.size();
                        client.close();
                      //  sendMessage();
                        break;

                    }else {
                        mStrMSG=client.getInetAddress()+":"+mStrMSG;
                      //  sendMessage();
                    }

                }

            }catch (IOException e){
                e.printStackTrace();
            }

        }
        //发送消息给所有客户端
        private void sendMessage() {
            Log.d("========",mStrMSG);

            for(Socket clicksSocket:mClientList){
                try {
                    printWriter=new PrintWriter(clicksSocket.getOutputStream(),true);
                    printWriter.println(mStrMSG);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    /**
     *
     * 接收线程
     *
     */
    class Receive_Thread extends Thread//继承Thread
    {
        public void run()//重写run方法
        {
            while (true)
            {
                try
                {
                    final byte[] buf = new byte[1024];//创建接收缓冲区,接收数据,
                    final int len = inputStream.read(buf);//数据读出来,并且返回数据的长度

                    final byte[] Buffer=new byte[len];//创建一个新的数组
                    System.arraycopy(buf,0,Buffer,0,len);//拷贝数据


                    runOnUiThread(new Runnable()//更新ui信息
                    {
                        public void run()
                        {
                            //更新ui的操作代码
                            if(len>0){
                                //tv_voltage.setText(new String(buf,0,len));
                                tv_voltage.setText(bytyToHexstr(Buffer));
                            }


                        }
                    });
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**发送消息按钮事件
     * 开机关机按钮
     *
     * **/
    private View.OnClickListener sendButtonListener=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                //获取输出流
                outputStream = clicksSocket.getOutputStream();

                //后加  16进制发送
                byte[] TcpSendData1 = HexString2Bytes("AA 01 20 00 00 00 00"
                        .replace(" ", ""));//16进制发送
                TcpSendDataLen = TcpSendData1.length;
                TcpSendData = TcpSendData1;

                //发送数据
                //  outputStream.write(sendEditText.getText().toString().getBytes());

                //下面是后加的
                if (clicksSocket != null && clicksSocket.isConnected()) {//如果TCP是正常连接的
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                outputStream.write(TcpSendData, 0, TcpSendDataLen);//发送数据

                            } catch (Exception e) {

                            }

                        }
                    }).start();

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };



    /**
     *
     * 获取WIFI下ip地址
     */
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }


    //16进制字节数组转换为16进制字符串
    public static String bytyToHexstr(byte[] bytes){

        String str_msg="";
        for(int i=0;i<bytes.length;i++){
            str_msg=str_msg+String.format("%02X",bytes[i])+"";

        }
        return str_msg;
    }
    //发送时,把获取到的字符串转换为16进制
    /**
     * 添上格式,实际上咱获取的文本框里面的都是字符串,咱需要把字符串转化为  如"33"==>0x33
     * 将已十六进制编码后的字符串src,以每两个字符分割转换为16进制形式
     * 如:"2B44EED9"--> byte[]{0x2B,0x44,0xEF0xD9}
     *
     * **/
    public static byte[] HexString2Bytes(String str){
        StringBuilder sb=null;
        String src=null;
        if((str.length()%2)!=0){//数据不是偶数
            sb=new StringBuilder(str);//构造一个StringBuilder对象
            sb.insert(str.length()-1,"0");//在指定的位置1,插入指定的字符串
            src=sb.toString();

        }else {
            src=str;
        }
        Log.e("error","str.length"+str.length());
        byte[] ret=new byte[src.length()/2];
        byte[] tmp=src.getBytes();
        for(int i=0;i<tmp.length/2;i++){
            ret[i]=uniteBytes(tmp[i*2],tmp[i*2+1]);

        }
        return ret;

    }

    //将两个ASCII字符合成一个字节;如:"EF"-->0xEF.Byte.decode()将String解码为 Byte
    public static byte uniteBytes(byte src0,byte src1){

        try{
            byte _b0=Byte.decode("0x"+new String(new byte[]{src0})).byteValue();//.byteValue()转换为byte类型的数
            // 该方法的作用是以byte类型返回该 Integer 的值。只取低八位的值，高位不要。
            _b0= (byte) (_b0<<4);//左移4位
            byte _b1=Byte.decode("0x"+new String(new  byte[]{src1})).byteValue();
            byte ret= (byte) (_b0^_b1);//按位异或运算符(^)是二元运算符，要化为二进制才能进行计算
            return ret;

        }catch (Exception e){
            //TODO:handle exception
        }

        return 0;
    }

    /**
     * CRC检验值
     * @param modbusdata
     * @param length
     * @return CRC检验值
     */
    protected int crc16_modbus(byte[] modbusdata, int length)
    {
        int i=0, j=0;
        int crc = 0xffff;//有的用0,有的用0xff
        try
        {
            for (i = 0; i < length; i++)
            {
                //注意这里要&0xff,因为byte是-128~127,&0xff 就是0x0000 0000 0000 0000  0000 0000 1111 1111
                //参见:https://blog.csdn.net/ido1ok/article/details/85235955
                crc ^= (modbusdata[i]&(0xff));
                for (j = 0; j < 8; j++)
                {
                    if ((crc & 0x01) == 1)
                    {
                        crc = (crc >> 1) ;
                        crc = crc ^ 0xa001;
                    }
                    else
                    {
                        crc >>= 1;
                    }
                }
            }
        }
        catch (Exception e)
        {

        }

        return crc;
    }

    /**
     * CRC校验正确标志
     * @param modbusdata
     * @param length
     * @return 0-failed 1-success
     */
    protected int crc16_flage(byte[] modbusdata, int length)
    {
        int Receive_CRC = 0, calculation = 0;//接收到的CRC,计算的CRC

        Receive_CRC = crc16_modbus(modbusdata, length);
        calculation = modbusdata[length];
        calculation <<= 8;
        calculation += modbusdata[length+1];
        if (calculation != Receive_CRC)
        {
            return 0;
        }
        return 1;
    }

    @Override
    protected void onDestroy() {

        if (serverSocket!=null){
            try{
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
