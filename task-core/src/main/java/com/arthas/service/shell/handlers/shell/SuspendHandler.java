package com.arthas.service.shell.handlers.shell;


import com.arthas.service.shell.impl.ShellImpl;
import com.arthas.service.shell.system.ExecStatus;
import com.arthas.service.shell.system.Job;
import com.arthas.service.shell.term.SignalHandler;
import com.arthas.service.shell.term.Term;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class SuspendHandler implements SignalHandler {

    private ShellImpl shell;

    public SuspendHandler(ShellImpl shell) {
        this.shell = shell;
    }

    @Override
    public boolean deliver(int key) {
        Term term = shell.term();

        Job job = shell.getForegroundJob();
        if (job != null) {
            term.echo(shell.statusLine(job, ExecStatus.STOPPED));
            job.suspend();
        }

        return true;
    }
}
