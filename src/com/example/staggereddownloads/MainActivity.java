package com.example.staggereddownloads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private NsdHelper mNsdHelper;
	private InetAddress mServerIp;

	private ServerSocket serverSocket;
	Thread serverThread = null;

	private Socket mClientSocket;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mNsdHelper = new NsdHelper(this);
		mNsdHelper.initializeNsd();

		Button registerBtn = (Button) findViewById(R.id.button1);
		registerBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mNsdHelper.registerService();
				serverThread = new Thread(new ServerThread());
				serverThread.start();
			}
		});

		Button listenBtn = (Button) findViewById(R.id.button2);
		listenBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mNsdHelper.discoverServices();
				new Thread(new Runnable() {

					@Override
					public void run() {
						while(!Thread.currentThread().isInterrupted()) {
							if (mNsdHelper.getChosenServiceInfo() != null) {
								mServerIp = mNsdHelper.getChosenServiceInfo().getHost();
								Log.e("hiemanshu", "Got Server IP : " + mServerIp.getHostAddress().toString());
								new Thread(new ClientThread()).start();
								return;
							}
						}
					}
				}).start();

			}
		});

		Button sendButton = (Button) findViewById(R.id.button3);
		sendButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream())), true);
					out.println("Hello from client!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		mNsdHelper.tearDown();
		super.onDestroy();
	}

	class ServerThread implements Runnable {
		public void run() {
			Socket socket = null;
			try {
				serverSocket = new ServerSocket(NsdHelper.SERVICE_PORT);
			} catch (Exception e) {
				e.printStackTrace();
			}
			while(!Thread.currentThread().isInterrupted()) {
				try {
					socket = serverSocket.accept();
					CommThread commThread = new CommThread(socket);
					new Thread(commThread).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	class CommThread implements Runnable {

		private Socket clientSocket;
		private BufferedReader input;
		private PrintWriter output;

		public CommThread(Socket clientScket) {
			clientSocket = clientScket;
			try {
				input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					final String read = input.readLine();
					Log.e("hiemanshu", read);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), read, Toast.LENGTH_SHORT).show();							
						}
					});
					output.println("Reply from Server with LINK!");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	class ClientThread implements Runnable {

		private BufferedReader input;

		@Override
		public void run() {
			Log.e("hiemanshu", "Client thread started!");
			try {
				mClientSocket = new Socket(mServerIp, NsdHelper.SERVICE_PORT);
				input = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			while (!Thread.currentThread().isInterrupted()) {
				final String read;
				try {
					read = input.readLine();
					Log.e("hiemanshu", read);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), read, Toast.LENGTH_SHORT).show();							
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
