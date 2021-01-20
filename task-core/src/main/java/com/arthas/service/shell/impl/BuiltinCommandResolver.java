package com.arthas.service.shell.impl;


import com.arthas.service.shell.command.Command;
import com.arthas.service.shell.command.CommandBuilder;
import com.arthas.service.shell.command.CommandProcess;
import com.arthas.service.shell.command.CommandResolver;
import com.arthas.service.shell.command.internal.GrepHandler;
import com.arthas.service.shell.command.internal.PlainTextHandler;
import com.arthas.service.shell.command.internal.WordCountHandler;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.handlers.NoOpHandler;

import java.util.Arrays;
import java.util.List;

/**
 * @author beiwei30 on 23/11/2016.
 */
class BuiltinCommandResolver implements CommandResolver {

    private Handler<CommandProcess> handler;

    public BuiltinCommandResolver() {
        this.handler = new NoOpHandler();
    }

    @Override
    public List<Command> commands() {
        return Arrays.asList(CommandBuilder.command("exit").processHandler(handler).build(),
                CommandBuilder.command("quit").processHandler(handler).build(),
                CommandBuilder.command("jobs").processHandler(handler).build(),
                CommandBuilder.command("fg").processHandler(handler).build(),
                CommandBuilder.command("bg").processHandler(handler).build(),
                CommandBuilder.command("kill").processHandler(handler).build(),
                CommandBuilder.command(PlainTextHandler.NAME).processHandler(handler).build(),
                CommandBuilder.command(GrepHandler.NAME).processHandler(handler).build(),
                CommandBuilder.command(WordCountHandler.NAME).processHandler(handler).build());
    }
}
