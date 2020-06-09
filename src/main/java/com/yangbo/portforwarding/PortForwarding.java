package com.yangbo.portforwarding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortForwarding {
	
	private static int localPort = 0;
	private static String remotehost = null;
	private static int remotePort = 0;
	
	ExecutorService threadPool = null;
	
	private static int bufferSize = 1024;
	
	public static void main(String[] args) {
		PortForwarding pf = new PortForwarding();
		pf.init(args);
		pf.monitor();
	}
	
	private void monitor() {
		try(ServerSocket serverSocket = new ServerSocket(localPort)) {
			System.out.println("------->monitor "+localPort+" success!");
			
			while(true) {
				Socket accept = serverSocket.accept();
				threadPool.execute(new ForwardTask(accept));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init(String[] args) {
		localPort = Integer.parseInt(args[0]);
		remotehost = args[1];
		remotePort = Integer.parseInt(args[2]);
		
		System.out.println("------->localPort:"+localPort+"  remotehost:"+remotehost+"  remotePort:"+remotePort);
		
		threadPool = Executors.newFixedThreadPool(10);
		
	}
	
	class Tuple{
		Socket localSocket;
		Socket remoteSocket;
		
		public Tuple(Socket localSocket,Socket remoteSocket) {
			this.localSocket = localSocket;
			this.remoteSocket = remoteSocket;
		}

		public Socket getLocalSocket() {
			return localSocket;
		}

		public void setLocalSocket(Socket localSocket) {
			this.localSocket = localSocket;
		}

		public Socket getRemoteSocket() {
			return remoteSocket;
		}

		public void setRemoteSocket(Socket remoteSocket) {
			this.remoteSocket = remoteSocket;
		}
	}
	
	class ForwardTask implements Runnable{

		Socket localSocket;
		Socket remoteSocket;
		
		public ForwardTask(Socket accept) {
			this.localSocket = accept;
			
			System.out.println("------->get connect from " + localSocket.getInetAddress().getHostAddress());
			
			try {
				remoteSocket = new Socket(remotehost, remotePort);
				System.out.println("------->connect to "+remotehost + " " + remotePort + " success!");
				
			} catch (IOException e) {
				e.printStackTrace();
				closeLocalSocket();
				closeRemoteSocket();
			}
			
		}
		
		public void closeLocalSocket() {
			if(localSocket != null && !localSocket.isClosed()) {
				try {
					localSocket.close();
					System.out.println("------->localSocket closed !");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void closeRemoteSocket() {
			if(remoteSocket != null && !remoteSocket.isClosed()) {
				try {
					remoteSocket.close();
					System.out.println("------->remoteSocket closed !");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void run() {
			InputStream localInputStream = null;
			OutputStream localOutputStream = null;
			InputStream remoteInputStream = null;
			OutputStream remoteOutputStream = null;
			try {
				localInputStream = localSocket.getInputStream();
				localOutputStream = localSocket.getOutputStream();
				remoteInputStream = remoteSocket.getInputStream();
				remoteOutputStream = remoteSocket.getOutputStream();
				
				ExchangeTask exchangeTask1 = new ExchangeTask(localInputStream, remoteOutputStream);
				ExchangeTask exchangeTask2 = new ExchangeTask(remoteInputStream, localOutputStream);
				threadPool.execute(exchangeTask1);
				threadPool.execute(exchangeTask2);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		class ExchangeTask implements Runnable{

			InputStream inputStream;
			OutputStream outputStream;
			
			public ExchangeTask(InputStream inputStream,OutputStream outputStream) {
				this.inputStream = inputStream;
				this.outputStream = outputStream;
			}
			
			@Override
			public void run() {
				try {
					//需要发送实际读到的大小
					byte[] data = new byte[bufferSize];
					int len = 0;
					while((len = inputStream.read(data)) > 0){
						outputStream.write(data,0,len);
					}
					
				} catch (Exception e) {
//					System.out.println("------->send error");
//					e.printStackTrace();
				}finally {
					//读到-1表示socket已经关闭了
					closeLocalSocket();
					closeRemoteSocket();
				}
			}
			
		}
		
	}
	
}

