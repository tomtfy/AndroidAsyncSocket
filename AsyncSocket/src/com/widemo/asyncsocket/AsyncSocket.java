package com.widemo.asyncsocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

/**************************************************************************
 * AsyncSocketClient
 * Author : isUseful ? TanJian : Unknown
 * English by google translate.
 **************************************************************************/
public class AsyncSocket
{

	private static final String			TAG							= "AsyncSocketClient";

	private static final int			RECEIVED_BYTES_SIZE			= 1024;

	private static final int			SOCKET_WHAT_CONNECTED		= 0;
	private static final int			SOCKET_WHAT_FAILED			= SOCKET_WHAT_CONNECTED + 1;
	private static final int			SOCKET_WHAT_INTERRUPTION	= SOCKET_WHAT_FAILED + 1;
	private static final int			SOCKET_WHAT_RECEIVE			= SOCKET_WHAT_INTERRUPTION + 1;

	public static final String			SOCKET_ENCODE_UTF8			= "UTF-8";

	/**
	 * Socket Not Connected
	 */
	public static final int				SOCKET_STATE_NOTCONNECTED	= 0;

	/**
	 * Socket Connecting
	 */
	public static final int				SOCKET_STATE_CONNECTING		= SOCKET_STATE_NOTCONNECTED + 1;

	/**
	 * Socket Connected
	 */
	public static final int				SOCKET_STATE_CONNECTED		= SOCKET_STATE_CONNECTING + 1;

	private final boolean				_debug;
	private final String				_serverIP;
	private final int					_serverProt;
	private final SocketConnectRunnable	_socketRunnable;
	private final SocketConnectHandler	_socketHandler;
	private final AsyncSocketListener	_socketListener;

	private boolean						_working					= false;
	private int							_state						= SOCKET_STATE_NOTCONNECTED;
	private int							_timeout					= 30 * 1000;
	private String						_encode						= SOCKET_ENCODE_UTF8;
	private Socket						_socket						= null;
	private DataInputStream				_socketReader				= null;
	private DataOutputStream			_socketWriter				= null;
	private Executor					_executor					= null;

	/**
	 * Instantiate AsyncSocketClient with IP and port, log off.
	 * 
	 * @param ip
	 * @param prot
	 */
	public AsyncSocket(String ip, int prot, AsyncSocketListener listener)
	{
		this(ip, prot, listener, false);
	}

	/**
	 * Instantiate AsyncSocketClient with IP, port and log switch.
	 * 
	 * @param ip
	 * @param prot
	 * @param encode
	 * @param debug
	 */
	public AsyncSocket(String ip, int prot, AsyncSocketListener listener, boolean debug)
	{
		_debug = debug;
		_serverIP = ip;
		_serverProt = prot;
		_socketListener = listener;
		_socketHandler = new SocketConnectHandler();
		_socketRunnable = new SocketConnectRunnable(_socketHandler);
		_executor = Executors.newCachedThreadPool();
		logInfo(String.format("Connect to %1$s:%2$d", _serverIP, _serverProt));
	}

	/**
	 * Set socket timeout, If socket is already connected, set the next time you
	 * connect.
	 * 
	 * @param timeout
	 */
	public void setTimeOut(int timeout)
	{
		_timeout = timeout;
	}

	/**
	 * Set socket stream character encoding, If socket is already connected, set
	 * the next time you connect.
	 * 
	 * @param encoder
	 */
	public void setEncoder(String encoder)
	{
		_encode = encoder;
	}

	/**
	 * Current socket state.
	 * 
	 * @return
	 */
	public int getState()
	{
		return _state;
	}

	/**
	 * Send data to server
	 * 
	 * @param data
	 */
	public void send(byte[] data)
	{
		if (_state != SOCKET_STATE_CONNECTED || data == null || data.length <= 0)
		{
			logWarn("CAN'T send message because socket not connected!");
			return;
		}
		try
		{
			_socketWriter.write(data);
			_socketWriter.flush();
			logInfo("Send Data");
		}
		catch (IOException e)
		{
			logError(e.getMessage());
		}
	}

