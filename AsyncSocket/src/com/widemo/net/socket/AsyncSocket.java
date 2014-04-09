package com.widemo.net.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.widemo.net.socket.data.SocketState;

/**************************************************************************
 * AsyncSocketClient</br>
 * Author : isUseful ? TanJian : Unknown</br>
 * English by google translate.
 **************************************************************************/
public class AsyncSocket
{

	public static final int		CALLBACK_THREAD_MAIN		= 0;
	public static final int		CALLBACK_THREAD_BACKGROUND	= 1;

	private static final String	TAG							= "AsyncSocketClient";
	private static final int	RECEIVED_BYTES_SIZE			= 1024;				// Receive byte array length

	private enum SocketWhat
	{
		CONNECTED, FAILED, INTERRUPTION, RECEIVE
	}

	private final boolean				_debug;
	private final SocketConnectHandler	_socketHandler;
	private final AsyncSocketListener	_socketListener;

	private int							_socketID;
	private String						_serverHost;
	private int							_serverPort;
	private boolean						_working		= false;
	private SocketState					_state			= SocketState.NOTCONNECTED;
	private int							_timeout		= 30 * 1000;
	private int							_callbackThread	= CALLBACK_THREAD_BACKGROUND;
	private Socket						_socket			= null;
	private DataInputStream				_socketReader	= null;
	private DataOutputStream			_socketWriter	= null;
	private ExecutorService				_executor		= null;
	private SocketConnectRunnable		_socketConnectThread;

	/**
	 * Instantiate AsyncSocketClient with IP and port, log off.
	 * 
	 * @param ip
	 * @param port
	 * @param listener
	 */
	public AsyncSocket(AsyncSocketListener listener)
	{
		this(listener, false);
	}

	/**
	 * Instantiate AsyncSocket with IP, port and log switch.
	 * 
	 * @param ip
	 * @param port
	 * @param listener
	 * @param debug
	 */
	public AsyncSocket(AsyncSocketListener listener, boolean debug)
	{
		_socketID = 1;
		_debug = debug;
		_socketListener = listener;
		_socketHandler = new SocketConnectHandler();
		_executor = Executors.newCachedThreadPool();
		logInfo(String.format("Connect to %1$s:%2$d", _serverHost, _serverPort));
	}

	/**
	 * Set socket server host, if socket is already connected, set the next time you connect.
	 * 
	 * @param host
	 */
	public void setServerHost(String host)
	{
		if (TextUtils.isEmpty(host))
		{
			throw new NullPointerException("Host CAN NOT be null");
		}
		logInfo("Set server host to \"" + host + "\"");
		_serverHost = host;
	}

	/**
	 * Set socket server port, if socket is already connected, set the next time you connect.
	 * 
	 * @param port
	 */
	public void setServerPort(int port)
	{
		if (port < 0 || port > 65535)
		{
			throw new NullPointerException("Error port");
		}
		logInfo("Set server port to \"" + port + "\"");
		_serverPort = port;
	}

	/**
	 * Set AsyncSocket instance ID, used only to distinguish objects, not as a connection parameter.
	 * 
	 * @param id
	 */
	public void setID(int id)
	{
		logInfo("Set AsyncSocket ID to \"" + id + "\"");
		_socketID = id;
	}

	/**
	 * Set socket timeout, if socket is already connected, set the next time you connect.
	 * 
	 * @param timeout
	 *            the timeout value in milliseconds or 0 for an infinite timeout.
	 */
	public void setTimeOut(int timeout)
	{
		logInfo("Set timeout to " + timeout + "ms");
		_timeout = timeout;
	}

	/**
	 * Set socket callback thread, if set the callback thread to CALLBACK_THREAD_BACKGROUND, the socket will use its own thread notice, so if you do not accidentally cause the thread to crash, congratulations, Bug be with you.
	 * 
	 * @param threadType
	 *            CALLBACK_THREAD_BACKGROUND or CALLBACK_THREAD_MAIN
	 */
	public void setCallbackThread(int threadType)
	{
		if (threadType != CALLBACK_THREAD_BACKGROUND && threadType != CALLBACK_THREAD_MAIN)
		{
			throw new IllegalArgumentException("Thread type error!");
		}
		_callbackThread = threadType;
	}

	/**
	 * Current socket callback thread type.
	 * 
	 * @return CALLBACK_THREAD_BACKGROUND or CALLBACK_THREAD_MAIN
	 */
	public int getCallbackThread()
	{
		return _callbackThread;
	}

	/**
	 * Current socket state.
	 * 
	 * @return
	 */
	public SocketState getState()
	{
		return _state;
	}

