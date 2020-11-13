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

	//��Ʈ ���� �ϴ� �κ� 
	public void connectSerial() throws Exception {

		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			commPort = portIdentifier.open(this.getClass().getName(),2000);
			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);
				serialPort.setSerialPortParams(9600, // ��żӵ�
						SerialPort.DATABITS_8, // ������ ��Ʈ
						SerialPort.STOPBITS_1, // stop ��Ʈ
						SerialPort.PARITY_NONE); // �и�Ƽ
				in = serialPort.getInputStream();//��ȣ�� ��Ʈ�� ������
				bin = new BufferedInputStream(in);
				
				// ������ ������ ��Ʈ�� ����
				out = serialPort.getOutputStream();//��Ʈ�� ���� ��ȣ�� ��Ŭ������ ����ϴ� ��
			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}
	}

	//��Ʈ�� ������ �Ƶ��̳� �����͸� ��Ŭ������ �����ϴ� �ڵ� 
	
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
//			//ó����  �������� ��� �߰��ϰڴ�.
			this.data = ":G11A9\r";
		}

		public SerialWriter(String serialData) {
			// CheckSum Data ����
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
				
//				// can ��ſ� �����Ұ�!
				out.write(inputData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	

	
	// Asynchronized Receive Data
	// --------------------------------------------------------
	// �̺�Ʈ�� �����ϸ� �갡 ����ȴ�.
	// �Ƶ��̳뿡�� ������ ��ȣ�� ��Ŭ������ ���� �� �ִ� �ڵ� 
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
				
				//������ serial data������
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
	
	//IoT���� ������ ������ �޴� ������
	//������ �����͸� CMD ��� �ϰ�
	//�� ���� ������ t1�� �ְ�
	//runnable ��ü �����Ͽ� ������ 
	public void sendIoT(String cmd) {
	
		Thread t1 = new Thread (new SendIoT(cmd));
		t1.start();
	}
	class SendIoT implements Runnable{

		String cmd; //������ ������ 
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





