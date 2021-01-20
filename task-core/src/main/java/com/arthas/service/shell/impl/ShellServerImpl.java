package com.arthas.service.shell.impl;

import com.arthas.service.ServiceBootstrap;
import com.arthas.service.shell.Shell;
import com.arthas.service.shell.ShellServer;
import com.arthas.service.shell.ShellServerOptions;
import com.arthas.service.shell.command.CommandResolver;
import com.arthas.service.shell.future.Future;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.handlers.server.SessionClosedHandler;
import com.arthas.service.shell.handlers.server.SessionsClosedHandler;
import com.arthas.service.shell.handlers.server.TermServerListenHandler;
import com.arthas.service.shell.handlers.server.TermServerTermHandler;
import com.arthas.service.shell.system.Job;
import com.arthas.service.shell.system.impl.GlobalJobControllerImpl;
import com.arthas.service.shell.system.impl.InternalCommandManager;
import com.arthas.service.shell.system.impl.JobControllerImpl;
import com.arthas.service.shell.term.Term;
import com.arthas.service.shell.term.TermServer;
import com.arthas.service.utils.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellServerImpl extends ShellServer {

    private static final Logger logger = LogUtil.getArthasLogger();

    private final CopyOnWriteArrayList<CommandResolver> resolvers;
    private final InternalCommandManager commandManager;
    private final List<TermServer> termServers;
    private final long timeoutMillis;
    private final long reaperInterval;
    private String welcomeMessage;
    private ServiceBootstrap bootstrap;
    private Instrumentation instrumentation;
    private int pid;
    private boolean closed = true;
    private final Map<String, ShellImpl> sessions;
    private final Future<Void> sessionsClosed = Future.future();
    private ScheduledExecutorService scheduledExecutorService;
    private JobControllerImpl jobController = new GlobalJobControllerImpl();

    public ShellServerImpl(ShellServerOptions options) {
        this(options, null);
    }

    public ShellServerImpl(ShellServerOptions options, ServiceBootstrap bootstrap) {
        this.welcomeMessage = options.getWelcomeMessage();
        this.termServers = new ArrayList<TermServer>();
        this.timeoutMillis = options.getSessionTimeout();
        this.sessions = new ConcurrentHashMap<String, ShellImpl>();
        this.reaperInterval = options.getReaperInterval();
        this.resolvers = new CopyOnWriteArrayList<CommandResolver>();
        this.commandManager = new InternalCommandManager(resolvers);
        this.instrumentation = options.getInstrumentation();
        this.bootstrap = bootstrap;
        this.pid = options.getPid();

        // Register builtin commands so they are listed in help
        resolvers.add(new BuiltinCommandResolver());
    }

    @Override
    public synchronized ShellServer registerCommandResolver(CommandResolver resolver) {
        resolvers.add(0, resolver);
        return this;
    }

    @Override
    public synchronized ShellServer registerTermServer(TermServer termServer) {
        termServers.add(termServer);
        return this;
    }

    // 这里就初始化了 sessoin对象，然后设置了对应的欢迎语，所以你 attach 成功后，看到了图形界面 wiki,version 等
    public void handleTerm(Term term) {
        synchronized (this) {
            // That might happen with multiple ser
            if (closed) {
                term.close();
                return;
            }
        }
        // 这里注意的是 ShellImpl 构造把命令列表以及内建命令缓存到 session 内存
        // session.readline() 然后就是等待用户命令输入了，如图中$,这里利用了 term 框架封装好的 readline 方法库，同时根据对应的
        // ShellLineHandler 来回调处理相应的命令
        ShellImpl session = createShell(term);
        session.setWelcome(welcomeMessage);
        session.closedFuture.setHandler(new SessionClosedHandler(this, session));
        session.init();
        sessions.put(session.id, session); // Put after init so the close handler on the connection is set
        // 该方法会接着调用 ShellServerImpl 的 handleTerm 方法进行处理，ShellServerImpl 的 handleTerm 方法会调用 ShellImpl的
        // readline 方法，该方法调用注册，ShellLineHandler作为回调类，服务端接收到客户端发送的请求行后，会回调 ShellLineHandler的
        // handler 方法处理请求，readline 方法源码
        session.readline(); // Now readline
    }

    @Override
    public ShellServer listen(final Handler<Future<Void>> listenHandler) {
        final List<TermServer> toStart;
        synchronized (this) {
            if (!closed) {
                throw new IllegalStateException("Server listening");
            }
            toStart = termServers;
        }
        final AtomicInteger count = new AtomicInteger(toStart.size());
        if (count.get() == 0) {
            setClosed(false);
            listenHandler.handle(Future.<Void>succeededFuture());
            return this;
        }
        // 可以看到TermServerListenHandler被实例化，赋值给TermServer对象，然后，TermServer监听，下面给出了 telnet 监听为例
        Handler<Future<TermServer>> handler = new TermServerListenHandler(this, listenHandler, toStart);
        for (TermServer termServer : toStart) {
            // TermImpl 内部首先可以看到对 Function 就是刚才快捷键对应的处理类，下面随便看看一个类，快捷键向上看历史命令
            termServer.termHandler(new TermServerTermHandler(this));
            // 回顾一下最上面的 termServer listen 的时候，termServer.termHandler(new TermServerTermHandler(this))
            // 实例化了 TermServerTermHandler，所以这里执行了 TermServerTermHandler.handle方法
            termServer.listen(handler);
        }
        return this;
    }

    private void evictSessions() {
        long now = System.currentTimeMillis();
        Set<ShellImpl> toClose = new HashSet<ShellImpl>();
        for (ShellImpl session : sessions.values()) {
            // do not close if there is still job running,
            // e.g. trace command might wait for a long time before condition is met
            if (now - session.lastAccessedTime() > timeoutMillis && session.jobs().size() == 0) {
                toClose.add(session);
            }
            logger.debug(session.id + ":" + session.lastAccessedTime());
        }
        for (ShellImpl session : toClose) {
            long timeOutInMinutes = timeoutMillis / 1000 / 60;
            String reason = "session is inactive for " + timeOutInMinutes + " min(s).";
            session.close(reason);
        }
    }

    public synchronized void setTimer() {
        if (!closed && reaperInterval > 0) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            // TODO rename the thread, currently it is `pool-3-thread-1`, which is ambiguous
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    evictSessions();
                }
            }, 0, reaperInterval, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void removeSession(ShellImpl shell) {
        boolean completeSessionClosed;

        Job job = shell.getForegroundJob();
        if (job != null) {
            // close shell's foreground job
            job.terminate();
            logger.info(null, "Session {} closed, so terminate foreground job, id: {}, line: {}",
                        shell.session().getSessionId(), job.id(), job.line());
        }

        synchronized (ShellServerImpl.this) {
            sessions.remove(shell.id);
            completeSessionClosed = sessions.isEmpty() && closed;
        }
        if (completeSessionClosed) {
            sessionsClosed.complete();
        }
    }

    @Override
    public synchronized Shell createShell() {
        return createShell(null);
    }

    @Override
    public synchronized ShellImpl createShell(Term term) {
        if (closed) {
            throw new IllegalStateException("Closed");
        }
        return new ShellImpl(this, term, commandManager, instrumentation, pid, jobController);
    }

    @Override
    public void close(final Handler<Future<Void>> completionHandler) {
        List<TermServer> toStop;
        List<ShellImpl> toClose;
        synchronized (this) {
            if (closed) {
                toStop = Collections.emptyList();
                toClose = Collections.emptyList();
            } else {
                setClosed(true);
                if (scheduledExecutorService != null) {
                    scheduledExecutorService.shutdownNow();
                }
                toStop = termServers;
                toClose = new ArrayList<ShellImpl>(sessions.values());
                if (toClose.isEmpty()) {
                    sessionsClosed.complete();
                }
            }
        }
        if (toStop.isEmpty() && toClose.isEmpty()) {
            completionHandler.handle(Future.<Void>succeededFuture());
        } else {
            final AtomicInteger count = new AtomicInteger(1 + toClose.size());
            Handler<Future<Void>> handler = new SessionsClosedHandler(count, completionHandler);

            for (ShellImpl shell : toClose) {
                shell.close("server is going to shutdown.");
            }

            for (TermServer termServer : toStop) {
                termServer.close(handler);
            }
            jobController.close();
            sessionsClosed.setHandler(handler);
            bootstrap.destroy();
        }
    }
}
