package com.taobao.arthas.boot;

import com.taobao.arthas.boot.common.AnsiLog;
import com.taobao.arthas.boot.common.SocketUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author hengyunabc 2018-10-26
 */

public class Bootstrap {
    private static final int DEFAULT_TELNET_PORT = 3658;
    private static final int DEFAULT_HTTP_PORT = 8563;
    private static final String DEFAULT_TARGET_IP = "127.0.0.1";
    private static File ARTHAS_LIB_DIR;

    private boolean help = false;

    private long pid = -1;
    private String targetIp;
    private Integer telnetPort;
    private Integer httpPort;
    /**
     * @see com.taobao.arthas.core.config.Configure#DEFAULT_SESSION_TIMEOUT_SECONDS
     */
    private Long sessionTimeout;

    public static final Random random = new Random();
    private Integer height = null;
    private Integer width = null;

    private boolean verbose = false;

    private static final String TASK_CLIENT_JAR_DEFAULT_PACKAGE_NAME = "task-client-jar-with-dependencies.jar";

    private static String TASK_CLIENT_JAR_DETAULT_PACKAGE_PATH = "/task-client/target";

    private static final String TASK_CORE_JAR_DEFAULT_PACKAGE_NAME = "task-core-jar-with-dependencies.jar";

    private static String TASK_CORE_JAR_DETAULT_PACKAGE_PATH = "/task-core/target";

    private static String TASK_CORE_JAR_DETAULT_PRE = "";

    public static String env = "dev";

    /**
     * enforce use http to download arthas. default use https
     */
    private boolean useHttp = false;

    private boolean attachOnly = false;


    static {
        ARTHAS_LIB_DIR = new File(System.getProperty("user.dir"));
        TASK_CORE_JAR_DETAULT_PRE = ARTHAS_LIB_DIR.getAbsolutePath();
        if ("dev".equals(env)) {
            TASK_CLIENT_JAR_DETAULT_PACKAGE_PATH = TASK_CORE_JAR_DETAULT_PRE + TASK_CLIENT_JAR_DETAULT_PACKAGE_PATH;
            TASK_CORE_JAR_DETAULT_PACKAGE_PATH = TASK_CORE_JAR_DETAULT_PRE + TASK_CORE_JAR_DETAULT_PACKAGE_PATH;
        } else {
            TASK_CLIENT_JAR_DETAULT_PACKAGE_PATH = TASK_CORE_JAR_DETAULT_PRE;
            TASK_CORE_JAR_DETAULT_PACKAGE_PATH = TASK_CORE_JAR_DETAULT_PRE;
        }
    }
    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        // check telnet/http port
        long telnetPortPid = -1;
        long httpPortPid = -1;
        boolean startCoreFlag = true;
        if (bootstrap.getTelnetPortOrDefault() > 0) {
            telnetPortPid = SocketUtils.findTcpListenProcess(bootstrap.getTelnetPortOrDefault());
            if (telnetPortPid > 0) {
                if("dev".equals(env)){
                    AnsiLog.info("telnetPortPid :" + telnetPortPid + " 己经存在...");
                }
                startCoreFlag = false;
            }
        }
        if (bootstrap.getHttpPortOrDefault() > 0) {
            httpPortPid = SocketUtils.findTcpListenProcess(bootstrap.getHttpPortOrDefault());
            if (httpPortPid > 0) {
                startCoreFlag = false;
            }
        }
        if (startCoreFlag) {
            Thread subThread = new Thread(new SubThread());
            subThread.start();
            lockTryTimes(10000l);
        }
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{new File(TASK_CLIENT_JAR_DETAULT_PACKAGE_PATH, TASK_CLIENT_JAR_DEFAULT_PACKAGE_NAME).toURI().toURL()});
        AnsiLog.info(" client path :" + TASK_CLIENT_JAR_DETAULT_PACKAGE_PATH + "," + TASK_CLIENT_JAR_DEFAULT_PACKAGE_NAME);
        Class<?> telnetConsoleClas = classLoader.loadClass("com.arthas.client.TelnetConsole");
        Method mainMethod = telnetConsoleClas.getMethod("main", String[].class);

        List<String> telnetArgs = new ArrayList<String>();
        Thread.currentThread().setContextClassLoader(classLoader);
        mainMethod.invoke(null, new Object[]{telnetArgs.toArray(new String[0])});
    }

    private static class SubThread implements Runnable {
        public void run() {
            try {
                List<String> serviceArgs = Arrays.asList(new String[]{"-jar", TASK_CORE_JAR_DETAULT_PACKAGE_PATH + "/" + TASK_CORE_JAR_DEFAULT_PACKAGE_NAME});
                ProcessUtils.startArthasCore(serviceArgs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean lockTryTimes(final Long tryTimes) {
        try {
            // 请求锁超时时间，毫秒
            long timeout = tryTimes * 1000;
            // 系统当前时间，毫秒
            long nowTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - nowTime) < timeout) {
                seleep(10, 500);
                long telnetPortPid = SocketUtils.findTcpListenProcess(DEFAULT_TELNET_PORT);
                if (telnetPortPid > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void seleep(long millis, int nanos) {
        try {
            Thread.sleep(millis, random.nextInt(nanos));
        } catch (InterruptedException e) {
            AnsiLog.info("获取分布式锁休眠被中断：", e);
        }
    }

    private static String listVersions() {
        StringBuilder result = new StringBuilder(1024);
        List<String> versionList = listNames(ARTHAS_LIB_DIR);
        Collections.sort(versionList);

        result.append("Local versions:\n");
        for (String version : versionList) {
            result.append(" " + version).append('\n');
        }
        result.append("Remote versions:\n");

        List<String> remoteVersions = DownloadUtils.readRemoteVersions();
        Collections.reverse(remoteVersions);
        for (String version : remoteVersions) {
            result.append(" " + version).append('\n');
        }
        return result.toString();
    }

    private static List<String> listNames(File dir) {
        List<String> names = new ArrayList<String>();
        if (!dir.exists()) {
            return names;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return names;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".") || file.isFile()) {
                continue;
            }
            names.add(name);
        }
        return names;
    }


    public boolean isuseHttp() {
        return useHttp;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public String getTargetIpOrDefault() {
        if (this.targetIp == null) {
            return DEFAULT_TARGET_IP;
        } else {
            return this.targetIp;
        }
    }

    public Integer getTelnetPort() {
        return telnetPort;
    }

    public int getTelnetPortOrDefault() {
        if (this.telnetPort == null) {
            return DEFAULT_TELNET_PORT;
        } else {
            return this.telnetPort;
        }
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public int getHttpPortOrDefault() {
        if (this.httpPort == null) {
            return DEFAULT_HTTP_PORT;
        } else {
            return this.httpPort;
        }
    }

    public boolean isAttachOnly() {
        return attachOnly;
    }

    public long getPid() {
        return pid;
    }

    public boolean isHelp() {
        return help;
    }


    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

}
