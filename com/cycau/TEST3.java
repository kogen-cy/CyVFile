package com.cycau;


import java.util.List;
import java.util.Random;

import com.cycau.core.CyVFile;

public class TEST3 {

	public static void main(String[] args) {
		test1("C:\\temp\\tset.cyv");
		test2("C:\\temp\\tset.00005.cyv");
	}
	private static void test1(String filePath) {
		CyVFile cyv = new CyVFile(filePath, 1);
		for (int idx=0; idx<10000; idx++) {
			cyv.write(makeData());
		}
		cyv.close();
	}
	private static String[] makeData() {
		Random random = new Random();
		String[] str = new String[10];
		str[0] = String.valueOf(random.nextInt(10000));
		str[1] = "bbbbbbbbbbbbbbbb" + random.nextInt(1000000);
		str[2] = "cccccccccccccccc" + random.nextInt(1000000);
		str[3] = "dddddddddddddddd" + random.nextInt(1000000);
		str[4] = "eeeeeeeeeeeeeeee" + random.nextInt(1000000);
		str[5] = "ffffffffffffffff" + random.nextInt(1000000);
		str[6] = "gggggggggggggggg" + random.nextInt(1000000);
		str[7] = "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" + random.nextInt(1000000);
		str[8] = "iiiiiiiiiiiiiiii" + random.nextInt(1000000);
		str[9] = "文を連ねて、まとまった思想・感情を表現したもの。 主に詩に対して、散文をいう。 ２ 文法で、文よりも大きな単位。 一文だけのこともあるが、通常はいくつかの文が集まって、まとまった思想・話題を表現するもの" + random.nextInt(1000000);
		return str;
	}
	private static void test2(String filePath) {
		CyVFile cyv = new CyVFile(filePath);
		List<String> row;
		while((row = cyv.read()) != null) {
			System.out.println(row);
			System.out.println(cyv.currShort(0));
			System.out.println(cyv.currInt(0));
			System.out.println(cyv.currLong(0));
			System.out.println(cyv.currFloat(0));
			System.out.println(cyv.currDouble(0));
			System.out.println(cyv.currBigDecimal(0));
		}
		cyv.close();
	}
}
