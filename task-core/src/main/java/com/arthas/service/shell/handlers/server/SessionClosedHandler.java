package com.arthas.service.shell.handlers.server;


import com.arthas.service.shell.future.Future;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.impl.ShellImpl;
import com.arthas.service.shell.impl.ShellServerImpl;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class SessionClosedHandler implements Handler<Future<Void>> {
    private ShellServerImpl shellServer;
    private final ShellImpl session;

    public SessionClosedHandler(ShellServerImpl shellServer, ShellImpl session) {
        this.shellServer = shellServer;
        this.session = session;
    }

    @Override
    public void handle(Future<Void> ar) {
        shellServer.removeSession(session);
    }
}
