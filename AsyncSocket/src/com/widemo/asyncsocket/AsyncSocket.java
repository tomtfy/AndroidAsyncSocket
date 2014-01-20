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
import android.util.Log;

import com.widemo.asyncsocket.data.SocketState;

/**************************************************************************
 * AsyncSocketClient</br>
 * Author : isUseful ? TanJian : Unknown</br>
 * English by google translate.
 **************************************************************************/
public class AsyncSocket
{

	private static final String	TAG					= "AsyncSocketClient";

	private static final int	RECEIVED_BYTES_SIZE	= 1024;				// Receive byte array length

	private enum SocketWhat
	{
		CONNECTED, FAILED, INTERRUPTION, RECEIVE
	}

	private final boolean				_debug;
	private final String				_serverIP;
	private final int					_serverProt;
	private final SocketConnectRunnable	_socketRunnable;
	private final SocketConnectHandler	_socketHandler;
	private final AsyncSocketListener	_socketListener;

	private int							_socketID;
	private boolean						_working		= false;
	private SocketState					_state			= SocketState.NOTCONNECTED;
	private int							_timeout		= 30 * 1000;
	private Socket						_socket			= null;
	private DataInputStream				_socketReader	= null;
	private DataOutputStream			_socketWriter	= null;
	private Executor					_executor		= null;

	/**
	 * 使用指定IP、端口号实例化AsyncSocket，日志开关默认关闭
	 * Instantiate AsyncSocketClient with IP and port, log off.
	 * 
	 * @param ip
	 * @param prot
	 * @param listener
	 */
	public AsyncSocket(String ip, int prot, AsyncSocketListener listener)
	{
		this(ip, prot, listener, false);
	}

	/**
	 * 使用指定IP、端口号及日志开关，实例化AsyncSocket
	 * Instantiate AsyncSocket with IP, port and log switch.
	 * 
	 * @param ip
	 * @param prot
	 * @param listener
	 * @param debug
	 */
	public AsyncSocket(String ip, int prot, AsyncSocketListener listener, boolean debug)
	{
		_socketID = 1;
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
	 * 设置AsyncSocket实例ID，仅用于区分对象，不作为连接参数
	 * 
	 * @param id
	 */
	public void setID(int id)
	{
		_socketID = id;
	}

	/**
	 * 设置Socket连接超时时间，如果Socket已经连接，则在断开后下次连接时生效
	 * Set socket timeout, If socket is already connected, set the next time you
	 * connect.
	 * 
	 * @param timeout
	 *            the timeout value in milliseconds or 0 for an infinite timeout.
	 */
	public void setTimeOut(int timeout)
	{
		_timeout = timeout;
	}

	/**
	 * 获取当前Socket连接状态
	 * Current socket state.
	 * 
	 * @return
	 */
	public SocketState getState()
	{
		return _state;
	}

	/**
	 * 获取AsyncSocket实例ID
	 * 
	 * @return
	 */
	public int getID()
	{
		return _socketID;
	}

	/**
	 * 发送数据到服务器
	 * Send data to server
	 * 
	 * @param data
	 */
	public void send(byte[] data)
	{
		if (_state != SocketState.CONNECTED || data == null || data.length <= 0)
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
		if (_state != SocketState.CONNECTING)
		{
			return;
		}
		_state = SocketState.NOTCONNECTED;
		_working = false;
	}

	public synchronized void connect()
	{
		if (_state != SocketState.NOTCONNECTED)
		{
			return;
		}
		_state = SocketState.CONNECTING;
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

			SocketWhat what = SocketWhat.values()[msg.what];
			switch (what)
			{
				case CONNECTED:
					_state = SocketState.CONNECTED;
					if (_socketListener != null)
					{
						_socketListener.OnSocketConnected(AsyncSocket.this);
					}
					break;
				case FAILED:
					_state = SocketState.NOTCONNECTED;
					if (_socketListener != null)
					{
						_socketListener.OnSocketConnectionFailed(AsyncSocket.this);
					}
					break;
				case INTERRUPTION:
					_state = SocketState.NOTCONNECTED;
					if (_socketListener != null)
					{
						_socketListener.OnSocketInterruption(AsyncSocket.this);
					}
					break;
				case RECEIVE:
					byte[] bytesReceived = (byte[]) msg.obj;
					int bytes = msg.arg1;
					if (_socketListener != null)
					{
						_socketListener.OnSocketReceive(AsyncSocket.this, bytesReceived, bytes);
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

		private void sendMessage(SocketWhat what, int arg1, int arg2, Object obj)
		{
			Message msg = _handler.obtainMessage(what.ordinal(), arg1, arg2, obj);
			_handler.sendMessage(msg);
		}

		private void sendMessage(SocketWhat what)
		{
			_handler.sendEmptyMessage(what.ordinal());
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
				sendMessage(SocketWhat.FAILED);
				return;
			}
			_working = true;
			byte[] bytesReceived = new byte[RECEIVED_BYTES_SIZE];
			int bytes = 0;
			sendMessage(SocketWhat.CONNECTED);
			while (_working)
			{
				try
				{
					bytes = _socketReader.read(bytesReceived, 0, bytesReceived.length);
					if (bytes > 0)
					{
						logInfo(String.format("Receive Message, Data size = %1$d", bytes));
						sendMessage(SocketWhat.RECEIVE, bytes, 0, bytesReceived);
					}
				}
				catch (IOException e)
				{
					logError(e.getMessage());
					clean();
					sendMessage(SocketWhat.INTERRUPTION);
					return;
				}
			}
			clean();
		}
	}
}
