package com.widemo.asyncsocket;

/**************************************************************************
 * AsyncSocketListener
 * Author : isUseful ? TanJian : Unknown
 * English by google translate.
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

	/**
	 * Is called when receive data.
	 * 
	 * @param bytesReceived
	 *            receive byte array.
	 * @param bytes
	 *            the number of bytes that have been read.
	 */
	void OnSocketReceive(byte[] bytesReceived, int bytes);
}
