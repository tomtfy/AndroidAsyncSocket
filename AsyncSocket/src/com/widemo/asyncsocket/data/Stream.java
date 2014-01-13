package com.widemo.asyncsocket.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Stream
{
	volatile int	size;
	volatile int	readpos;
	volatile int	writepos;
	byte[]			buffer;

	public Stream()
	{
		this.buffer = new byte[128];
		this.size = 0;
		this.readpos = 0;
		this.writepos = 0;
	}

	public Stream(byte[] in)
	{
		this.buffer = new byte[in.length];
		System.arraycopy(in, 0, this.buffer, 0, in.length);
		this.size = this.buffer.length;
		this.readpos = 0;
		this.writepos = 0;
	}

	public void resetRead()
	{
		this.readpos = 0;
	}

	public void resetWrite()
	{
		this.writepos = 0;
	}

	public int read()
	{
		if (this.readpos >= this.size) return -1;

		if (this.readpos < this.size) return this.buffer[(this.readpos++)];

		return -1;
	}

	public String readString()
	{
		int len = readInt();

		if (len > 0)
		{

			char[] chars = new char[len];
			for (int i = 0; i < len; i++)
			{
				chars[i] = readChar();
			}
			String result = new String(chars);
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
		int result = (int) _readData(4);
		return result;
	}

	public char readChar()
	{
		return (char) (int) _readData(2);
	}

	public short readShort()
	{
		short result = (short) (int) _readData(2);
		return result;
	}

	public long readLong()
	{
		long result = ((int) _readData(4) & 0xFFFFFFFFL) + (_readData(4) << 32);
		return result;
	}

	private long _readData(int aLength)
	{
		int av = available();
		if (aLength > av) return -1L;

		if (aLength > 8) return -1L;
		long ret = 3505670057818587136L;
		int handleTemp = (aLength - 1) * 8;
		int tmp = 0;
		while (tmp < aLength * 8)
		{
			ret |= (read() & 0xFF) << (handleTemp - tmp);
			tmp += 8;
		}
		return ret;
	}

	public void writeInt(int aValue)
	{
		_writeData(aValue, 4);
	}

	public void writeShort(short aValue)
	{
		_writeData(aValue, 2);
	}

	public void writeLong(long aValue)
	{
		_writeData(aValue, 8);
	}

	private void _writeData(long aValue, int aLength)
	{
		if (this.writepos + aLength > this.buffer.length) _expand(this.writepos + aLength + 100);

		int tmp = 0;
		int handleTemp = (aLength - 1) * 8;
		while (tmp < aLength * 8)
		{
			this.buffer[(this.writepos++)] = (byte) (aValue >> (handleTemp - tmp) & 0xFF);
			tmp += 8;
		}
		this.size += aLength;
	}

	public void write(byte aValue)
	{
		if (this.writepos + 1 > this.buffer.length) _expand(this.buffer.length + 100);

		this.buffer[this.writepos] = aValue;
		this.writepos += 1;
		this.size += 1;
	}

	public void writeObject(Object obj)
	{
		int i;
		if (obj == null) return;
		if (obj instanceof Object[])
		{
			Object[] tmp = (Object[]) obj;
			writeInt(tmp.length);
			for (i = 0; i < tmp.length; ++i)
				writeObject(tmp[i]);

			return;
		}
		if (obj instanceof byte[])
		{
			byte[] tmp = (byte[]) obj;
			writeInt(tmp.length);
			write(tmp);
			return;
		}
		if (obj instanceof long[])
		{
			long[] tmp = (long[]) obj;
			writeLong(tmp.length);
			for (i = 0; i < tmp.length; ++i)
				writeLong(tmp[i]);

			return;
		}
		if (obj instanceof String)
		{
			String tmp = (String) obj;
			char[] data = tmp.toCharArray();
			writeInt(data.length);
			for (i = 0; i < data.length; ++i)
				writeShort((short) data[i]);

			return;
		}
		if (obj instanceof char[])
		{
			char[] data = (char[]) obj;
			writeInt(data.length);
			for (i = 0; i < data.length; ++i)
				writeShort((short) data[i]);

			return;
		}
		if (obj instanceof int[])
		{
			int[] data = (int[]) obj;
			writeInt(data.length);
			for (i = 0; i < data.length; ++i)
				writeInt(data[i]);

			return;
		}
		if (obj instanceof short[])
		{
			short[] data = (short[]) obj;
			writeInt(data.length);
			for (i = 0; i < data.length; ++i)
				writeShort(data[i]);

			return;
		}
	}

	public void write(byte[] aValue)
	{
		if (aValue == null) return;
		write(aValue, 0, aValue.length);
	}

	public void write(byte[] aValue, int aFrom, int aLength)
	{
		if (aValue == null) return;
		if (aLength + aFrom > aValue.length) aLength = aValue.length - aFrom;

		if (this.writepos + aLength > this.buffer.length) _expand(this.writepos + aLength + 100);

		int tmp = 0;
		while (tmp < aLength)
		{
			this.buffer[(this.writepos++)] = aValue[(aFrom + tmp)];
			++tmp;
		}
		this.size += aLength;
	}

	private void _expand(int aSize)
	{
		byte[] newdata = new byte[aSize];
		if (this.buffer != null) System.arraycopy(this.buffer, 0, newdata, 0, this.buffer.length);
		this.buffer = null;
		this.buffer = newdata;
	}

	public byte[] getBytesM()
	{
		byte[] data = new byte[this.size];
		System.arraycopy(this.buffer, 0, data, 0, this.size);
		return data;
	}

	public byte readByte()
	{
		byte result = (byte) _readData(1);
		return result;
	}

	public int readBytes(byte[] data)
	{
		return readBytes(data, 0, data.length);
	}

	public int readBytes(byte[] data, int offset, int length)
	{
		if (data != null)
		{
			if (this.readpos >= this.buffer.length)
			{
				return 0;
			}
			int l = length;
			int av = available();
			if (l > av) l = av;

			System.arraycopy(this.buffer, this.readpos, data, offset, l);
			this.readpos += l;
			return l;
		}
		return 0;
	}

	public int size()
	{
		return this.size;
	}

	public int available()
	{
		return (this.size - this.readpos);
	}

	public int getReadPos()
	{
		return this.readpos;
	}

	public int getWritePos()
	{
		return this.writepos;
	}

	public void writeByte(int aValue) throws IOException
	{
		_writeData(aValue & 0xFF, 1);
	}

	public static void writeByte(int data, OutputStream out) throws IOException
	{
		out.write(data & 0xFF);
	}

	public static void writeBytes(byte[] data, OutputStream out) throws IOException
	{
		out.write(data);
	}

	public void writeBytes(byte[] data)
	{
		if (data == null) return;
		_expand(this.writepos + data.length + 100);
		System.arraycopy(data, 0, this.buffer, this.writepos, data.length);
		this.writepos += data.length;
		this.size += data.length;
	}

	public void replaceInt(int from, int newint)
	{
		byte[] intbyte = new byte[4];
		intbyte[3] = (byte) (newint >> 24 & 0xFF);
		intbyte[2] = (byte) (newint >> 16 & 0xFF);
		intbyte[1] = (byte) (newint >> 8 & 0xFF);
		intbyte[0] = (byte) (newint & 0xFF);
		replace(from, 4, intbyte, 0, 4);
	}

	public void replaceShort(int from, short newshort)
	{
		byte[] intbyte = new byte[2];
		intbyte[1] = (byte) (newshort >> 8 & 0xFF);
		intbyte[0] = (byte) (newshort & 0xFF);
		replace(from, 2, intbyte, 0, 2);
	}

	public void replaceByte(int from, byte newbyte)
	{
		this.buffer[from] = newbyte;
	}

	public void replace(int from, int length, byte[] data, int dataOffset, int dataLength)
	{
		if (data == null) return;
		if (dataOffset + dataLength > data.length) return;
		int sizeoffset = dataLength - length;
		if (sizeoffset > 0)
		{
			byte[] tmp = new byte[this.size + sizeoffset + 100];
			System.arraycopy(this.buffer, 0, tmp, 0, from);
			System.arraycopy(this.buffer, from + length, tmp, from + dataLength, this.size - from + length);
			System.arraycopy(data, dataOffset, tmp, from, dataLength);
			this.buffer = null;
			this.buffer = tmp;
		}
		else
		{
			System.arraycopy(this.buffer, from + length, this.buffer, from + dataLength, this.size - from + length);
			System.arraycopy(data, dataOffset, this.buffer, from, dataLength);
		}
		this.size += sizeoffset;
	}

	public static void writeInt(int data, OutputStream out) throws IOException
	{
		writeByte(data & 0xFF, out);
		writeByte(data >> 8 & 0xFF, out);
		writeByte(data >> 16 & 0xFF, out);
		writeByte(data >> 24 & 0xFF, out);
	}

	public static void writeShort(int data, OutputStream out) throws IOException
	{
		writeByte(data & 0xFF, out);
		writeByte(data >> 8 & 0xFF, out);
	}

	public static byte readByte(InputStream in) throws IOException
	{
		return (byte) (in.read() & 0xFF);
	}

	public static void readBytes(byte[] data, InputStream in) throws IOException
	{
		int readed = 0;
		int pos = 0;
		if (data.length == 0) return;
		do
		{
			pos += readed;
			if (pos == data.length) return;
		}
		while ((readed = in.read(data, pos, data.length - pos)) != -1);
	}

	public static int readInt(InputStream in) throws IOException
	{
		int b0 = readByte(in);
		int b1 = readByte(in);
		int b2 = readByte(in);
		int b3 = readByte(in);
		return ((b3 & 0xFF) << 24 | (b2 & 0xFF) << 16 | (b1 & 0xFF) << 8 | b0 & 0xFF);
	}

	public static short readShort(InputStream in) throws IOException
	{
		int b0 = readByte(in);
		int b1 = readByte(in);
		return (short) ((b1 & 0xFF) << 8 | b0 & 0xFF);
	}

	public static void writeObject(Object obj, OutputStream out) throws IOException
	{
		int i;
		if (obj == null) return;
		if (obj instanceof Object[])
		{
			Object[] tmp = (Object[]) obj;
			writeInt(tmp.length, out);
			for (i = 0; i < tmp.length; ++i)
				writeObject(tmp[i], out);

			return;
		}
		if (obj instanceof byte[])
		{
			byte[] tmp = (byte[]) obj;
			writeInt(tmp.length, out);
			writeBytes(tmp, out);
			return;
		}
		if (obj instanceof String)
		{
			String tmp = (String) obj;
			char[] data = tmp.toCharArray();
			writeInt(data.length, out);
			for (i = 0; i < data.length; ++i)
				writeShort((short) data[i], out);

			return;
		}
		if (obj instanceof char[])
		{
			char[] data = (char[]) obj;
			writeInt(data.length, out);
			for (i = 0; i < data.length; ++i)
				writeShort((short) data[i], out);

			return;
		}
		if (obj instanceof int[])
		{
			int[] data = (int[]) obj;
			writeInt(data.length, out);
			for (i = 0; i < data.length; ++i)
				writeInt(data[i], out);

			return;
		}
		if (obj instanceof short[])
		{
			short[] data = (short[]) obj;
			writeInt(data.length, out);
			for (i = 0; i < data.length; ++i)
				writeShort(data[i], out);

			return;
		}
	}
}
