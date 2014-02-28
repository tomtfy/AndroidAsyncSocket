package com.widemo.net.socket.data;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**************************************************************************
 * Stream</br>
 * Author : isUseful ? TanJian : Unknown</br>
 * English by google translate.
 **************************************************************************/
public class Stream
{
	public static final String	CHARSET_UTF8		= "UTF-8";
	public static final int		DEFAULT_EXPAND_SIZE	= 128;

	private ByteBuffer			buffer;
	private int					length				= 0;
	private boolean				debug				= false;

	public Stream()
	{
		this(false);
	}

	public Stream(boolean debug)
	{
		this.debug = debug;
		this.buffer = ByteBuffer.allocate(DEFAULT_EXPAND_SIZE);
		rewind();
	}

	public Stream(byte[] in)
	{
		this(in, false);
	}

	public Stream(byte[] in, boolean debug)
	{
		this.buffer = ByteBuffer.allocate(length + DEFAULT_EXPAND_SIZE);
		writeBytes(in);
		rewind();
	}

	private void expand(int length)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(length);
		byteBuffer.put(toBytes());
		int position = buffer.position();
		buffer = byteBuffer;
		buffer.position(position);
	}

	public String readString()
	{
		return readString(CHARSET_UTF8);
	}

	public String readString(String charset)
	{
		int len = readInt();

		if (len > 0)
		{
			byte[] stringData = new byte[len];
			readBytes(stringData);

			String result;
			try
			{
				result = new String(stringData, charset);
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
			if (length > remaining())
			{
				length = remaining();
			}
			buffer.get(data, offset, length);
			return length;
		}

		return 0;
	}

	public void writeByte(byte aValue)
	{
		if (remaining() < 1)
		{
			expand(length + 1 + DEFAULT_EXPAND_SIZE);
		}
		buffer.put(aValue);
		length++;
	}

	public void writeInt(int aValue)
	{
		if (remaining() < 4)
		{
			expand(length + 4 + DEFAULT_EXPAND_SIZE);
		}
		buffer.putInt(aValue);
		length += 4;
	}

	public void writeShort(short aValue)
	{
		if (remaining() < 2)
		{
			expand(length + 2 + DEFAULT_EXPAND_SIZE);
		}
		buffer.putShort(aValue);
		length += 2;
	}

	public void writeLong(long aValue)
	{
		if (remaining() < 8)
		{
			expand(length + 8 + DEFAULT_EXPAND_SIZE);
		}
		buffer.putLong(aValue);
		length += 8;
	}

	public void writeChar(char aValue)
	{
		if (remaining() < 2)
		{
			expand(length + 2 + DEFAULT_EXPAND_SIZE);
		}
		buffer.putChar(aValue);
		length += 2;
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
			if (remaining() < stringData.length)
			{
				expand(length + stringData.length + DEFAULT_EXPAND_SIZE);
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

	public void writeBytes(byte[] src, int srcOffset, int byteCount)
	{
		if (src == null) return;
		if (byteCount + srcOffset > src.length) byteCount = src.length - srcOffset;
		if (remaining() < byteCount)
		{
			expand(length + byteCount + DEFAULT_EXPAND_SIZE);
		}

		buffer.put(src, srcOffset, byteCount);
		length += byteCount;
	}

	public byte[] toBytes()
	{
		byte[] data = new byte[length];
		System.arraycopy(buffer.array(), 0, data, 0, length);
		return data;
	}

	public void rewind()
	{
		buffer.rewind();
	}

	public void clear()
	{
		buffer.clear();
		buffer = null;
		buffer = ByteBuffer.allocate(DEFAULT_EXPAND_SIZE);
		length = 0;
	}

	public void clear(int startIndex, int length)
	{
		int bufferLength = this.length;
		if (startIndex < 0)
		{
			startIndex = 0;
		}
		if (startIndex + length > bufferLength)
		{
			length = bufferLength - startIndex;
		}
		if (startIndex > bufferLength || length <= 0)
		{
			return;
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(bufferLength - length + DEFAULT_EXPAND_SIZE);
		byte[] data = toBytes();
		if (startIndex > 0)
		{
			byte[] startData = new byte[startIndex];
			System.arraycopy(data, 0, startData, 0, startIndex);
			byteBuffer.put(startData);
		}
		byte[] endData = new byte[bufferLength - length];
		System.arraycopy(data, startIndex + length, endData, 0, bufferLength - length - startIndex);
		byteBuffer.put(endData);
		buffer = byteBuffer;
		buffer.position(0);
		this.length -= length;
	}

	public int remaining()
	{
		return buffer.remaining();
	}

	public int size()
	{
		return length;
	}

	public int position()
	{
		return buffer.position();
	}

	public void position(int newPosition)
	{
		if (newPosition < 0 || newPosition > length)
		{
			return;
		}
		buffer.position(newPosition);
	}
}
