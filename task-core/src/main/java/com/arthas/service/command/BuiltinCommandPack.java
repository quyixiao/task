package com.arthas.service.command;


import com.arthas.service.command.basic1000.HelpCommand;
import com.arthas.service.shell.command.Command;
import com.arthas.service.shell.command.CommandResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO automatically discover the built-in commands.
 * @author beiwei30 on 17/11/2016.
 */
public class BuiltinCommandPack implements CommandResolver {

    private static List<Command> commands = new ArrayList<Command>();

    static {
        initCommands();
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    private static void initCommands() {
        commands.add(Command.create(HelpCommand.class));

    }
}
