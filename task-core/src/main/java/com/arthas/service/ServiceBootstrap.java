package com.arthas.service;

import com.arthas.service.command.BuiltinCommandPack;
import com.arthas.service.shell.ShellServer;
import com.arthas.service.shell.ShellServerOptions;
import com.arthas.service.shell.command.CommandResolver;
import com.arthas.service.shell.handlers.BindHandler;
import com.arthas.service.shell.impl.ShellServerImpl;
import com.arthas.service.shell.term.impl.HttpTermServer;
import com.arthas.service.shell.term.impl.TelnetTermServer;
import com.arthas.service.utils.LogUtil;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceBootstrap {

    private static AtomicBoolean isBindRef = new AtomicBoolean(false);

    private static ServiceBootstrap arthasBootstrap;
    private ExecutorService executorService;

    private int pid;
    private Thread shutdown;

    private Instrumentation instrumentation;

    private ServiceBootstrap(int pid, Instrumentation instrumentation) {
        this.pid = pid;
        this.instrumentation = instrumentation;

        executorService = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "as-command-execute-daemon");
                t.setDaemon(true);
                return t;
            }
        });

        shutdown = new Thread("as-shutdown-hooker") {

            @Override
            public void run() {
                ServiceBootstrap.this.destroy();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdown);
    }


    /**
     * @return ArthasServer单例
     */
    public static ServiceBootstrap getInstance() {
        if (arthasBootstrap == null) {
            throw new IllegalStateException("ArthasBootstrap must be initialized before!");
        }
        return arthasBootstrap;
    }


    public void destroy() {

        // see https://github.com/alibaba/arthas/issues/319
        LogUtil.closeResultLogger();
    }

    public void execute(Runnable command) {
        executorService.execute(command);
    }

    public static void main(String[] args) {
        Integer pid = 0;
        String ip = "127.0.0.1";
        int telnetPort = 3658;
        int httpPort = 8563;

        Instrumentation instrumentation = null;

        arthasBootstrap = new ServiceBootstrap(pid, instrumentation);

        ShellServerOptions options = new ShellServerOptions()
                .setInstrumentation(instrumentation)
                .setPid(pid)
                .setSessionTimeout(1000 * 1000);

        ShellServer shellServer;

        //使用命令模式，预设命令
        BuiltinCommandPack builtinCommands = new BuiltinCommandPack();
        List<CommandResolver> resolvers = new ArrayList<CommandResolver>();
        resolvers.add(builtinCommands);

        // 创建服务server
        shellServer = new ShellServerImpl(options, arthasBootstrap);
        // telnet方式的server
        shellServer.registerTermServer(new TelnetTermServer(ip, telnetPort,
                options.getConnectionTimeout()));

        // websocket方式的server
        shellServer.registerTermServer(new HttpTermServer(ip, httpPort,
                options.getConnectionTimeout()));
        // 注册命令解析器
        for (CommandResolver resolver : resolvers) {
            shellServer.registerCommandResolver(resolver);
        }
        //server进行监听，注册错误报错handler ,这个代码主要是监听命令作用，具体的 listen 方法内部看看
        shellServer.listen(new BindHandler(isBindRef));
    }
}
