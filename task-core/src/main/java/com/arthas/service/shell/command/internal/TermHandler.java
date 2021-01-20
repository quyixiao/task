package com.arthas.service.shell.command.internal;


import com.arthas.service.shell.term.Term;

/**
 * 将数据写到term
 *
 * @author gehui 2017年7月26日 上午11:20:00
 */
public class TermHandler extends StdoutHandler {
    private Term term;

    public TermHandler(Term term) {
        this.term = term;
    }

    @Override
    public String apply(String data) {
        term.write(data);
        return data;
    }
}
