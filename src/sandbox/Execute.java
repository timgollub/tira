package sandbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.json.JSONObject;

/**
 * TODO Describe Execute
 *
 * @author johannes.kiesel@uni-weimar.de
 * @version $Id: Execute.java,v 1.1 2012/07/11 19:31:45 dogu3912 Exp $
 */
public class Execute {

    private static String getCommand(
        final String[] args)
    {
        // Build command from args
        final StringBuilder commandBuilder = new StringBuilder();
        for (String arg : args) {
            commandBuilder.append(arg).append(' ');
        }
        // Remove last space
        commandBuilder.setLength(commandBuilder.length() - 1);
        return commandBuilder.toString();
    }

    private static File writeScript(
        final String command,
        final String scriptName,
        final File directory)
    throws IOException
    {
        final File file = new File(directory, scriptName);
        final FileWriter drain = new FileWriter(file);
        try {
            drain.write(command);
            if (!file.setExecutable(true)) {
                throw new IOException(
                    "Could not set " + file + " to be executable");
            }
        } catch (IOException io) {
            file.delete();
            throw io;
        } finally {
            drain.close();
        }
        return file;
    }

    private static int executeScript(
        final File script,
        final JSONStreamParser jsonParser)
    throws IOException, InterruptedException
    {
        final ProcessBuilder processBuilder = new ProcessBuilder(
            script.getAbsolutePath());
        processBuilder.directory(script.getParentFile());
        final Process run = processBuilder.start();
        StreamRedirector.redirect(run.getInputStream(), System.out);
        StreamRedirector.redirect(run.getErrorStream(), System.err, jsonParser);
        StreamRedirector.redirect(System.in, run.getOutputStream());
        return run.waitFor();
    }

    /**
     * @param args
     * @throws IOException 
     * @throws InterruptedException 
     */
    public static void main(
        final String[] args)
    throws IOException, InterruptedException
    {
        if (args.length == 0) {
            System.err.println("Usage: Execute <program code>");
            System.exit(1);
        }

        final String command = Execute.getCommand(args);
        final File workingDirectory = new File(".");
        final String scriptName = "script.bat";

        final File script =
            Execute.writeScript(command, scriptName, workingDirectory);

        int exitValue = 0;
        final JSONStreamParser jsonParser = new JSONStreamParser();
        try {
            exitValue = Execute.executeScript(script, jsonParser);
        } finally {
            script.delete();
        }

        final List<JSONObject> jsons = jsonParser.popObjects();
        System.out.println("--- JSON: " + jsons.size() + " objects found ---");
        for (JSONObject json : jsons) {
            System.out.println(json.toString());
        }

        System.exit(exitValue);
    }

}
