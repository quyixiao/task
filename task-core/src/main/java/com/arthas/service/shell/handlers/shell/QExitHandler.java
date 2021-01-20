package com.arthas.service.shell.handlers.shell;

import com.arthas.service.shell.command.CommandProcess;
import com.arthas.service.shell.handlers.Handler;

/**
 *
 * @author hengyunabc 2019-02-09
 *
 */
public class QExitHandler implements Handler<String> {
    private CommandProcess process;

    public QExitHandler(CommandProcess process) {
        this.process = process;
    }

    @Override
    public void handle(String event) {
        if ("q".equalsIgnoreCase(event)) {
            process.end();
        }
    }
}
