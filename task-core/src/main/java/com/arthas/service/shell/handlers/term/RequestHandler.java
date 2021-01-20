package com.arthas.service.shell.handlers.term;

import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.term.impl.TermImpl;
import io.termd.core.function.Consumer;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class RequestHandler implements Consumer<String> {
    private TermImpl term;
    private final Handler<String> lineHandler;

    public RequestHandler(TermImpl term, Handler<String> lineHandler) {
        this.term = term;
        this.lineHandler = lineHandler;
    }

    @Override
    public void accept(String line) {
        term.setInReadline(false);
        System.out.println(" line = "+ line);
        lineHandler.handle(line);
    }
}
