import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;

/**
 * Copyright 2018 Mobvoi Inc. All Rights Reserved.
 *
 * @author chao.wang   chao.wang@mobvoi.com.
 * @date 2018-11-28 下午5:56.
 */
public class TestDate {

  public static void main(String[] agrs) throws ParseException {

    String date = "2018-10-11";

    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date from = sdf.parse(date);
    System.out.println(from.toString());
  }


}
