package uk.ac.ox.it;

import picocli.CommandLine;
import picocli.CommandLine.AbstractParseResultHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.DefaultExceptionHandler;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.RunLast;

import java.util.List;

import static picocli.CommandLine.defaultExceptionHandler;

@Command(name = "imagecheck", mixinStandardHelpOptions = true, subcommands = {HelpCommand.class, ImageRestore.class, ImageCheck.class}, description = "Processes course images in canvas")
public class Entrypoint {

    @Option(names = {"--debug"}, description = "Output debugging information")
    boolean debug;

    public static void main(String... args) {
        Entrypoint entrypoint = new Entrypoint();
        CommandLine cmd = new CommandLine(entrypoint);
        DefaultExceptionHandler<List<Object>> exceptionHandler = defaultExceptionHandler().andExit(1);
        AbstractParseResultHandler<List<Object>> handler = new RunLast().andExit(0);
        try {
            ParseResult parse = cmd.parseArgs(args);
            if (!parse.hasSubcommand()) {
                cmd.usage(System.out);
            } else {
                handler.handleParseResult(parse);
            }
        } catch (ParameterException ex) {
            exceptionHandler.handleParseException(ex, args);
        } catch (ExecutionException ex) {
            if (!entrypoint.debug && ex.getCause() instanceof ImageCheck.MessageException) {
                System.err.println("Error: " + ex.getCause().getMessage());
            } else {
                throw ex;
            }
        }
    }

}
