package com.arthas.service.shell.handlers.command;


import com.arthas.service.shell.command.CommandProcess;
import com.arthas.service.shell.handlers.Handler;

/**
 * @author ralf0131 2017-01-09 13:23.
 */
public class CommandInterruptHandler implements Handler<Void> {

    private CommandProcess process;

    public CommandInterruptHandler(CommandProcess process) {
        this.process = process;
    }

    @Override
    public void handle(Void event) {
        process.end();
        process.session().unLock();
    }
}
