package com.widemo.asyncsocket;

/**************************************************************************
 * AsyncSocketListener</br>
 * Author : isUseful ? TanJian : Unknown</br>
 * English by google translate.
 **************************************************************************/
public interface AsyncSocketListener
{
	/**
	 * Is called when the socket connected.
	 */
	void OnSocketConnected(AsyncSocket asyncSocket);

	/**
	 * Is called when the socket connection failed.
	 */
	void OnSocketConnectionFailed(AsyncSocket asyncSocket);

	/**
	 * Is called when the socket interruption.
	 */
	void OnSocketInterruption(AsyncSocket asyncSocket);

	/**
	 * Is called when receive data.
	 * 
	 * @param bytesReceived
	 *            receive byte array.
	 * @param bytes
	 *            the number of bytes that have been read.
	 */
	void OnSocketReceive(AsyncSocket asyncSocket, byte[] bytesReceived, int bytes);
}
