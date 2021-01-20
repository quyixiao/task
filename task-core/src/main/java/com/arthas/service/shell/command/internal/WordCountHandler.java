package com.arthas.service.shell.command.internal;

import com.arthas.service.shell.cli.CliToken;
import com.taobao.middleware.cli.CLIs;
import com.taobao.middleware.cli.CommandLine;
import com.taobao.middleware.cli.Option;

import java.util.List;

/**
 * @author ralf0131 2017-02-23 23:28.
 */
public class WordCountHandler extends StdoutHandler implements StatisticsFunction {

    public static final String NAME = "wc";

    private boolean lineMode;

    private String result = null;
    private volatile int total = 0;

    public static StdoutHandler inject(List<CliToken> tokens) {
        List<String> args = StdoutHandler.parseArgs(tokens, NAME);
        CommandLine commandLine = CLIs.create(NAME)
                .addOption(new Option().setShortName("l").setFlag(true))
                .parse(args);
        Boolean lineMode = commandLine.isFlagEnabled("l");
        return new WordCountHandler(lineMode);
    }

    private WordCountHandler(boolean lineMode) {
        this.lineMode = lineMode;
    }

    @Override
    public String apply(String input) {
        if (!this.lineMode) {
            // TODO the default behavior should be equivalent to `wc -l -w -c`
            result = "wc currently only support wc -l!\n";
        } else {
            if (input != null && !"".equals(input.trim())) {
                total += input.split("\n").length;
            }
        }

        return null;
    }

    @Override
    public String result() {
        if (result != null) {
            return result;
        }

        return total + "\n";
    }
}
