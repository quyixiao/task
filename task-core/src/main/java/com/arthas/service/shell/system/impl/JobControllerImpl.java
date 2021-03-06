package com.arthas.service.shell.system.impl;


import com.arthas.service.GlobalOptions;
import com.arthas.service.shell.cli.CliToken;
import com.arthas.service.shell.command.Command;
import com.arthas.service.shell.command.internal.RedirectHandler;
import com.arthas.service.shell.command.internal.StdoutHandler;
import com.arthas.service.shell.command.internal.TermHandler;
import com.arthas.service.shell.future.Future;
import com.arthas.service.shell.handlers.Handler;
import com.arthas.service.shell.impl.ShellImpl;
import com.arthas.service.shell.system.Job;
import com.arthas.service.shell.system.JobController;
import com.arthas.service.shell.system.Process;
import com.arthas.service.shell.term.Term;
import com.arthas.service.utils.Constants;
import io.termd.core.function.Function;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-05-14
 */
public class JobControllerImpl implements JobController {

    private final SortedMap<Integer, JobImpl> jobs = new TreeMap<Integer, JobImpl>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);
    private boolean closed = false;

    public JobControllerImpl() {
    }

    public synchronized Set<Job> jobs() {
        return new HashSet<Job>(jobs.values());
    }

    public synchronized Job getJob(int id) {
        return jobs.get(id);
    }

    synchronized boolean removeJob(int id) {
        return jobs.remove(id) != null;
    }

    @Override
    public Job createJob(InternalCommandManager commandManager, List<CliToken> tokens, ShellImpl shell) {
        int jobId = idGenerator.incrementAndGet();
        StringBuilder line = new StringBuilder();
        for (CliToken arg : tokens) {
            line.append(arg.raw());
        }
        boolean runInBackground = runInBackground(tokens);
        Process process = createProcess(tokens, commandManager, jobId, shell.term());
        process.setJobId(jobId);
        JobImpl job = new JobImpl(jobId, this, process, line.toString(), runInBackground, shell);
        jobs.put(jobId, job);
        return job;
    }

    private int getRedirectJobCount() {
        int count = 0;
        for (Job job : jobs.values()) {
            if (job.process() != null && job.process().cacheLocation() != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void close(final Handler<Void> completionHandler) {
        List<JobImpl> jobs;
        synchronized (this) {
            if (closed) {
                jobs = Collections.emptyList();
            } else {
                jobs = new ArrayList<JobImpl>(this.jobs.values());
                closed = true;
            }
        }
        if (jobs.isEmpty()) {
            if (completionHandler != null) {
                completionHandler.handle(null);
            }
        } else {
            final AtomicInteger count = new AtomicInteger(jobs.size());
            for (JobImpl job : jobs) {
                job.terminateFuture.setHandler(new Handler<Future<Void>>() {
                    @Override
                    public void handle(com.arthas.service.shell.future.Future<Void> v) {
                        if (count.decrementAndGet() == 0 && completionHandler != null) {
                            completionHandler.handle(null);
                        }
                    }
                });
                job.terminate();
            }
        }
    }

    /**
     * Try to create a process from the command line tokens.
     *
     * @param line           the command line tokens
     * @param commandManager command manager
     * @param jobId          job id
     * @param term           term
     * @return the created process
     */
    private Process createProcess(List<CliToken> line, InternalCommandManager commandManager, int jobId, Term term) {
        try {
            ListIterator<CliToken> tokens = line.listIterator();
            while (tokens.hasNext()) {
                CliToken token = tokens.next();
                if (token.isText()) {
                    // 获取到对应的命令管理器
                    Command command = commandManager.getCommand(token.value());
                    if (command != null) {
                        return createCommandProcess(command, tokens, jobId, term);
                    }
                    else {
                        command = commandManager.getCommand("default");
                        if(command !=null){
                            return createCommandProcess(command, tokens, jobId, term);
                        }
                        throw new IllegalArgumentException(token.value() + ": command not found");
                    }
                }
            }
            throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean runInBackground(List<CliToken> tokens) {
        boolean runInBackground = false;
        CliToken last = com.arthas.service.utils.TokenUtils.findLastTextToken(tokens);
        if (last != null && "&".equals(last.value())) {
            runInBackground = true;
            tokens.remove(last);
        }
        return runInBackground;
    }

    private Process createCommandProcess(Command command, ListIterator<CliToken> tokens, int jobId, Term term) throws IOException {
        List<CliToken> remaining = new ArrayList<CliToken>();
        List<CliToken> pipelineTokens = new ArrayList<CliToken>();
        boolean isPipeline = false;
        RedirectHandler redirectHandler = null;
        List<Function<String, String>> stdoutHandlerChain = new ArrayList<Function<String, String>>();
        String cacheLocation = null;
        while (tokens.hasNext()) {
            CliToken remainingToken = tokens.next();
            if (remainingToken.isText()) {
                String tokenValue = remainingToken.value();
                if ("|".equals(tokenValue)) {
                    isPipeline = true;
                    // 将管道符|之后的部分注入为输出链上的handler
                    injectHandler(stdoutHandlerChain, pipelineTokens);
                    continue;
                } else if (">>".equals(tokenValue) || ">".equals(tokenValue)) {
                    String name = getRedirectFileName(tokens);
                    if (name == null) {
                        // 如果没有指定重定向文件名，那么重定向到以jobid命名的缓存中
                        name = Constants.CACHE_ROOT + File.separator + Constants.PID + File.separator + jobId;
                        cacheLocation = name;

                        if (getRedirectJobCount() == 8) {
                            throw new IllegalStateException("The amount of async command that saving result to file can't > 8");
                        }
                    }
                    redirectHandler = new RedirectHandler(name, ">>".equals(tokenValue));
                    break;
                }
            }
            if (isPipeline) {
                pipelineTokens.add(remainingToken);
            } else {
                remaining.add(remainingToken);
            }
        }
        injectHandler(stdoutHandlerChain, pipelineTokens);
        if (redirectHandler != null) {
            stdoutHandlerChain.add(redirectHandler);
        } else {
            stdoutHandlerChain.add(new TermHandler(term));
            if (GlobalOptions.isSaveResult) {
                stdoutHandlerChain.add(new RedirectHandler());
            }
        }
        ProcessImpl.ProcessOutput ProcessOutput = new ProcessImpl.ProcessOutput(stdoutHandlerChain, cacheLocation, term);
        return new ProcessImpl(command, remaining, command.processHandler(), ProcessOutput);
    }

    private String getRedirectFileName(ListIterator<CliToken> tokens) {
        while (tokens.hasNext()) {
            CliToken token = tokens.next();
            if (token.isText()) {
                return token.value();
            }
        }
        return null;
    }

    private void injectHandler(List<Function<String, String>> stdoutHandlerChain, List<CliToken> pipelineTokens) {
        if (!pipelineTokens.isEmpty()) {
            StdoutHandler handler = StdoutHandler.inject(pipelineTokens);
            if (handler != null) {
                stdoutHandlerChain.add(handler);
            }
            pipelineTokens.clear();
        }
    }

    @Override
    public void close() {
        close(null);
    }
}
