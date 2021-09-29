package com.wugui.datax.executor.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class DataXExecutorExampleBootApplicationTests {

	@Test
	public void test() {
		String content = "-DlastTime='%s' -DcurrentTime='%s' -DsplitM=%YYYY-%MM -DsplitD=%YYYY%MM%dd -DsplitY=%YYYY";
		Date date = new Date();

		String ret = handleParams(content, date);

		System.out.println(ret);
	}

	private String handleParams(String content, Date date) {
		String[] params = content.split(" ");

		String ret = "";
		for (int i = 0; i < params.length; i++) {
			String param = params[i];
			String[] kv = param.split("=");
			if (kv[0].startsWith("-Dsplit")) {
				String formatString = kv[1];
				SimpleDateFormat sf = new SimpleDateFormat(formatString);
				String value = sf.format(date);
				value = value.replaceAll("%", "");
				params[i] = kv[0] + "=" + value;
			}

			ret += params[i] + " ";
		}
		return ret;
	}

}