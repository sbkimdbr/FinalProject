package com.can;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.chat.Client;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class SendAndReceiveSerial implements SerialPortEventListener {
	
	private BufferedInputStream bin;
	private InputStream in;
	private OutputStream out;
	private SerialPort serialPort;
	private CommPortIdentifier portIdentifier;
	private CommPort commPort;
	private String result;
	private String rawCanID, rawTotal;
	// private boolean start = false;
	private Client client;

	public SendAndReceiveSerial(String portName, boolean mode) {

		Client client;
		try {
			client = new Client("192.168.123.106", 5555, "[IoT]");
			client.connect();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			if (mode == true) {
				portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
				System.out.printf("Port Connect : %s\n", portName);
				connectSerial();
				// Serial Initialization ....
				(new Thread(new SerialWriter())).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	//포트 오픈 하는 부분 
	public void connectSerial() throws Exception {

		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			commPort = portIdentifier.open(this.getClass().getName(),2000);
			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);
				serialPort.setSerialPortParams(9600, // 통신속도
						SerialPort.DATABITS_8, // 데이터 비트
						SerialPort.STOPBITS_1, // stop 비트
						SerialPort.PARITY_NONE); // 패리티
				in = serialPort.getInputStream();//신호가 포트로 들어오고
				bin = new BufferedInputStream(in);
				
				// 밖으로 나가는 스트림 생성
				out = serialPort.getOutputStream();//포트로 들어온 신호를 이클립스로 출력하는 곳
			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}
	}

	//포트로 들어오는 아두이노 데이터를 이클립스로 전달하는 코드 
	
	public void sendSerial(String rawTotal, String rawCanID) {
		this.rawTotal = rawTotal;
		this.rawCanID = rawCanID;
		// System.out.println("send: " + rawTotal);
		try {
			 //Thread.sleep(50);
			Thread.sleep(30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Thread sendTread = 
				new Thread(new SerialWriter(rawTotal));
		sendTread.start();
	}

	private class SerialWriter implements Runnable {
		String data;

		public SerialWriter() {
//			//처음에  프로토콜 장비에 추가하겠다.
			this.data = ":G11A9\r";
		}

		public SerialWriter(String serialData) {
			// CheckSum Data 생성
			this.data = sendDataFormat(serialData);
		}

		public String sendDataFormat(String serialData) {
			serialData = serialData.toUpperCase();
			char c[] = serialData.toCharArray();
			int cdata = 0;
			for (char cc : c) {
				cdata += cc;
			}
			cdata = (cdata & 0xFF);

			String returnData = ":";
			returnData += serialData + Integer.toHexString(cdata).toUpperCase();
			returnData += "\r";
			return returnData;
		}

		public void run() {
			try {

				byte[] inputData = data.getBytes();
				
//				// can 통신에 참여할게!
				out.write(inputData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	

	
	// Asynchronized Receive Data
	// --------------------------------------------------------
	// 이벤트를 실행하면 얘가 실행된다.
	// 아두이노에서 들어오는 신호를 이클립스가 받을 수 있는 코드 
	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[128];

			try {

				while (bin.available() > 0) {
					int numBytes = bin.read(readBuffer);
				}

				String ss = new String(readBuffer);
				System.out.println("Receive Low Data:" + ss + "||");
				
				//서버에 serial data보낸다
				//client.sendTarget("192.168.123.106",ss);

			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}

	

	

	public void close() throws IOException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (in != null) {
			in.close();
		}
		if (out != null) {
			out.close();
		}
		if (commPort != null) {
			commPort.close();
		}

	}
	
	//IoT에서 들어오는 데이터 받는 쓰레드
	//들어오는 데이터를 CMD 라고 하고
	//그 값을 쓰레드 t1에 넣고
	//runnable 객체 생성하여 보낸다 
	public void sendIoT(String cmd) {
	
		Thread t1 = new Thread (new SendIoT(cmd));
		t1.start();
	}
	class SendIoT implements Runnable{

		String cmd; //들어오는 데이터 
		public SendIoT(String cmd) {
			this.cmd = cmd;
		}

		
		@Override
		public void run() {
		     byte[]datas=cmd.getBytes();
		     try {
		    	 System.out.println(datas);
				out.write(datas);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	

	public static void main(String args[]) throws IOException {

		SendAndReceiveSerial ss = new SendAndReceiveSerial("COM3", true);
		//ss.sendSerial("W2810003B010000000000005011", "10"//+ "003B01");
		//ss.sendIoT("start");		
		//ss.close();
		

	}

}