	/**
	 * Get AsyncSocket ID
	 * 
	 * @return
	 */
	public int getID()
	{
		return _socketID;
	}

	/**
	 * Send data to server.
	 * 
	 * @param data
	 */
	public boolean send(byte[] data)
	{
		if (_state != SocketState.CONNECTED || data == null || data.length <= 0)
		{
			logWarn("CAN'T send message because socket not connected!");
			return false;
		}
		try
		{
			_socketWriter.write(data);
			_socketWriter.flush();
			logInfo("Send data, length = " + data.length);
		}
		catch (IOException e)
		{
			logError(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Close connected socket, and notify interruption message to listener on socket closed.
	 */
	public synchronized void close()
	{
		if (_state == SocketState.NOTCONNECTED)
		{
			return;
		}
		_state = SocketState.NOTCONNECTED;
		_working = false;
		_socketConnectThread.interrupt();

	}

	/**
	 * Connect socket server, and notify result to listener
	 */
	public synchronized void connect()
	{
		if (_state != SocketState.NOTCONNECTED)
		{
			return;
		}
		if (TextUtils.isEmpty(_serverHost) || _serverPort <= 0)
		{
			throw new NullPointerException("Error IP address or port");
		}
		_state = SocketState.CONNECTING;
		_socketConnectThread = new SocketConnectRunnable(_socketHandler);
		_socketConnectThread.setPriority(Thread.NORM_PRIORITY);
		_socketConnectThread.setName(TAG);
		_executor.execute(_socketConnectThread);
	}

	private void logInfo(String msg)
	{
		if (_debug && !TextUtils.isEmpty(msg))
		{
			Log.i(TAG, msg);
		}
	}

	private void logWarn(String msg)
	{
		if (_debug && !TextUtils.isEmpty(msg))
		{
			Log.w(TAG, msg);
		}
	}

	private void logError(String msg)
	{
		if (_debug && !TextUtils.isEmpty(msg))
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
			logInfo(String.format("Handler Message: ", msg.what));
			switch (what)
			{
				case CONNECTED:
					_state = SocketState.CONNECTED;
					if (_socketListener != null)
					{
						logInfo(String.format("Send connected notify to listener."));
						_socketListener.onSocketConnected(AsyncSocket.this);
					}
					break;
				case FAILED:
					_state = SocketState.NOTCONNECTED;
					if (_socketListener != null)
					{
						logInfo(String.format("Send connection failed notify to listener."));
						_socketListener.onSocketConnectionFailed(AsyncSocket.this);
					}
					break;
				case INTERRUPTION:
					_state = SocketState.NOTCONNECTED;
					if (_socketListener != null)
					{
						logInfo(String.format("Send interruption notify to listener."));
						_socketListener.onSocketInterruption(AsyncSocket.this);
					}
					break;
				case RECEIVE:
					byte[] bytesReceived = (byte[]) msg.obj;
					int bytes = msg.arg1;
					if (_socketListener != null)
					{
						logInfo(String.format("Send receive notify to listener."));
						_socketListener.onSocketReceive(AsyncSocket.this, bytesReceived, bytes);
					}
					break;
			}
		}
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

	private class SocketConnectRunnable extends Thread
	{
		private final Handler	_handler;

		public SocketConnectRunnable(Handler handler)
		{
			_handler = handler;
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

				InetSocketAddress isa = new InetSocketAddress(_serverHost, _serverPort);
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

			int bytes = 0;
			sendMessage(SocketWhat.CONNECTED);
			byte[] bytesReceived = new byte[RECEIVED_BYTES_SIZE];
			while (_working && !interrupted())
			{
				try
				{

					bytes = _socketReader.read(bytesReceived, 0, bytesReceived.length);
					if (bytes > 0)
					{
						byte[] data = new byte[bytes];
						System.arraycopy(bytesReceived, 0, data, 0, bytes);
						logInfo(String.format("Receive Message, Data size = %1$d", bytes));
						if (_callbackThread == CALLBACK_THREAD_MAIN)
						{
							sendMessage(SocketWhat.RECEIVE, bytes, 0, data);
						}
						else
						{
							if (_socketListener != null)
							{
								logInfo(String.format("Send receive notify to listener."));
								_socketListener.onSocketReceive(AsyncSocket.this, data, bytes);
							}
						}
					}
				}
				catch (IOException e)
				{
					logError(e.getMessage());
					break;
				}
			}
			clean();
			sendMessage(SocketWhat.INTERRUPTION);
		}
	}
}
