package com.example.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.yang.sharemethod.GetSystemIP;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class ActivityTCPServer extends Activity {
	private final static char[] mChars = "0123456789ABCDEF".toCharArray();
	EditText editTextPort,editTextSendData;//端口号,发送数据框
	Button buttonMonitor;//监听按钮
	Button buttonClear,buttonSend;//清除和发送
	Spinner spinnerClient;//客户端列表
	private List<String> listClient = new ArrayList<String>();;//小锟斤拷 
	private ArrayAdapter<String> arrayAdapterClient;//小锟斤拷锟斤拷锟斤拷锟斤拷
	int AreaSpinnerValue = 0;//字符串对应的id
	
	boolean MonitorFlage = false;//按钮循环显示连接和断开
	boolean MonitorConnectFlage = true;//监听任务
	private ServerSocket serverSocket = null;//创建ServerSocket对象
	int socketPort = 8888;
	Socket socket = null;////连接通道，创建Socket对象
	OutputStream outputStream;
	
	WifiManager wifiManager;
	boolean threadReadDataFlage = false;//接收数据任务一直运行控制
	boolean threadSendDataFlage = false;//发送数据任务一直运行控制
	boolean SendDataFlage = false;//可以发送数据
	
	
	RadioGroup radioGroupSendMode,radioGroupEncoderMode,radioGroupReadShowMode,radioGroupSendEncoderMode;
	RadioButton radioButton1_0,radioButton1_1,radioButton1_2,
	radioButton2_0,radioButton2_1,radioButton2_2,radioButton3_0,radioButton3_1,radioButton3_2
	,radioButton4_0,radioButton4_1,radioButton4_2;
	
	TextView textViewReadData;//显示信息的文本框
	ScrollView scrollViewFather,scrollViewChild;
	
	int ReadDataMode=0;//接收数据的方式
	int ReadDataEncoderMode=0;//接收数据编码的方式
	String ReadDataEncoderModeString = "UTF-8";//默认编码方式
	
	int SentMode=0;//发送数据的方式
	int SentEncoderMode=0;//发送数据编码的方式
	String SendDataEncoderModeString = "UTF-8";//默认编码方式
	
	byte[] SendBuffer = new byte[2048];//存储发送的数据
	int SendDataCnt = 0;//控制发送数据的个数
	
	ArrayList<Socket> arrayListSockets = new ArrayList<Socket>();//存储连接的Socket
	
	private SharedPreferences sharedPreferences;//存储数据
	private SharedPreferences.Editor editor;//存储数据
	
	String PortSaveData = "";
	String sendDataString = "";
	
	Intent TcpServerIntent = new Intent();//跳转界面
	
	TextView textViewUse,textViewback;//使用说明,返回
	
	Thread mthreadSendData;//记下发送任务,便于停止
	Thread mthreadMonitorConnect;//记下监听任务,便于停止
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tcpserver);
		
		sharedPreferences = ActivityTCPServer.this.getSharedPreferences("PortSaveData",MODE_PRIVATE );
		PortSaveData = sharedPreferences.getString("PortData", "8080");
		sendDataString =  sharedPreferences.getString("sendData", "");
		
		
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);  
		
		editTextPort = (EditText) findViewById(R.id.editTextTCPServer1);//端口号
		buttonMonitor = (Button) findViewById(R.id.ButtonTCPServer2);//监听按钮
		buttonMonitor.setOnClickListener(buttonMonitorClick);
		spinnerClient = (Spinner) findViewById(R.id.spinnerTCPServer1);//客户端列表
		
		arrayAdapterClient = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, listClient); 
		arrayAdapterClient.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerClient.setAdapter(arrayAdapterClient);
		
		arrayAdapterClient.notifyDataSetChanged();
		
		
		textViewReadData = (TextView) findViewById(R.id.textViewTCPServer16);/////////////////////////////
		textViewReadData.setMovementMethod(new ScrollingMovementMethod());
		
		scrollViewFather = (ScrollView) findViewById(R.id.scrollViewTCPServer1);
		scrollViewFather.setOnTouchListener(scrollViewFatherTouch);
		
		scrollViewChild = (ScrollView) findViewById(R.id.scrollViewTCPServer2);
		scrollViewChild.setOnTouchListener(scrollViewChildTouch);
		
		buttonClear = (Button) findViewById(R.id.buttonTCPServer1);//清除
		buttonClear.setOnClickListener(buttonClearClick);
		buttonSend = (Button) findViewById(R.id.ButtonTCPServer3);//发送
		buttonSend.setOnClickListener(buttonSendClick);
		
		radioGroupReadShowMode = (RadioGroup)findViewById(R.id.radioGroupTCPServer2);//发送的格式
		radioGroupReadShowMode.setOnCheckedChangeListener(radioGroupReadShowModeChange);
		radioButton2_0 = (RadioButton) findViewById(R.id.radioTCPServer2_0);
		radioButton2_1 = (RadioButton) findViewById(R.id.radioTCPServer2_1);
		
		radioGroupEncoderMode = (RadioGroup)findViewById(R.id.radioGroupTCPServer3);
		radioGroupEncoderMode.setOnCheckedChangeListener(radioGroupEncoderModeChange);
		radioButton3_0 = (RadioButton) findViewById(R.id.radioTCPServer3_0);
		radioButton3_1 = (RadioButton) findViewById(R.id.radioTCPServer3_1);
		radioButton3_2 = (RadioButton) findViewById(R.id.radioTCPServer3_2);
		
		radioGroupSendMode = (RadioGroup)findViewById(R.id.radioGroupTCPServer1);
		radioGroupSendMode.setOnCheckedChangeListener(radioGroupSendModeChange);
		radioButton1_0 = (RadioButton) findViewById(R.id.radioTCPServer1_0);
		radioButton1_1 = (RadioButton) findViewById(R.id.radioTCPServer1_1);
		radioButton1_2 = (RadioButton) findViewById(R.id.radioTCPServer1_2);
		
		radioGroupSendEncoderMode = (RadioGroup)findViewById(R.id.RadioGroupTCPServer4);
		radioGroupSendEncoderMode.setOnCheckedChangeListener(radioGroupSendEncoderModeChange);
		radioButton4_0 = (RadioButton) findViewById(R.id.radioTCPServer4_0);
		radioButton4_1 = (RadioButton) findViewById(R.id.radioTCPServer4_1);
		radioButton4_2 = (RadioButton) findViewById(R.id.radioTCPServer4_2);
		
		editTextSendData = (EditText) findViewById(R.id.editTextTCPServer2);//发送数据的文本框
		editTextSendData.setText(sendDataString);
		
		editTextPort.setText(PortSaveData);
		editTextPort.setSelection(PortSaveData.length());
		
		textViewUse = (TextView) findViewById(R.id.textViewTCPServer12);//使用说明
		textViewUse.setOnClickListener(textViewUseClick);
		
		textViewback = (TextView) findViewById(R.id.textViewTCPServer11);//返回
		textViewback.setOnClickListener(textViewbackClick);
	}
	
	/**
	 * 使用说明点击
	 */
	private OnClickListener textViewUseClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			TcpServerIntent.setClass(ActivityTCPServer.this,ActivityAboutTCPServer.class);
			ActivityTCPServer.this.startActivity(TcpServerIntent);
		}
	};
	/**
	 * 返回主界面
	 */
	private OnClickListener textViewbackClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			TcpServerIntent.setClass(ActivityTCPServer.this, MainActivity.class);
			ActivityTCPServer.this.startActivity(TcpServerIntent);
