package com.han.camunda.bpm.example.util;

import com.yestae.framework.common.utils.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @program: base
 * @description: 数字工具类
 * @author: zouco
 * @create: 2019-08-20 09:29
 **/
public class NumberUtil {

    /**
     * * 描述：通过一个整数i获取你所要的哪几个(从0开始) i为 多个2的n次方之和，
     * 如i=7，那么根据原值是2的n次方之各，你的原值必定是1，2，4 。
     * *
     * * @param i
     * * 数值
     * * @return
     */
    public static int[] getWhich(long i) {
        int exp = Math.getExponent(i);
        if (i == (1 << (exp + 1)) - 1) {
            exp = exp + 1;
        }
        int[] num = new int[exp];
        int x = exp - 1;
        for (int n = 0; (1 << n) < i + 1; n++) {
            if ((1 << (n + 1)) > i && (1 << n) < (i + 1)) {
                num[x] = n;
                i -= 1 << n;
                n = 0;
                x--;
            }
        }
        return num;
    }

    /**
     * * 描述：非四舍五入取整处理
     * *
     * * @param v
     * * 需要四舍五入的数字
     * * @return
     */
    public static int roundDown(double v) {
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, 0, BigDecimal.ROUND_DOWN).intValue();
    }

    /**
     * * 描述：四舍五入取整处理
     * *
     * * @param v
     * * 需要四舍五入的数字
     * * @return
     */
    public static int roundUp(double v) {
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, 0, BigDecimal.ROUND_UP).intValue();
    }

    /**
     * * 描述：提供精确的小数位四舍五入处理。
     * *
     * * @param v
     * * 需要四舍五入的数字
     * * @param scale
     * * 小数点后保留几位
     * * @return 四舍五入后的结果
     */
    public static double round(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }

        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * * 描述：四舍五入保留两位小数
     * *
     * * @param num
     * * 数字
     * * @return 保留两位小数的数字字串
     */
    public static String format(double num) {
        return format(num, "0.00");
    }

    /**
     * * 描述：四舍五入数字保留小数位
     * *
     * * @param num
     * * 数字
     * * @param digits
     * * 小数位
     * * @return
     */
    public static String format(double num, int digits) {
        String pattern = "0";
        if (digits > 0) {
            pattern += "." + createStr("0", digits, "");
        }
        return format(num, pattern);
    }

    /**
     * * 描述：生成字符串
     * *
     * * @param arg0
     * * 字符串元素
     * * @param arg1
     * * 生成个数
     * * @param arg2
     * * 间隔符号
     * * @return
     */
    private static String createStr(String arg0, int arg1, String arg2) {
        if (arg0 == null) {
            return "";
        } else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < arg1; i++) {
                if (arg2 == null)
                    arg2 = "";
                sb.append(arg0).append(arg2);
            }
            if (sb.length() > 0) {
                sb.delete(sb.lastIndexOf(arg2), sb.length());
            }

            return sb.toString();
        }
    }

    /**
     * * 描述：数字格式化
     * *
     * * @param num
     * * 数字
     * * @param pattern
     * * 格式
     * * @return
     */
    public static String format(double num, String pattern) {
        NumberFormat fmt = null;
        if (pattern != null && pattern.length() > 0) {
            fmt = new DecimalFormat(pattern);
        } else {
            fmt = new DecimalFormat();
        }
        return fmt.format(num);
    }

    /**
     * * 求浮点数的权重
     * *
     * * @param number
     * * @return
     */
    public static double weight(double number) {
        if (number == 0) {
            return 1;
        }

        double e = Math.log10(Math.abs(number));
        int n = Double.valueOf(Math.floor(e)).intValue();
        double weight = 1;
        if (n > 0) {
            for (int i = 0; i < n; i++) {
                weight *= 10;
            }
        } else {
            for (int i = 0; i > n; i--) {
                weight /= 10;
            }
        }
        return weight;
    }

    /**
     * 获得权重的单位
     *
     * @param scale
     * @return
     */
    public static String unit(double scale) {
        if (scale == 1 || scale == 0) {
            return "";// 不设置单位倍率单位，使用基准单位
        }
        String[] units = new String[]{"十", "百", "千", "万", "十万", "百万", "千万", "亿", "十亿", "百亿", "千亿", "兆"};
        String[] units2 = new String[]{"十分", "百分", "千分", "万分", "十万分", "百万分", "千万分"};
        double e = Math.log10(scale);
        int position = Double.valueOf(Math.ceil(e)).intValue();
        if (position >= 1 && position <= units.length) {
            return units[position - 1];
        } else if (position <= -1 && -position <= units2.length) {
            return units2[-position - 1];
        } else {
            return "无量";
        }
    }

    /**
     * 获得浮点数的缩放比例
     *
     * @param num
     * @return
     */
    public static double scale(double num) {
        double absValue = Math.abs(num);
        // 无需缩放
        if (absValue < 10000 && absValue >= 1) {
            return 1;
        }
        // 无需缩放
        else if (absValue < 1 && absValue > 0.0001) {
            return 1;
        } else {
            return weight(num) / 10;
        }
    }

    /**
     * 获得缩放后并且格式化的浮点数
     *
     * @param num
     * @param scale
     * @return
     */
    public static double scaleNumber(double num, double scale) {
        DecimalFormat df = null;
        if (scale == 1) {
            df = new DecimalFormat("#.0000");
        } else {
            df = new DecimalFormat("#.00");
        }
        double scaledNum = num / scale;
        return Double.valueOf(df.format(scaledNum));
    }

    /**
     * 产生n位随机数 TODO:性能不要，有待优化
     */
    public static String ramdomNumber(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive !");
        }
        Random random = new Random();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < n; i++) {
            result.append(random.nextInt(10));
        }
        return result.toString();
    }

    /**
     * 缩放1W倍
     */
    public static double changeTo(double number) {
        boolean flag = false;
        if (number < 0) {
            flag = true;
        }
        double value = Math.abs(number);
        value = value / 10000.0;
        if (flag) {
            value = Double.parseDouble("-" + value);
        }
        return value;
    }

    /**
     * 描述：缩放比例
     *
     * @param number
     * @param scale
     * @param points
     * @return
     */
    public static String scaleNumberToStr(double number, double scale, int points) {
        boolean flag = (number < 0);
        number = Math.abs(number);
        String result = "";
        DecimalFormat nbf3 = (DecimalFormat) NumberFormat.getInstance();// 默认格式
        nbf3.setGroupingUsed(false);
        nbf3.setMinimumFractionDigits(points);
        nbf3.setMaximumFractionDigits(points);
        double scaledNum = number / scale;
        result = nbf3.format(scaledNum);
        if (flag) {
            result = "-" + result;
        }
        return result;
    }

    /**
     * @return int
     * @Author hanyf
     * @Description 对象为空返回指定值
     * @Date 10:33 2019/7/30
     * @Param in 传入的对象
     * @Param intForNull in 为空的返回值
     **/
    public static int getInt(Integer in, int intForNull) {
        if (in == null)
            return intForNull;
        else
            return in.intValue();
    }

    /**
     * @return int
     * @Author hanyf
     * @Description Integer 为空返回 0
     * @Date 10:34 2019/7/30
     * @Param [in]
     **/
    public static int getInt(Integer in) {
        return getInt(in, 0);
    }

    /**
     * @return int
     * @Author hanyf
     * @Description Integer 小于 0 返回 0
     * @Date 10:35 2019/7/30
     * @Param [in]
     **/
    public static int getPositiveInt(Integer in) {
        int value = getInt(in, 0);
        if (value < 0) {
            return 0;
        }
        return value;
    }

    /**
     * @return int
     * @Author hanyf
     * @Description String 转 int
     * @Date 10:35 2019/7/30
     * @Param [in, intForNull]
     **/
    public static int getInt(String in, int intForNull) {
        int iRet = intForNull;
        try {
            if ((in == null) || (in.trim().length() <= 0))
                iRet = intForNull;
            else
                iRet = Integer.parseInt(in);
        } catch (NumberFormatException e) {
            return iRet = intForNull;
        }

        return iRet;
    }

    /**
     * @return int
     * @Author hanyf
     * @Description
     * @Date 10:36 2019/7/30
     * @Param [in]
     **/
    public static int getInt(String in) {
        return getInt(in, 0);
    }

    /**
     * @return int
     * @Author hanyf
     * @Description 对象转 int
     * @Date 10:36 2019/7/30
     * @Param [in]
     **/
    public static int getInt(Object in) {
        if (in == null) {
            return getInt("", 0);
        } else {
            return getInt(in.toString(), 0);
        }
    }

    public static Integer getInteger(Long l) {
        return l == null ? null : l.intValue();
    }

    /**
     * @return double
     * @Author hanyf
     * @Description 对象转 double
     * @Date 10:37 2019/7/30
     * @Param [in]
     **/
    public static double getDouble(Object in) {
        if (in == null) {
            return 0;
        } else if (in instanceof BigDecimal) {
            return ((BigDecimal) in).doubleValue();
        } else if (in instanceof Double) {
            return ((Double) in).doubleValue();
        } else {
            double rst = 0;

            try {
                rst = Double.parseDouble(("" + in).trim());
            } catch (Exception e) {
                rst = 0;
            }

            return rst;
        }
    }

    /**
     * @return long
     * @Author hanyf
     * @Description 对象转 long
     * @Date 10:38 2019/7/30
     * @Param [in]
     **/
    public static long getLong(Object in) {
        if (in == null) {
            return 0;
        } else if (in instanceof BigDecimal) {
            return ((BigDecimal) in).longValue();
        } else if (in instanceof Double) {
            return ((Double) in).longValue();
        } else {
            long rst = 0;

            try {
                rst = Long.parseLong(("" + in).trim());
            } catch (Exception e) {
                rst = 0;
            }

            return rst;
        }
    }

    /**
     * @return java.lang.Short
     * @Author hanyf
     * @Description short类型 判断是否为空
     * @Date 10:38 2019/7/30
     * @Param [in]
     **/
    public static Short getShort(Short in) {
        if (in == null) {
            return 0;
        }
        return in;
    }

    /**
     * @return java.util.List<T>
     * @Author hanyf
     * @Description String 转集合
     * @Date 10:40 2019/7/30
     * @Param [strNumber, regex]
     **/
    public static <T> List<T> toNumberList(String strNumber, String regex) {
        try {
            if (strNumber == null || strNumber.equals("")) {
                return null;
            }
            List<T> t = new ArrayList<T>();
            String[] numberArray = strNumber.split(regex);

            for (String s : numberArray) {
                String methodName = "parse" + ((T) s).getClass().getName();
                Method m1 = ((T) s).getClass().getMethod(methodName, null);

                t.add((T) (m1.invoke(s, null)));
            }
            return t;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<Long> toLongList(String strNumber, String regex) {
        List<Long> t = new ArrayList<Long>();
        if (strNumber == null || strNumber.equals("")) {
            return t;
        }
        String[] numberArray = strNumber.split(regex);
        for (String s : numberArray) {
            t.add(getLong(s));
        }

        return t;
    }

    /**
     * @return java.util.List<T>s
     * @Author hanyf
     * @Description String 转集合
     * @Date 10:40 2019/7/30
     * @Param [strNumber, regex]
     **/
    public static List<String> toStringList(String strNumber, String regex) {
        try {
            if (strNumber == null || strNumber.equals("")) {
                return null;
            }
            List<String> t = new ArrayList<String>();
            String[] numberArray = strNumber.split(regex);

            for (String s : numberArray) {
                t.add(s);
            }
            return t;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<Integer> toIntegerList(String strNumber, String regex) {
        List<Integer> t = new ArrayList<Integer>();
        if (strNumber == null || strNumber.equals("")) {
            return t;
        }
        String[] numberArray = strNumber.split(regex);
        for (String s : numberArray) {
            t.add(getInt(s));
        }

        return t;
    }


    public static Set<Long> toLongSet(String strNumber, String regex) {
        Set<Long> t = new HashSet<Long>();
        if (strNumber == null || strNumber.equals("")) {
            return t;
        }
        String[] numberArray = strNumber.split(regex);
        for (String s : numberArray) {
            t.add(getLong(s));
        }

        return t;
    }

    public static Set<String> toStringSet(String strNumber, String regex) {
        Set<String> t = new HashSet<String>();
        if (strNumber == null || strNumber.equals("")) {
            return t;
        }
        String[] numberArray = strNumber.split(regex);
        for (String s : numberArray) {
            t.add(s);
        }

        return t;
    }

    public static short getShort(String shot, short shortForNull) {
        short resuFlag = shortForNull;
        try {
            if ((shot == null) || (shot.trim().length() <= 0))
                resuFlag = shortForNull;
            else
                resuFlag = Short.parseShort(shot);
        } catch (Exception e) {
            return resuFlag = shortForNull;
        }
        return resuFlag;
    }

    public static short getShort(String shot) {
        return getShort(shot, (short) 0);
    }

    public static short getShort(Object shot) {
        if (shot == null) {
            return getShort("", (short) 0);
        } else {
            return getShort(shot.toString(), (short) 0);
        }
    }

    public static short getShort(Short shot, short shortForNull) {
        if (shot == null)
            return shortForNull;
        else
            return shot.shortValue();
    }

    public static Float getFloat(String str) {
        if ((str == null) || (str.trim().length() <= 0))
            return (float) 0;
        else
            return Float.parseFloat(str);
    }

    public static BigDecimal toBigDecimal(Object obj, int newScale) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal bd = new BigDecimal(obj.toString());
            // 设置小数位数，第一个变量是小数位数，第二个变量是取舍方法(四舍五入)
            return bd.setScale(newScale, BigDecimal.ROUND_HALF_UP);

        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * @return java.math.BigDecimal
     * @Author hanyf
     * @Description 默认保留两位小数
     * @Date 11:16 2019/7/30
     * @Param [obj]
     **/
    public static BigDecimal toBigDecimal(Object obj) {
        return toBigDecimal(obj, 2);
    }

    /**
     * @return java.lang.String
     * @Author hanyf
     * @Description List 转 String
     * @Date 11:16 2019/7/30
     * @Param [list, separator]
     **/
    public static String listToString(List<?> list, char separator) {
        if (list == null) {
            return "";
        }
        return StringUtils.join(list.toArray(), separator);
    }
}
