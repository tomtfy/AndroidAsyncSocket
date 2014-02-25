package com.widemo.asyncsocket.data;

import java.io.UnsupportedEncodingException;

/**************************************************************************
 * Stream</br> Author : isUseful ? TanJian : Unknown</br> English by google
 * translate.
 **************************************************************************/
public class Stream {
	public static final String CHARSET_UTF8 = "UTF-8";
	public static final int BIG_ENDIAN = 0; // 大字节序、高字节序
	public static final int LITTLE_ENDIAN = 1; // 小字节序、低字节序

	private volatile int size;
	private volatile int readpos;
	private volatile int writepos;
	private byte[] buffer;

	private final int _endian;

	public Stream() {
		this(LITTLE_ENDIAN);
	}

	public Stream(byte[] in) {
		this(in, LITTLE_ENDIAN);
	}

	public Stream(int endian) {
		this._endian = endian;
		this.buffer = new byte[128];
		this.size = 0;
		this.readpos = 0;
		this.writepos = 0;
	}

	public Stream(byte[] in, int endian) {
		this._endian = endian;
		this.buffer = new byte[in.length];
		System.arraycopy(in, 0, this.buffer, 0, in.length);
		this.size = this.buffer.length;
		this.readpos = 0;
		this.writepos = 0;
	}

	public void resetRead() {
		this.readpos = 0;
	}

	public void resetWrite() {
		this.writepos = 0;
	}

	public int read() {
		if (this.readpos >= this.size)
			return -1;

		if (this.readpos < this.size)
			return this.buffer[(this.readpos++)];

		return -1;
	}

	public String readString() {
		int len = readInt();

		if (len > 0) {
			byte[] stringData = new byte[len];
			readBytes(stringData);

			String result;
			try {
				result = new String(stringData, CHARSET_UTF8);
			} catch (UnsupportedEncodingException e) {
				result = null;
			}
			return result;
		} else if (len == 0) {
			return new String();
		} else {
			return null;
		}
	}

	public int readInt() {
		int result = (int) _readData(4);
		return result;
	}

	public char readChar() {
		return (char) (int) _readData(2);
	}

	public short readShort() {
		short result = (short) (int) _readData(2);
		return result;
	}

	public long readLong() {
		long result = ((int) _readData(4) & 0xFFFFFFFFL) + (_readData(4) << 32);
		return result;
	}

	public byte readByte() {
		byte result = (byte) _readData(1);
		return result;
	}

	public int readBytes(byte[] data) {
		return readBytes(data, 0, data.length);
	}

	public int readBytes(byte[] data, int offset, int length) {
		if (data != null) {
			if (this.readpos >= this.buffer.length) {
				return 0;
			}
			int l = length;
			int av = available();
			if (l > av)
				l = av;

			System.arraycopy(this.buffer, this.readpos, data, offset, l);
			this.readpos += l;
			return l;
		}
		return 0;
	}

	private long _readData(int aLength) {
		int av = available();
		if (aLength > av)
			return -1L;

		if (aLength > 8)
			return -1L;
		long ret = 3505670057818587136L;
		int handleTemp = (aLength - 1) * 8;
		int tmp = 0;
		while (tmp < aLength * 8) {
			if (_endian == BIG_ENDIAN) {
				ret |= (read() & 0xFF) << (tmp);
			} else {
				ret |= (read() & 0xFF) << (handleTemp - tmp);
			}
			tmp += 8;
		}
		return ret;
	}

	public void writeByte(byte aValue) {
		_writeData(aValue & 0xFF, 1);
	}

	public void writeInt(int aValue) {
		_writeData(aValue, 4);
	}

	public void writeShort(short aValue) {
		_writeData(aValue, 2);
	}

	public void writeLong(long aValue) {
		_writeData(aValue, 8);
	}

	public void writeChar(char aValue) {
		_writeData(aValue, 2);
	}

