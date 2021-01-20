package com.arthas.service.shell.handlers.shell;


import com.arthas.service.shell.cli.Completion;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.system.impl.InternalCommandManager;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class CommandManagerCompletionHandler implements Handler<Completion> {
    private InternalCommandManager commandManager;

    public CommandManagerCompletionHandler(InternalCommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void handle(Completion completion) {
        commandManager.complete(completion);
    }
}
