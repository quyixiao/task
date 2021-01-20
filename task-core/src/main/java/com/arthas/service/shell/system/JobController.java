package com.arthas.service.shell.system;

import com.arthas.service.shell.cli.CliToken;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.impl.ShellImpl;
import com.arthas.service.shell.system.impl.InternalCommandManager;

import java.util.List;
import java.util.Set;

/**
 * The job controller.<p/>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface JobController {

    /**
     * @return the active jobs
     */
    Set<Job> jobs();

    /**
     * Returns an active job in this session by its {@literal id}.
     *
     * @param id the job id
     * @return the job of {@literal null} when not found
     */
    Job getJob(int id);

    /**
     * Create a job wrapping a process.
     *
     * @param commandManager command manager
     * @param tokens    the command tokens
     * @param shell     the current shell
     * @return the created job
     */
    Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, ShellImpl shell);

    /**
     * Close the controller and terminate all the underlying jobs, a closed controller does not accept anymore jobs.
     */
    void close(Handler<Void> completionHandler);

    /**
     * Close the shell session and terminate all the underlying jobs.
     */
    void close();

}
