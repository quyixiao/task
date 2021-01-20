package com.arthas.service.shell.handlers.server;

import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.impl.ShellServerImpl;
import com.arthas.service.shell.term.Term;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class TermServerTermHandler implements Handler<Term> {
    private ShellServerImpl shellServer;

    public TermServerTermHandler(ShellServerImpl shellServer) {
        this.shellServer = shellServer;
    }

    @Override
    public void handle(Term term) {
        shellServer.handleTerm(term);
    }
}
