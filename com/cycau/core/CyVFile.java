package com.cycau.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/***
 * @author kogen.cy
 * @author y.cycau@gmail.com
 * 
 * 目的：　I)整合性保証、II)CSVのエスケープによる負担軽減
 * 項目長さ：[0-9A-Za-z] MAX:61
 * 長項目の続き（文章）： p～
 * デフォルト行の終わり： ;`\n
 */
public class CyVFile {
//	private final char SPLIT = '`';
	private final short MAX_COLUMN_LENGHT = 50;
	private final byte[] LEN2CHAR = {
		 '0','1','2','3','4','5','6','7','8','9'
		,'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
		,'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'
	};
	private final short[] CHAR2LEN = {
		-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
		,0,1,2,3,4,5,6,7,8,9
		,-1,-1,-1,-1,-1,-1,-1
		,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35
		,-1,-1,-1,-1,-1,-1
		,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61
	};

	private byte[] END_OF_LINE = {';', '`'};
	private int DEFAULT_ROW_LENGTH = 65535;
	private byte[] BUFF;
	private FileInputStream fi;
	private FileOutputStream fo;
	private long startTime;
			
	public CyVFile(String inputPath, String... endOfLine) {
		try {
			this.BUFF = new byte[DEFAULT_ROW_LENGTH];
			this.fi = new FileInputStream(inputPath);
			if (endOfLine.length > 0) {
				this.END_OF_LINE = endOfLine[0].getBytes();
			}
			startTime = System.currentTimeMillis();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public CyVFile(String outputPath, boolean append, String... endOfLine) {
		try {
			this.BUFF = new byte[DEFAULT_ROW_LENGTH];
			this.fo = new FileOutputStream(outputPath, append);
			if (endOfLine.length > 0) {
				this.END_OF_LINE = endOfLine[0].getBytes();
			}
			startTime = System.currentTimeMillis();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * @param outputPath
	 * @param append
	 * @param maxFileSize 1MB unit
	 * @param endOfLine
	 */
	private String fileName;
	private String fileExt;
	private short fileNum = 0;
	private int MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2GB
	private int wroteLen = 0;
	public CyVFile(String outputPath, int maxFileSize, String... endOfLine) {
		try {
			this.BUFF = new byte[DEFAULT_ROW_LENGTH];
			if (maxFileSize > 0) {
				this.MAX_FILE_SIZE = maxFileSize * 1024 * 1024; // maxFileSize * 1MB
				int posDot = outputPath.lastIndexOf(".");
				int posSlash = outputPath.lastIndexOf("/");
				if (posDot > 0 && posDot > posSlash) {
					this.fileName = outputPath.substring(0, posDot);
					this.fileExt = outputPath.substring(posDot);
				} else {
					this.fileName = outputPath;
					this.fileExt = "";
				}
				this.fileNum = 1;
			}
			this.fo = new FileOutputStream(fileName + ".00000" + fileExt);
			if (endOfLine.length > 0) {
				this.END_OF_LINE = endOfLine[0].getBytes();
			}
			startTime = System.currentTimeMillis();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private int BUFF_POS = -1;
	private int BUFF_LEN = 0;
	private int ROW_CNT = 0;
	private boolean EOF = false;
	private List<String> CURR_ROW;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public String currStr(int columnIndex) {
		return getVal(columnIndex);
	}
	public Integer currInt(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		return Integer.parseInt(val);
	}
	public Short currShort(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		return Short.parseShort(val);
	}
	public Long currLong(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		return Long.parseLong(val);
	}
	public Float currFloat(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		return Float.parseFloat(val);
	}
	public Double currDouble(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		return Double.parseDouble(val);
	}
	public BigDecimal currBigDecimal(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		return new BigDecimal(val);
	}
	public Date currDate(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		try {
			return dateFormat.parse(val);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	public Date currDateTime(int columnIndex) {
		String val = getVal(columnIndex);
		if (val == null) return null;
		try {
			return dateTimeFormat.parse(val);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	private String getVal(int columnIndex) {
		if (CURR_ROW == null) return null;
		if (columnIndex >= CURR_ROW.size()) throw new RuntimeException("out of bounds, " + columnIndex + "/" + CURR_ROW.size());
		return CURR_ROW.get(columnIndex);
	}
	/**
	 * @return List<String>
	 */
	public List<String> read() {
		if (BUFF_POS >= BUFF_LEN) return null;
		
		int byteTmpPos = 0;
		byte[] byteTmp = new byte[1024];
		CURR_ROW = new ArrayList<String>();
		while (BUFF_POS < BUFF_LEN) {
			// 余裕を持たせて続きを読み込む
			if (!EOF && BUFF_POS > (BUFF_LEN-64)) {
				if (BUFF_POS < 0) BUFF_POS = 0;
				int copyLen = BUFF_LEN - BUFF_POS;
				if (copyLen > 0) System.arraycopy(this.BUFF, BUFF_POS, this.BUFF, 0, copyLen);
				try {
					BUFF_LEN = fi.read(this.BUFF, copyLen, this.BUFF.length - copyLen);
					if (BUFF_LEN < 1) EOF = true;
					BUFF_LEN += copyLen;
					BUFF_POS = 0;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				continue;
			}

			// 行の終わり判断
			if (BUFF_POS < (BUFF_LEN-1) && BUFF[BUFF_POS] == END_OF_LINE[0] && BUFF[BUFF_POS+1] == END_OF_LINE[1]) {
				if (BUFF_POS == (BUFF_LEN-2)) { //ファイル最後
					BUFF_POS += END_OF_LINE.length;
					ROW_CNT++;
					return CURR_ROW;
				}
				if (BUFF_POS < (BUFF_LEN-2) && BUFF[BUFF_POS+2] == '\n') {
					BUFF_POS += END_OF_LINE.length + 1;
					ROW_CNT++;
					return CURR_ROW;
				}
				if (BUFF_POS < (BUFF_LEN-3) && BUFF[BUFF_POS+2] == '\r' && BUFF[BUFF_POS+3] == '\n') {
					BUFF_POS += END_OF_LINE.length + 2;
					ROW_CNT++;
					return CURR_ROW;
				}
			}

			byte charOfLen = BUFF[BUFF_POS++];
			if (charOfLen > 122) throw new RuntimeException(String.format("bad format!, row number [%,3d]", ROW_CNT+1));
			short len = CHAR2LEN[charOfLen];
			if (len < 0) throw new RuntimeException(String.format("bad format!, row number [%,3d]", ROW_CNT+1));

			// 空の場合
			if (len == 0) {
				if (byteTmpPos > 0) {
					CURR_ROW.add(new String(byteTmp, 0, byteTmpPos));
					byteTmpPos = 0;
					continue;
				}
				CURR_ROW.add("");
				continue;
			}

			// 短項目を読み込む
			if (len <= MAX_COLUMN_LENGHT) {
				if (byteTmpPos > 0) {
					byteTmp = writeByte(this.BUFF, BUFF_POS, byteTmp, byteTmpPos, len);
					CURR_ROW.add(new String(byteTmp, 0, byteTmpPos + len));
					byteTmpPos = 0;
				} else {
					CURR_ROW.add(new String(this.BUFF, BUFF_POS, len));
				}
				BUFF_POS += len;
				continue;
			}

			// 長項目を読み込む
			byteTmp = writeByte(BUFF, BUFF_POS, byteTmp, byteTmpPos, len);
			BUFF_POS += len;
			byteTmpPos += len;
		}

		ROW_CNT++;
		return CURR_ROW;
	}
	/**
	 * @param columns
	 * @return CyVFile
	 */
	public CyVFile write(Object[] columns) {
		if (columns == null) return null;

		int pos = 0;
		for (int idx=0; idx<columns.length; idx++) {
			// nullの場合は空扱い
			if (columns[idx] == null) {
				this.BUFF[pos++] = LEN2CHAR[0];
				continue;
			}

			byte[] src = convByte(columns[idx]);
			// 短い項目の場合は、書き込むのみ
			if (src.length <= MAX_COLUMN_LENGHT) {
				this.BUFF[pos++] = LEN2CHAR[src.length];
				this.BUFF = writeByte(src,0, BUFF, pos, src.length);
				pos += src.length;
				continue;
			}

			// 長い項目は分割して書き込む
			int writeLen = 0;
			int leftLen = src.length;
			while (leftLen > 0) {
				writeLen = leftLen;
				if (writeLen > MAX_COLUMN_LENGHT) {
					writeLen = MAX_COLUMN_LENGHT+1;
					//マルチバイトの２バイト以降の場合
					while((src[src.length - leftLen + writeLen] & 0xC0) == 0x80) {
						writeLen++;
					}
				}
				this.BUFF[pos++] = LEN2CHAR[writeLen];
				this.BUFF = writeByte(src, src.length - leftLen, BUFF, pos, writeLen);
				pos += writeLen;
				leftLen -= writeLen;
			}
			if (writeLen > MAX_COLUMN_LENGHT) {
				this.BUFF[pos++] = LEN2CHAR[0];
			}
		}
		this.BUFF = writeByte(END_OF_LINE, 0, BUFF, pos, END_OF_LINE.length);
		pos += END_OF_LINE.length;
		this.BUFF[pos++] = '\n';
		try {
			fo.write(this.BUFF, 0, pos);
			ROW_CNT++;
			if (fileNum > 0) {
				wroteLen += pos;
				if (wroteLen > MAX_FILE_SIZE) {
					fo.close();
					this.fo = new FileOutputStream(fileName + "." + String.format("%05d", fileNum) + fileExt);
					System.out.printf("CyVFile wrote count [%,3d]\n", ROW_CNT);
					wroteLen = 0;
					fileNum++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return this;
	}
	private byte[] convByte(Object coldata) {
		if (coldata == null) return null;
		if (coldata instanceof String) return ((String)coldata).getBytes();
		if (coldata instanceof Date) return dateTimeFormat.format((Date)coldata).getBytes();
		return coldata.toString().getBytes();
	}
	private byte[] writeByte(byte[] src, int pos, byte[] desc, int descPos, int lengh) {
		if (descPos + lengh < desc.length) {
			System.arraycopy(src, pos, desc, descPos, lengh);
			return desc;
		}

		byte[] byteExt = new byte[desc.length + 1024]; // 1KByte拡張
		System.arraycopy(desc, 0, byteExt, 0, desc.length);
		System.arraycopy(src, pos, byteExt, descPos, lengh);
		return byteExt;
	}
	public void close() {
		try {
			if (this.fi != null) {
				fi.close();
				System.out.printf("CyVFile read count [%,3d] in %,3dms\n", ROW_CNT, System.currentTimeMillis()- startTime);
			}
			if (this.fo != null) {
				fo.close();
				System.out.printf("CyVFile wrote count [%,3d] in %,3dms\n", ROW_CNT, System.currentTimeMillis()- startTime);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