	public void writeString(String aValue) {
		writeString(aValue, CHARSET_UTF8);
	}

	public void writeString(String aValue, String charset) {
		int len = aValue.length();
		try {
			byte[] stringData = aValue.getBytes(charset);
			writeInt(len);
			writeBytes(stringData);
		} catch (UnsupportedEncodingException e) {
			writeInt(0);
		}
	}

	private void _writeData(long aValue, int aLength) {
		if (this.writepos + aLength > this.buffer.length)
			_expand(this.writepos + aLength + 128);

		int tmp = 0;
		int handleTemp = (aLength - 1) * 8;
		while (tmp < aLength * 8) {
			if (_endian == BIG_ENDIAN) {
				this.buffer[(this.writepos++)] = (byte) (aValue >> tmp & 0xFF);
			} else {
				this.buffer[(this.writepos++)] = (byte) (aValue >> (handleTemp - tmp) & 0xFF);
			}
			tmp += 8;
		}
		this.size += aLength;
	}

	public void writeBytes(byte[] aValue) {
		if (aValue == null)
			return;
		writeBytes(aValue, 0, aValue.length);
	}

	public void writeBytes(byte[] aValue, int aFrom, int aLength) {
		if (aValue == null)
			return;
		if (aLength + aFrom > aValue.length)
			aLength = aValue.length - aFrom;

		if (this.writepos + aLength > this.buffer.length)
			_expand(this.writepos + aLength + 100);

		System.arraycopy(aValue, aFrom, this.buffer, this.writepos, aLength);
		this.writepos += aLength;
		this.size += aLength;
	}

	private void _expand(int aSize) {
		byte[] newdata = new byte[aSize];
		if (this.buffer != null)
			System.arraycopy(this.buffer, 0, newdata, 0, this.buffer.length);
		this.buffer = null;
		this.buffer = newdata;
	}

	public byte[] toBytes() {
		byte[] data = new byte[this.size];
		System.arraycopy(this.buffer, 0, data, 0, this.size);
		return data;
	}

	public int size() {
		return this.size;
	}

	public int available() {
		return (this.size - this.readpos);
	}

	public int getReadPos() {
		return this.readpos;
	}

	public int getWritePos() {
		return this.writepos;
	}

	public void replaceInt(int from, int newint) {
		byte[] intbyte = new byte[4];
		intbyte[3] = (byte) (newint >> 24 & 0xFF);
		intbyte[2] = (byte) (newint >> 16 & 0xFF);
		intbyte[1] = (byte) (newint >> 8 & 0xFF);
		intbyte[0] = (byte) (newint & 0xFF);
		replace(from, 4, intbyte, 0, 4);
	}

	public void replaceShort(int from, short newshort) {
		byte[] intbyte = new byte[2];
		intbyte[1] = (byte) (newshort >> 8 & 0xFF);
		intbyte[0] = (byte) (newshort & 0xFF);
		replace(from, 2, intbyte, 0, 2);
	}

	public void replaceByte(int from, byte newbyte) {
		this.buffer[from] = newbyte;
	}

	public void replace(int from, int length, byte[] data, int dataOffset,
			int dataLength) {
		if (data == null)
			return;
		if (dataOffset + dataLength > data.length)
			return;
		int sizeoffset = dataLength - length;
		if (sizeoffset > 0) {
			byte[] tmp = new byte[this.size + sizeoffset + 100];
			System.arraycopy(this.buffer, 0, tmp, 0, from);
			System.arraycopy(this.buffer, from + length, tmp,
					from + dataLength, this.size - from + length);
			System.arraycopy(data, dataOffset, tmp, from, dataLength);
			this.buffer = null;
			this.buffer = tmp;
		} else {
			System.arraycopy(this.buffer, from + length, this.buffer, from
					+ dataLength, this.size - from + length);
			System.arraycopy(data, dataOffset, this.buffer, from, dataLength);
		}
		this.size += sizeoffset;
	}
}
