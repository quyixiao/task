package com.arthas.service.shell.term.impl;


import com.arthas.service.shell.cli.CliToken;
import com.arthas.service.shell.cli.CliTokens;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.session.Session;
import com.arthas.service.utils.LogUtil;
import com.taobao.middleware.logger.Logger;
import io.termd.core.function.Consumer;
import io.termd.core.readline.Completion;

import java.util.Collections;
import java.util.List;


/**
 * @author beiwei30 on 23/11/2016.
 */
class CompletionHandler implements Consumer<Completion> {
    private static final Logger logger = LogUtil.getArthasLogger();
    private final Handler<com.arthas.service.shell.cli.Completion> completionHandler;
    private final Session session;

    public CompletionHandler(Handler<com.arthas.service.shell.cli.Completion> completionHandler, Session session) {
        this.completionHandler = completionHandler;
        this.session = session;
    }

    @Override
    public void accept(final  io.termd.core.readline.Completion completion) {
        try {
            final String line = io.termd.core.util.Helper.fromCodePoints(completion.line());
            final List<CliToken> tokens = Collections.unmodifiableList(CliTokens.tokenize(line));
            com.arthas.service.shell.cli.Completion comp = new CompletionAdaptor(line, tokens, completion, session);
            completionHandler.handle(comp);
        } catch (Throwable t) {
            // t.printStackTrace();
            logger.error(null, "completion error", t);
        }
    }
}
