/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.ipan.nrgyrent.tron.utils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.ipan.nrgyrent.tron.utils.ConsoleColor.*;

public class Utils {
  public static final String PERMISSION_ID = "Permission_id";
  public static final String VISIBLE = "visible";
  public static final String TRANSACTION = "transaction";
  public static final String VALUE = "value";
  public static final String LOCK_WARNING = "⚠️" + ANSI_YELLOW
      + " Wallet is locked. Transaction not allowed. Please use " + greenBoldHighlight("unlock")
      + ANSI_YELLOW + " to retry" + ANSI_RESET;

  private static SecureRandom random = new SecureRandom();

  public static SecureRandom getRandom() {
    return random;
  }




  /** yyyy-MM-dd */
  public static Date strToDateLong(String strDate) {
    if (strDate.length() == 10) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      ParsePosition pos = new ParsePosition(0);
      Date strtodate = formatter.parse(strDate, pos);
      return strtodate;
    } else if (strDate.length() == 19) {
      strDate = strDate.replace("_", " ");
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      ParsePosition pos = new ParsePosition(0);
      Date strtodate = formatter.parse(strDate, pos);
      return strtodate;
    }
    return null;
  }

  public static String yellowBoldHighlight(String str) {
    return ANSI_BOLD + ANSI_YELLOW + str + ANSI_RESET;
  }

  public static String greenHighlight(String str) {
    return ANSI_GREEN + str + ANSI_RESET;
  }

  public static String greenBoldHighlight(String str) {
    return ANSI_BOLD + ANSI_GREEN + str + ANSI_RESET;
  }

  public static String greenBoldHighlight(int i) {
    return ANSI_BOLD + ANSI_GREEN + i + ANSI_RESET;
  }

  public static String blueBoldHighlight(String str) {
    return ANSI_BOLD + ANSI_BLUE + str + ANSI_RESET;
  }

  public static String redBoldHighlight(String str) {
    return ANSI_BOLD + ANSI_RED + str + ANSI_RESET;
  }

  public static String successfulHighlight() {
    return ANSI_BOLD + ANSI_GREEN + " successful" + ANSI_RESET;
  }

  public static String failedHighlight() {
    return ANSI_BOLD + ANSI_RED + " failed" + ANSI_RESET;
  }

  public static long getLong(String str) {
    if (isEmpty(str)) {
      return 300;
    }
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("The parameter is invalid. Please enter an integer.");
    }
  }

  private static byte[] readAllBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[4096];
    int bytesRead;
    while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, bytesRead);
    }
    return buffer.toByteArray();
  }
}
