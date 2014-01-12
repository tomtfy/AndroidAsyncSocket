package com.widemo.asyncsocket;

/**************************************************************************

AsyncSocketListener

Author   :   isUseful ? TanJian : Unknown

English by google translate.

**************************************************************************/
public interface AsyncSocketListener
{
	/**
	 * Is called when the socket connected.
	 */
	void OnSocketConnected();

	/**
	 * Is called when the socket connection failed.
	 */
	void OnSocketConnectionFailed();

	/**
	 * Is called when the socket interruption.
	 */
	void OnSocketInterruption();

	void OnSocketReceive(String msg);
}
