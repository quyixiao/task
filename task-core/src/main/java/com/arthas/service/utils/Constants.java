package com.arthas.service.utils;


import java.io.File;


/**
 * @author ralf0131 2016-12-28 16:20.
 */
public interface Constants {


    public static final long SECOND_OF_ONE_MINITS = 1 * 60l;
    public static final long SECOND_OF_TEN_MINITS = 10 * 60l;
    public static final long SECOND_OF_FIVE_MINITS = 5 * 60l;
    public static final long SECOND_OF_THREE = 30l;// 30秒
    public static final long SECOND_OF_HALF_HOUR = 30 * 60l;
    public static final long SECOND_OF_AN_HOUR = 60 * 60l;
    public static final long SECOND_OF_HALF_DAY = 14 * 60 * 60l;
    public static final long SECOND_OF_ONE_DAY = 24 * 60 * 60l;

    public static final int SECOND_OF_HALF_HOUR_INT = 30 * 60;
    public static final int SECOND_OF_AN_HOUR_INT = 60 * 60;

    public static final long SECOND_OF_ONE_WEEK = 7 * 24 * 60 * 60l;
    public static final long SECOND_OF_ONE_MONTH = 30 * 24 * 60 * 60l;

    public static final long MILLISECOND_OF_TWO_HOUR = 2 * 60 * 60 * 1000l;

    public static final int MINITS_OF_2HOURS = 120;
    public static final int MINITS_OF_HALF_HOUR = 30;

    public static final int MONTH_OF_YEAR = 12;

    public static final int DAY_OF_YEAR = 360;

    public static final int RATE_OF_UNIT = 100;

    public static final int DAY_OF_MONTH = 30;

    /**
     * Spy的全类名
     */
    String SPY_CLASSNAME = "com.arthas.spy.Spy";

    /**
     * 中断提示
     */
    String Q_OR_CTRL_C_ABORT_MSG = "Press Q or Ctrl+C to abort.";

    /**
     * 空字符串
     */
    String EMPTY_STRING = "";

    /**
     * 命令提示符
     */
    String DEFAULT_PROMPT = "$ ";

    /**
     * 带颜色命令提示符
     * raw string: "[33m$ [m"
     */
    String COLOR_PROMPT = Ansi.ansi().fg(Ansi.Color.YELLOW).a(DEFAULT_PROMPT).reset().toString();

    /**
     * 方法执行耗时
     */
    String COST_VARIABLE = "cost";

    String CMD_HISTORY_FILE = System.getProperty("user.home") + File.separator + ".arthas" + File.separator + "history";


    /**
     * 缓存目录
     */
    String CACHE_ROOT = LogUtil.LOGS_DIR + File.separator + "arthas-cache";


    /**
     * 当前进程PID
     */
    String PID = ApplicationUtils.getPid();


}
