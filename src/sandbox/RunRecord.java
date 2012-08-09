package sandbox;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;

import tira.AProgram;
import tira.TiraApp;
import tira.TiraNode;
import tira.Util;

/**
 * TODO Describe RunRecord
 *
 * @author johannes.kiesel@uni-weimar.de
 * @version $Id: RunRecord.java,v 1.11 2012/08/01 16:13:44 gollub Exp $
 */
public class RunRecord {

    private final TiraApp app;
    private final List<Run> runs;

    private RunRecord(
        final String tiraConfigFileName)
    throws JSONException, IOException, InterruptedException
    {
        final JSONObject tiraConfig =
            new JSONObject(Util.fileToString(new File(tiraConfigFileName)));
        ResourceConfig config = new ApplicationAdapter(new Application(){
            @Override
            public Set<Class<?>> getClasses() {
                Set<Class<?>> s = new HashSet<Class<?>>();
                s.add(TiraNode.class); return s;}});
        HttpServer server = 
                HttpServerFactory.create(
                    tiraConfig.getString(Util.NODE), config);
        TiraNode.init(server,tiraConfig);
        server.start();
        this.app = new TiraNode().getApp();
        //parse record folder and load programs.
        //this.app.loadPrograms();
        this.runs = new LinkedList<Run>();
    }

    private boolean run(
        final Scanner scanner)
    throws JSONException, IOException, InterruptedException
    {
        final Run run = this.parseProgram(scanner);
        if (run == null) { return false; }
        final AProgram program = run.program;
        final JSONObject runConfig = RunRecord.parseRunConfig(scanner);
        if (runConfig == null) { return false; }
        final String[] ids = program.createRuns(runConfig);
        for (String id : ids)
        {
            final Run r = new Run(program, run.programName);
            r.id = id;
            this.runs.add(r);
        }
        return true;
    }

    private Run parseProgram(
        final Scanner scanner)
    {
        System.out.println("Enter program name or \"quit\":");
        while (scanner.hasNextLine())
        {
            final String line = scanner.nextLine();
            if (line.equals("quit") || line.equals("exit")) {
                System.out.println("Exiting");
                return null;
            } else {
                final AProgram program =
                    this.app.getProgram(line);
                if (program == null)
                {
                    System.err.println("No such program: " + line);
                    System.out.println("Enter program name or \"quit\":");
                } else {
                    return new Run(program, line);
                }
            }
        }
        return null;
    }

    private static JSONObject parseRunConfig(
        final Scanner scanner)
    throws IOException
    {
        System.out.println("Enter JSON run configuration:");
        int bracketDepth = 0;
        final StringBuilder input = new StringBuilder();
        while (scanner.hasNextLine())
        {
            for (char character : scanner.nextLine().toCharArray()) {
                if (character == '{') {
                    ++bracketDepth;
                }
                if (bracketDepth > 0) {
                    input.append(character);
                }
                if (character == '}') {
                    --bracketDepth;
                    if (bracketDepth == 0) {
                        try {
                            return new JSONObject(input.toString());
                        } catch (JSONException je) {
                            throw new IOException(je);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param args
     * @throws IOException 
     * @throws JSONException 
     * @throws InterruptedException 
     */
    public static void main(
        final String[] args)
    throws JSONException, IOException, InterruptedException
    {
        if (args.length != 1)
        {
            System.err.println("Usage: RunRecord <tira.json>");
            System.exit(1);
        }
        final RunRecord program = new RunRecord(args[0]);
        final Scanner scanner = new Scanner(System.in);
        while (program.run(scanner));
        while (!program.runs.isEmpty())
        {
            System.out.println("--- Runs ---");
            final Iterator<Run> runs = program.runs.iterator();
            while (runs.hasNext())
            {
                final Run run = runs.next();
                if (run.isFinished()) {
                    runs.remove();
                }
            }
            System.out.println();
            Thread.sleep(5000);
        }
        //TiraNode.shutdown();
    }

    private class Run {
        private final AProgram program;
        private final String programName;
        private String id;
        private String lastState;
        private boolean finished;

        private Run(
            final AProgram program,
            final String name)
        {
            this.program = program;
            this.programName = name;
            this.id = "";
            this.lastState = Util.TODO;
            this.finished = false;
        }

        private boolean isFinished()
        throws JSONException
        {
            if (this.finished) { return true; }
            final JSONArray jsons =
                this.program.readRuns(
                    new JSONObject("{ID:\"" + this.id + "\"}"));
            if (jsons.length() > 0)
            {
                final String state = 
                    jsons.getJSONObject(0).getString(Util.STATE);
                System.out.println(
                    this.programName + " [" + this.id + "] is " + state);
                if (this.lastState.equals(state))
                {
                    return false;
                } else {
                    this.lastState = state;
                    if (!state.equals(Util.TODO) && !state.equals(Util.RUNNING))
                    {
                        this.finished = true;
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
    }

}
