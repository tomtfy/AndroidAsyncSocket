package com.widemo.asyncsocket.data;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**************************************************************************
 * Stream</br>
 * Author : isUseful ? TanJian : Unknown</br>
 * English by google translate.
 **************************************************************************/
public class Stream
{
	public static final String	CHARSET_UTF8	= "UTF-8";

	private ByteBuffer			buffer;

	public Stream()
	{
		this.buffer = ByteBuffer.allocate(128);
		buffer.rewind();
	}

	public Stream(byte[] in)
	{
		this.buffer = ByteBuffer.allocate(in.length + 128);
		this.buffer.put(in, 0, in.length);
		buffer.rewind();
	}

	private void expand(int length)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(length);
		byteBuffer.put(buffer.array());
		int position = buffer.position();
		buffer = byteBuffer;
		buffer.position(position);
	}

	public String readString()
	{
		int len = readInt();

		if (len > 0)
		{
			byte[] stringData = new byte[len];
			readBytes(stringData);

			String result;
			try
			{
				result = new String(stringData, CHARSET_UTF8);
			}
			catch (UnsupportedEncodingException e)
			{
				result = null;
			}
			return result;
		}
		else if (len == 0)
		{
			return new String();
		}
		else
		{
			return null;
		}
	}

	public int readInt()
	{
		return buffer.getInt();
	}

	public char readChar()
	{
		return buffer.getChar();
	}

	public short readShort()
	{
		return buffer.getShort();
	}

	public long readLong()
	{
		return buffer.getLong();
	}

	public byte readByte()
	{
		return buffer.get();
	}

	public int readBytes(byte[] data)
	{
		return readBytes(data, 0, data.length);
	}

	public int readBytes(byte[] data, int offset, int length)
	{
		if (data != null)
		{
			if (length > buffer.remaining())
			{
				length = buffer.remaining();
			}
			buffer.get(data, offset, length);
			return length;
		}

		return 0;
	}

	public void writeByte(byte aValue)
	{
		if (buffer.remaining() < 1)
		{
			expand(buffer.array().length + 1 + 128);
		}
		buffer.put(aValue);
	}

	public void writeInt(int aValue)
	{
		if (buffer.remaining() < 4)
		{
			expand(buffer.array().length + 4 + 128);
		}
		buffer.putInt(aValue);
	}

	public void writeShort(short aValue)
	{
		if (buffer.remaining() < 2)
		{
			expand(buffer.array().length + 2 + 128);
		}
		buffer.putShort(aValue);
	}

	public void writeLong(long aValue)
	{
		if (buffer.remaining() < 8)
		{
			expand(buffer.array().length + 8 + 128);
		}
		buffer.putLong(aValue);
	}

	public void writeChar(char aValue)
	{
		if (buffer.remaining() < 2)
		{
			expand(buffer.array().length + 2 + 128);
		}
		buffer.putChar(aValue);
	}

	public void writeString(String aValue)
	{
		writeString(aValue, CHARSET_UTF8);
	}

	public void writeString(String aValue, String charset)
	{
		int len = aValue.length();
		try
		{
			byte[] stringData = aValue.getBytes(charset);
			writeInt(len);
			if (buffer.remaining() < stringData.length)
			{
				expand(buffer.array().length + stringData.length + 128);
			}
			writeBytes(stringData);
		}
		catch (UnsupportedEncodingException e)
		{
			writeInt(0);
		}
	}

	public void writeBytes(byte[] aValue)
	{
		if (aValue == null) return;
		writeBytes(aValue, 0, aValue.length);
	}

	public void writeBytes(byte[] aValue, int aFrom, int aLength)
	{
		if (aValue == null) return;
		if (aLength + aFrom > aValue.length) aLength = aValue.length - aFrom;
		if (buffer.remaining() < aLength)
		{
			expand(buffer.array().length + aLength + 128);
		}

		buffer.put(aValue, aFrom, aLength);
	}

	public byte[] toBytes()
	{
		return this.buffer.array();
	}
}