//			new Thread () { 
//				public void run () { 
//					try 
//					{ 
//						Instrumentation inst= new Instrumentation(); 
//						inst.sendKeyDownUpSync(KeyEvent. KEYCODE_BACK); 
//						finish();
//					} 
//					catch(Exception e) { e.printStackTrace(); } 
//				} 
//			}.start();
		}
	};
	
	/*
	 * 发送编码格式选择
	 */
	private OnCheckedChangeListener radioGroupSendEncoderModeChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			if (checkedId == radioButton4_0.getId()) {
				SendDataEncoderModeString = "UTF-8";
			}
			else if (checkedId == radioButton4_1.getId()) {
				SendDataEncoderModeString = "UNICODE";
			}
			else if (checkedId == radioButton4_2.getId()) {
				SendDataEncoderModeString = "GBK";
			}
			hintKbTwo();//关闭键盘
		}
	};
	
	/**
	 * 选择发送的格式
	 */
	private OnCheckedChangeListener radioGroupSendModeChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			if (checkedId == radioButton1_0.getId()) {
				SentMode=0;
			}
			else if (checkedId == radioButton1_1.getId()) {
				SentMode=1;
			}
			else if (checkedId == radioButton1_2.getId()) {
				SentMode=2;
			}
			hintKbTwo();//关闭键盘
		}
	};
	
	/**
	 * 接收显示格式选择
	 */
	private OnCheckedChangeListener radioGroupReadShowModeChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			if (checkedId == radioButton2_0.getId()) {
				ReadDataMode = 0;
			}
			else if (checkedId == radioButton2_1.getId()) {
				ReadDataMode = 1;
			}
			hintKbTwo();//关闭键盘
		}
	};
	/**
	 * 接收编码格式选择
	 */
	private OnCheckedChangeListener radioGroupEncoderModeChange = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			if (checkedId == radioButton3_0.getId()) {
				ReadDataEncoderModeString = "UTF-8";
			}
			else if (checkedId == radioButton3_1.getId()) {
				ReadDataEncoderModeString = "UNICODE";
			}
			else if (checkedId == radioButton3_2.getId()) {
				ReadDataEncoderModeString = "GBK";
			}
			hintKbTwo();//关闭键盘
		}
	};
	/**
	 * 发送数据按钮
	 */
	private OnClickListener buttonSendClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			try 
	        {
				hintKbTwo();//关闭键盘
				switch (SentMode) 
				{
					case 0:
						byte[] mSendBuffer0 = editTextSendData.getText().toString().replace(" ", "").getBytes(SendDataEncoderModeString);
						
						for (int i = 0; i < mSendBuffer0.length; i++) {
							SendBuffer[i] = mSendBuffer0[i];
						}
						
						SendDataCnt = mSendBuffer0.length;
						break;
					case 1:
						byte[] mSendBuffer1 = str2HexStr(editTextSendData.getText().toString().replace(" ", "")).getBytes(SendDataEncoderModeString);

						for (int i = 0; i < mSendBuffer1.length; i++) {
							SendBuffer[i] = mSendBuffer1[i];
						}
						SendDataCnt = mSendBuffer1.length;		
						break;
					case 2:
						
						byte[] mSendBuffera = editTextSendData.getText().toString().replace(" ", "").getBytes(SendDataEncoderModeString);
						String mString = new String(mSendBuffera);
						
						byte[] mSendBuffer2 = HexString2Bytes(mString);
//						byte[] mSendBuffer2 = HexString2Bytes(editTextSendData.getText().toString().replace(" ", ""));
						for (int i = 0; i < mSendBuffer2.length; i++) {
							SendBuffer[i] = mSendBuffer2[i];
						}
						SendDataCnt = mSendBuffer2.length;	
						break;
						
					default:
						break;
				}
				
				String sendData = editTextSendData.getText().toString().replace(" ", "");
				editor = sharedPreferences.edit();
				editor.putString("sendData", sendData);
				editor.commit();
			} 
	        catch (Exception e) 
	        {
	        	
	        	Toast.makeText(getApplicationContext(),e.toString(), 500).show();
	        	
	        	if (SendDataEncoderModeString.equals("UNICODE")) {
	        		Toast.makeText(getApplicationContext(),"提示\r\n" +
	        				"一些数据格式暂不支持'UNICODE'编码", 500).show();
				}
	        	else if (SendDataEncoderModeString.equals("UTF-8")) {
	        		Toast.makeText(getApplicationContext(),"提示\r\n" +
	        				"一些数据格式暂不支持'UTF-8'编码", 500).show();
				}
	        	else if (SendDataEncoderModeString.equals("GBK")) {
	        		Toast.makeText(getApplicationContext(),"提示\r\n" +
	        	"一些数据格式暂不支持'GBK'编码", 500).show();
				}
			}
		}
	};
	
	/*清空接收*/
	private OnClickListener buttonClearClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			textViewReadData.setText("");
			hintKbTwo();//关闭键盘
		}
	};
	/*为实现两个scrollView不冲突*/
	private OnTouchListener scrollViewFatherTouch = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			findViewById(R.id.scrollViewTCPServer2).getParent().requestDisallowInterceptTouchEvent(false); 
			hintKbTwo();//关闭键盘
			return false;
		}
	};
	/*为实现两个scrollView不冲突*/
	private OnTouchListener scrollViewChildTouch = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			v.getParent().requestDisallowInterceptTouchEvent(true); 
			hintKbTwo();//关闭键盘
			return false;
		}
	};
	
	
	/*监听按钮*/
	private OnClickListener buttonMonitorClick = new OnClickListener() {
		@Override
		public void onClick(View v) 
		{
			String showIPAdressString = "";
			if (GetSystemIP.getMobileDataState(getApplicationContext()) == -1) {
				Toast.makeText(getApplicationContext(), "提示\r\n获取手机网络出错", 500).show();
			}
			else if (GetSystemIP.getMobileDataState(getApplicationContext()) == 0){//没有网络连接
				showIPAdressString = GetSystemIP.getLocalIpAddress();
			}
			else if (GetSystemIP.getMobileDataState(getApplicationContext()) == 1){//移动数据
				showIPAdressString = GetSystemIP.getLocalIpAddress();
			}
			else if (GetSystemIP.getMobileDataState(getApplicationContext()) == 2){//无线
				showIPAdressString = GetSystemIP.getGatSystemIP(getApplicationContext());
			}
			
			if (MonitorFlage == false)
			{
				try 
				{
					String mString = editTextPort.getText().toString().replace(" ", "");
					String temp[] = mString.split(":");
					if (temp.length>1) {//前面有IP地址
						socketPort =Integer.valueOf(temp[1]);//获取端口号 
					}
					else {
						socketPort =Integer.valueOf(mString);//获取端口号 
					}
					
					editTextPort.setText(showIPAdressString+":"+socketPort);
					serverSocket = new ServerSocket(socketPort);
					
					MonitorConnectFlage = true;//监听任务
					
					ThreadMonitorConnect threadMonitorConnect = new ThreadMonitorConnect();
					threadMonitorConnect.start();
					mthreadMonitorConnect = threadMonitorConnect;//记下监听任务
					
//	        		try {threadMonitorConnect.start();} catch (Exception  e) 
					//{threadMonitorConnect.run();}//启动监听任务--此方法在一些版本中不可行	
	        		MonitorFlage = true;
	        		buttonMonitor.setText("停止监听");
	        		
	        		
					editor = sharedPreferences.edit();
					editor.putString("PortData", socketPort+"");
					editor.commit();
	        		
				} catch (Exception e1) {
					Toast.makeText(getApplicationContext(), "提示\r\n监听出错,请检查端口号", 500).show();
				}
				
			}
			else 
			{
				try {serverSocket.close();} catch (Exception e) {}
				try {socket.close();} catch (Exception e) {}
				try 
				{
					for (Socket sk : arrayListSockets) 
					{
						try 
						{
							sk.close();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				
				MonitorConnectFlage = false;
				try {
					mthreadMonitorConnect.interrupt();
				} catch (Exception e) {
					// TODO: handle exception
				}
				
				MonitorFlage = false;
				
				threadSendDataFlage = false;//关闭发送的任务
				try 
				{
					mthreadSendData.interrupt();
				} catch (Exception e) {
					// TODO: handle exception
				}
				buttonMonitor.setText("启动监听");
				listClient.clear();
        		arrayAdapterClient.notifyDataSetChanged();
			}
			
		}
	};
	
	
	/**
	 * 监听连接任务
	 * @author yang
	 *
	 */
	private class ThreadMonitorConnect extends Thread
	{
		boolean mThreadMonitorConnectFlage = true;
		public void run()
		{
			while (mThreadMonitorConnectFlage && MonitorConnectFlage)
			{
				try 
				{
					socket = serverSocket.accept();//等待客户端连接
					
					arrayListSockets.add(socket);//添加socket
					
					String mString = (socket.getInetAddress()+":"+socket.getPort()).replace("/", "");
					listClient.add(mString);
					SendHandleMsg(mHandler,"ConState","new");
					
					threadReadDataFlage = true;//接收任务
					ThreadReadData threadReadData = new ThreadReadData(socket, mString);
					threadReadData.start();
					
					
					if (threadSendDataFlage == false) 
					{
						threadSendDataFlage = true;
						ThreadSendData threadSendData = new ThreadSendData();
						threadSendData.start();
						mthreadSendData = threadSendData;
					}
				} 
				catch (Exception e) 
				{
					try 
					{
						for (Socket sk : arrayListSockets) 
						{
							try {
								sk.close();
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					} catch (Exception e2) {
						// TODO: handle exception
					}
					
					mThreadMonitorConnectFlage = false;
					MonitorConnectFlage = false;
					MonitorFlage = false;
					SendHandleMsg(mHandler,"ConState","ConError");//向Handle发送消息
				}
			}
		}
	}
	
	/**
	 * 接收数据的任务
	 * @author yang
	 *
	 */
	private class ThreadReadData extends Thread
	{
		boolean mThreadReadDataFlage = true;
		String mStringSocketMsg = "";//存储连接的信息,方便删除
		private byte[] buf = new byte[1024];//数据缓存区
		private InputStream inputStream;//获取输入流
		private Socket mysocket;//获取传进来的socket
		private int len=0;//得到的数据的长度
		public ThreadReadData(Socket socket,String SocketMsg)
		{
			mysocket = socket;//获取socket
			mStringSocketMsg = SocketMsg;
			try 
			{
				inputStream = mysocket.getInputStream();//获取流
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			SendHandleMsg(mHandler,"Toast",mStringSocketMsg+"连接");//向Handle发送消息
		}
		public void run()
		{
			while (mThreadReadDataFlage && threadReadDataFlage)
			{
				try
				{
					len = inputStream.read(buf);//服务器断开会返回-1
					if (len == -1) //连接断开
					{
						mThreadReadDataFlage = false;
						SendHandleMsg(mHandler,"ConClose",mStringSocketMsg);//向Handle发送消息
						SendHandleMsg(mHandler,"Toast",mStringSocketMsg+"断开");//向Handle发送消息
						try {
							arrayListSockets.remove(mysocket);
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
					
					byte[] ReadBuffer0 = new byte[len];//存储接收到的数据
					
					for (int i = 0; i < len; i++) {
						ReadBuffer0[i] = buf[i];
					}
					
					SendHandleMsg(mHandler,"ReadData",ReadBuffer0);
				} 
				catch (Exception e) 
				{
					if (mThreadReadDataFlage) 
					{
						mThreadReadDataFlage = false;
						SendHandleMsg(mHandler,"ConClose",mStringSocketMsg);//向Handle发送消息
						SendHandleMsg(mHandler,"Toast",mStringSocketMsg+"断开");//向Handle发送消息
						try {
							arrayListSockets.remove(mysocket);
						} catch (Exception e1) {
							// TODO: handle exception
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * 发送数据任务
	 * @author yang
	 *
	 */
	class ThreadSendData extends Thread
	{
		private boolean mThreadFlage = true;
		public void run()
		{
			while (mThreadFlage && threadSendDataFlage)
			{
				if (SendDataCnt>0) //要发送的数据个数大于0
				{
					SendDataFlage = false;
					try 
					{
						String string = spinnerClient.getSelectedItem().toString();//
						String tempString[] = string.split(":");
						for (Socket sk : arrayListSockets) 
						{
							if (Integer.valueOf(tempString[1]) == sk.getPort()) 
							{
								outputStream = sk.getOutputStream();
								SendDataFlage = true;
								break;
							}
						}
					} catch (Exception e) {
						/*发送失败*/
						SendDataCnt = 0;
					}
					
					if (SendDataFlage && SendDataCnt>0) 
					{
						try 
						{
							outputStream.write(SendBuffer,0,SendDataCnt);//发送数据
							SendDataCnt = 0;//清零发送的个数
						} 
						catch (Exception e) 
						{
							
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Handler
	 */
	private Handler mHandler = new Handler()
	{
		@Override
	    public void handleMessage(Message msg) {
	        super.handleMessage(msg);
	        Bundle bundle = msg.getData(); 
	        /*****************************连接和断开*********************************/
	        String string = bundle.getString("ConState");//连接和断开
	        try 
	        {
	        	if(string.equals("new"))//有新的连接
				{
	        		arrayAdapterClient.notifyDataSetChanged();
	        		spinnerClient.setSelection(listClient.size()-1);
				}
	        	else if(string.equals("ConError")){
	        		buttonMonitor.setText("启动监听");
				}
				
			} catch (Exception e) {
				// TODO: handle exception
			}
	        
	        /***************************有连接断开了***********************************/
	        string = bundle.getString("ConClose");//有连接断开了
	        if (string != null) {
				try {
					listClient.remove(string);
					arrayAdapterClient.notifyDataSetChanged();
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
	        /***************************显示消息***********************************/
	        string = bundle.getString("Toast");//显示消息
	        if (string != null) {
				try{Toast.makeText(getApplicationContext(), string, 500).show();} catch (Exception e) {}
			}
	        
	        /*****************************接收到的消息*********************************/
	        byte[] ReadByte = bundle.getByteArray("ReadData");//接收到的消息
	        try 
	        {
	        	if (ReadDataMode == 0) {//字符串显示
	        		textViewReadData.append(new String(ReadByte,ReadDataEncoderModeString));
				}
	        	else if (ReadDataMode == 1) {//16进制显示
	        		textViewReadData.append(byteToHexStr(ReadByte));
				}
	        	
	        	textViewReadData.post(new Runnable() {
	                @Override
	                public void run() {
	                    // TODO Auto-generated method stub
//	                	textViewReadData.append(line);
	                    final int scrollAmount = textViewReadData.getLayout().getLineTop(textViewReadData.getLineCount()) - textViewReadData.getHeight();
	                    if (scrollAmount > 0)
	                    	textViewReadData.scrollTo(0, scrollAmount);
	                    else
	                    	textViewReadData.scrollTo(0, 0);
	                }
	            });
	        	
//	        	textViewReadData.post(new Runnable()
//        	    { 
//        	        public void run()
//        	        { 
//        	        	textViewReadData.scrollTo(0, textViewReadData.getBottom());
//        	        } 
//        	    });
	        	
//	        	scrollViewChild.post(new Runnable()
//        	    { 
//        	        public void run()
//        	        { 
//        	        	scrollViewChild.smoothScrollTo(0, textViewReadData.getBottom());//显示在最后
//        	        } 
//        	    });
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	};
	
	
	/**
	 * 
	 * @param handler
	 * @param key
	 * @param Msg
	 */
	private void SendHandleMsg(Handler handler,String key,String Msg)
    {
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putString(key, Msg);
		msg.setData(bundle);
		handler.sendMessage(msg);
    }
	
	/**
	 * 传送的byte
	 */
	private void SendHandleMsg(Handler handler,String key,byte[] byt)
    {
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putByteArray(key, byt);
		msg.setData(bundle);
		handler.sendMessage(msg);
    }
	
	
	//此方法只是关闭软键盘  
	private void hintKbTwo() 
	{  
		 InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);              
		 if(imm.isActive()&&getCurrentFocus()!=null)
		 {  
		    if (getCurrentFocus().getWindowToken()!=null) 
		    {  
			    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);  
		    }               
		 }  
	}  
	/**  
     * 字符串转换成十六进制字符串 
     * @param str String 待转换的ASCII字符串 
     * @return String 每个Byte之间空格分隔，如: [61 6C 6B] 
     */    
    public static String str2HexStr(String str)
    {    
        StringBuilder sb = new StringBuilder();  
        byte[] bs = str.getBytes();    
          
        for (int i = 0; i < bs.length; i++){    
            sb.append(mChars[(bs[i] & 0xFF) >> 4]);    
            sb.append(mChars[bs[i] & 0x0F]);  
            //sb.append(' ');  
        }    
        return sb.toString().trim();    
    }  
    /** 
	 * 将已十六进制编码后的字符串src，以每两个字符分割转换为16进制形式 如："2B44EFD9" --> byte[]{0x2B, 0x44, 0xEF, 
	 * 0xD9} 
	 * 
	 * @param src 
	 *            String 
	 * @return byte[] 
	 */ 
	public static byte[] HexString2Bytes(String str) { 
		StringBuilder sb = null;
		String src = null;
		if ((str.length()%2)!=0) {//数据不是偶数
			sb = new StringBuilder(str);//构造一个StringBuilder对象
	        sb.insert(str.length()-1, "0");//在指定的位置1，插入指定的字符串
	        src = sb.toString();
		}
		else {
			src = str;
		}
		Log.e("error", "str.length()"+str.length());
		byte[] ret = new byte[src.length() / 2]; 
		byte[] tmp = src.getBytes(); 
		for (int i = 0; i < tmp.length / 2; i++) 
		{ 
			ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]); 
		} 
		return ret; 
	} 
    
	/**
	 * 16进制byte转16进制String--用空格隔开
	 * @param bytes
	 * @return
	 */
	public static String byteToHexStr(byte[] bytes)
    {    
		String str_msg = "";
        for (int i = 0; i < bytes.length; i++){    
        	str_msg = str_msg + String.format("%02X",bytes[i])+" ";
        }    
        return str_msg;    
    }  
	
	/*16进制byte数组转String*/
	public static String bytes2HexString(byte[] b) {
	       String r = "";
	       for (int i = 0; i < b.length; i++) {
	           String hex = Integer.toHexString(b[i] & 0xFF);
	           if (hex.length() == 1) {
	               hex = '0' + hex;
	           }
	           r += hex.toUpperCase()+" ";
	       }

	       return r;
	   }
	
	/** 
	 * 将两个ASCII字符合成一个字节； 如："EF"--> 0xEF 
	 * 
	 * @param src0 
	 *            byte 
	 * @param src1 
	 *            byte 
	 * @return byte 
	 */ 
	public static byte uniteBytes(byte src0, byte src1) { 
		try 
		{
			byte _b0 = Byte.decode("0x"+new String(new byte[] { src0 })) .byteValue(); 
			_b0 = (byte) (_b0 << 4); 
			byte _b1 = Byte.decode("0x"+new String(new byte[] { src1 })) .byteValue(); 
			byte ret = (byte) (_b0 ^ _b1); 
			return ret; 
		} catch (Exception e) {
			// TODO: handle exception
		}
		return 0;
	} 
	
	
	/** 当活动(界面)不再可见时调用 */
    @Override
    protected void onStop() 
    {
    	try {serverSocket.close();} catch (Exception e) {}
		try {socket.close();} catch (Exception e) {}
		MonitorConnectFlage = false;
		MonitorFlage = false;
		
		listClient.clear();
		threadReadDataFlage = false;//接收数据任务一直运行控制
    	threadSendDataFlage = false;//发送数据任务一直运行控制
        super.onStop();
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	/**
//	 * 获取手机的IP地址,默认优先返回无线的IP
//	 * @return
//	 */
//	private String GetIPAdress()
//	{
//		String ip = "";
//		 //获取wifi服务  
//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);  
//        //判断wifi是否开启  
//        if (wifiManager.isWifiEnabled()) 
//        {  
//        	WifiInfo wifiInfo = wifiManager.getConnectionInfo();       
//            int ipAddress = wifiInfo.getIpAddress();   
//            ip = intToIp(ipAddress);   
//        }  
//        else {
//        	ip = getLocalIpAddress();
//		}
//        return ip;
//	}
//	private String intToIp(int i) {       
//        return (i & 0xFF ) + "." +       
//      ((i >> 8 ) & 0xFF) + "." +       
//      ((i >> 16 ) & 0xFF) + "." +       
//      ( i >> 24 & 0xFF) ;  
//	}   
//	
//	/**
//	 * 此方法在手机只打开数据流量的时候获取的是手机的公网IP,关闭手机网络,开启热点后获取的是本机的网关
//	 * @return
//	 */
//	//获取本地IP  
//    public static String getLocalIpAddress() {    
//           try {    
//               for (Enumeration<NetworkInterface> en = NetworkInterface    
//                               .getNetworkInterfaces(); en.hasMoreElements();) {    
//                           NetworkInterface intf = en.nextElement();    
//                          for (Enumeration<InetAddress> enumIpAddr = intf    
//                                   .getInetAddresses(); enumIpAddr.hasMoreElements();) {    
//                               InetAddress inetAddress = enumIpAddr.nextElement();    
//                               if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {    
//                               return inetAddress.getHostAddress().toString();    
//                               }    
//                          }    
//                       }    
//                   } catch (Exception ex) {    
//                       Log.e("WifiPreference IpAddress", ex.toString());    
//                   }    
//             
//             
//                return null;    
//   }   
//	
//	/**
//	 * 此方法是获取连接无线的网关
//	 * @return
//	 */
//	private String getGateWay() 
//	{  
//		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);  
//		
//		if (wifiManager != null)   
//		{  
//			DhcpInfo   dhcpInfo = wifiManager.getDhcpInfo();  
//			return GetSystemIP.long2ip(dhcpInfo.gateway);  
//        }  
//		else {
//			return "";
//		}
//    }   
	
}




















