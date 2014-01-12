package com.widemo.asyncsocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

/**************************************************************************

AsyncSocketClient

Author   :   isUseful ? TanJian : Unknown

English by google translate.

**************************************************************************/
public class AsyncSocketClient
{

	private static final String			TAG							= "AsyncSocketClient";
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
	private BufferedReader				_socketReader				= null;
	private PrintWriter					_socketWriter				= null;
	private Executor					_executor					= null;

	/**
	 * Instantiate AsyncSocketClient with ip and prot, log off.
	 * @param ip
	 * @param prot
	 */
	public AsyncSocketClient(String ip, int prot, AsyncSocketListener listener)
	{
		this(ip, prot, listener, false);
	}

	/**
	 * Instantiate AsyncSocketClient with ip, prot and log switch.
	 * @param ip
	 * @param prot
	 * @param encode
	 * @param debug
	 */
	public AsyncSocketClient(String ip, int prot, AsyncSocketListener listener, boolean debug)
	{
		_debug = debug;
		_serverIP = ip;
		_serverProt = prot;
		_socketListener = listener;
		_socketHandler = new SocketConnectHandler();
		_socketRunnable = new SocketConnectRunnable(_socketHandler);
		_executor = Executors.newCachedThreadPool();
		logInfo(String.format("Connect to %1$s:%2&d", _serverIP, _serverProt));
	}

	/**
	 * Set socket timeout, If socket is already connected, set the next time you connect.
	 * @param timeout
	 */
	public void setTimeOut(int timeout)
	{
		_timeout = timeout;
	}

	/**
	 * Set socket stream character encoding, If socket is already connected, set the next time you connect.
	 * @param encoder
	 */
	public void setEncoder(String encoder)
	{
		_encode = encoder;
	}

	/**
	 * Current socket state.
	 * @return
	 */
	public int getState()
	{
		return _state;
	}

	public synchronized void closeSocket()
	{
		if (_state != SOCKET_STATE_CONNECTING)
		{
			return;
		}
		_working = false;
	}

	public synchronized void connectSocket()
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
					String receiveMsg = (String) msg.obj;
					if (_socketListener != null)
					{
						_socketListener.OnSocketReceive(receiveMsg);
					}
			}
		}
	}

	private class SocketConnectRunnable implements Runnable
	{
		private InputStreamReader	_isr;
		private OutputStreamWriter	_osw;
		private BufferedWriter		_bw;

		private final Handler		_handler;

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
				if (_bw != null)
				{
					_bw.close();
				}
			}
			catch (Exception e)
			{
			}
			_bw = null;
			try
			{
				if (_osw != null)
				{
					_osw.close();
				}
			}
			catch (Exception e)
			{
			}
			_osw = null;
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
				if (_isr != null)
				{
					_isr.close();
				}
			}
			catch (Exception e)
			{
			}
			_isr = null;
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

				_isr = new InputStreamReader(_socket.getInputStream(), _encode);
				_socketReader = new BufferedReader(_isr);

				_osw = new OutputStreamWriter(_socket.getOutputStream(), _encode);
				_bw = new BufferedWriter(_osw);
				_socketWriter = new PrintWriter(_bw, true);
			}
			catch (Exception e)
			{
				logError(e.getMessage());
				clean();
				sendMessage(SOCKET_WHAT_FAILED, null);
				return;
			}
			_working = true;
			sendMessage(SOCKET_WHAT_CONNECTED, null);
			while (_working)
			{
				try
				{
					String msg = _socketReader.readLine();
					if (!TextUtils.isEmpty(msg))
					{
						sendMessage(SOCKET_WHAT_RECEIVE, msg);
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