	public synchronized void close()
	{
		if (_state != SOCKET_STATE_CONNECTING)
		{
			return;
		}
		_state = SOCKET_STATE_NOTCONNECTED;
		_working = false;
	}

	public synchronized void connect()
	{
		if (_state != SOCKET_STATE_NOTCONNECTED)
		{
			return;
		}
		_state = SOCKET_STATE_CONNECTING;
		_executor.execute(_socketRunnable);
	}

	private void logInfo(String msg)
	{
		if (_debug)
		{
			Log.i(TAG, msg);
		}
	}

	private void logWarn(String msg)
	{
		if (_debug)
		{
			Log.w(TAG, msg);
		}
	}

	private void logError(String msg)
	{
		if (_debug)
		{
			Log.e(TAG, msg);
		}
	}

	private class SocketConnectHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			int what = msg.what;
			switch (what)
			{
				case SOCKET_WHAT_CONNECTED:
					_state = SOCKET_STATE_CONNECTED;
					if (_socketListener != null)
					{
						_socketListener.OnSocketConnected();
					}
					break;
				case SOCKET_WHAT_FAILED:
					_state = SOCKET_STATE_NOTCONNECTED;
					if (_socketListener != null)
					{
						_socketListener.OnSocketConnectionFailed();
					}
					break;
				case SOCKET_WHAT_INTERRUPTION:
					_state = SOCKET_STATE_NOTCONNECTED;
					if (_socketListener != null)
					{
						_socketListener.OnSocketInterruption();
					}
					break;
				case SOCKET_WHAT_RECEIVE:
					byte[] bytesReceived = (byte[]) msg.obj;
					if (_socketListener != null)
					{
						_socketListener.OnSocketReceive(bytesReceived);
					}
			}
		}
	}

	private class SocketConnectRunnable implements Runnable
	{
		private final Handler	_handler;

		public SocketConnectRunnable(Handler handler)
		{
			_handler = handler;
		}

		private void clean()
		{
			_working = false;
			try
			{
				if (_socketWriter != null)
				{
					_socketWriter.close();
				}
			}
			catch (Exception e)
			{
			}
			_socketWriter = null;
			try
			{
				if (_socketReader != null)
				{
					_socketReader.close();
				}
			}
			catch (Exception e)
			{
			}
			_socketReader = null;
			try
			{
				if (_socket != null)
				{
					_socket.close();
				}
			}
			catch (Exception e)
			{
			}
			_socket = null;
		}

		private void sendMessage(int what, Object obj)
		{
			Message msg = _handler.obtainMessage(what, obj);
			_handler.sendMessage(msg);
		}

		@Override
		public void run()
		{
			try
			{
				if (_socket == null)
				{
					_socket = new Socket();
				}

				InetSocketAddress isa = new InetSocketAddress(_serverIP, _serverProt);
				_socket.connect(isa, _timeout);

				InputStream is = _socket.getInputStream();
				_socketReader = new DataInputStream(is);

				OutputStream os = _socket.getOutputStream();
				_socketWriter = new DataOutputStream(os);
			}
			catch (Exception e)
			{
				logError(e.getMessage());
				clean();
				sendMessage(SOCKET_WHAT_FAILED, null);
				return;
			}
			_working = true;
			byte[] bytesReceived = new byte[RECEIVED_BYTES_SIZE];
			int bytes = 0;
			sendMessage(SOCKET_WHAT_CONNECTED, null);
			while (_working)
			{
				try
				{
					bytes = _socketReader.read(bytesReceived, 0, bytesReceived.length);
					if (bytes > 0)
					{
						logInfo(String.format("Receive Message, Data size = %1$d", bytes));
						sendMessage(SOCKET_WHAT_RECEIVE, bytesReceived);
					}
				}
				catch (IOException e)
				{
					logError(e.getMessage());
					clean();
					sendMessage(SOCKET_WHAT_INTERRUPTION, null);
					return;
				}
			}
			clean();
		}
	}
}
