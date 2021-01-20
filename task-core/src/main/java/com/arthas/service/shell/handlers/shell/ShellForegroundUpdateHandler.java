package com.arthas.service.shell.handlers.shell;


import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.impl.ShellImpl;
import com.arthas.service.shell.system.Job;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class ShellForegroundUpdateHandler implements Handler<Job> {
    private ShellImpl shell;

    public ShellForegroundUpdateHandler(ShellImpl shell) {
        this.shell = shell;
    }

    @Override
    public void handle(Job job) {
        if (job == null) {
            shell.readline();
        }
    }
}
