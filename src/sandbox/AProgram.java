package sandbox;

//import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sandbox.StreamRedirector;
import tira.Util;

public abstract class AProgram {
    
    protected String dataDir = ".";
    protected String programDir = ".";
    
    public abstract JSONObject getProgramRecord();
    
    public abstract String getProgramInfo();
    
    public abstract JSONObject getDefaultConfig();
    
    
    public abstract String[] createRuns(JSONObject runConfig);
    
    public abstract JSONArray readRuns(JSONObject runConfig);
    
    public abstract void updateRun(String runId, JSONObject update);
    
    public abstract void deleteRuns(JSONObject runConfig);
    
    public abstract JSONObject takeRun(String runId);
    
    public void execute(String runId) throws JSONException, IOException, InterruptedException
    {
        JSONObject run = takeRun(runId);
        if(run==null){return;}
        String cmd = Util.substitute(run.getString(Util.MAIN), run);
        //run = Util.augment(run, programConfig);
        String runDir =dataDir+"/"+run.getString(Util.ID);
        //write script to working dir.
        File scriptFile = writeCommand(cmd, runDir);
        //start script.
        int exitCode = call(scriptFile,runId);        
        if(exitCode!=0) {updateRun(runId, (new JSONObject()).put(Util.STATE, Util.ERROR));}
        //TODO: read files and update runs accordingly.
        else {updateRun(runId, (new JSONObject()).put(Util.STATE, Util.DONE));}
    }
    
    private File  writeCommand(String cmd, String dir) throws FileNotFoundException {
        File cmdDir = new File(dir); cmdDir.mkdirs();
        File scriptFile = new File(cmdDir,"run.bat");
        PrintWriter pw = new PrintWriter(scriptFile);
        pw.print(cmd); pw.close();
        scriptFile.setExecutable(true);
        return scriptFile;
    }
    
    private int call(File scriptFile, String runId) throws IOException, InterruptedException, JSONException
    {
        ProcessBuilder pb = new ProcessBuilder(scriptFile.getAbsolutePath());
        File pwd = scriptFile.getParentFile();
        pb.directory(pwd);
        System.out.println("[START] " + pb.directory().getAbsolutePath());
        //TODO: write log entries directly to the database via update().
        //logger.write("[START] "+ Util.dateFormat.format(new Date())+"\n");

        /*
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String line;
        while((line=br.readLine())!=null)
        {
            System.out.println(line);
            line.trim();
            if(line.startsWith("{") && line.endsWith("}"))//assume JSONObject-string.
            {
                try {updateRun(runId,new JSONObject(line));
                    
                }catch (JSONException e) {}
            }                
        }
        br.close();
        p.getInputStream().close();
        p.getOutputStream().close();
        */
        // INSERTED //
        final OutputStream stdoutStream =
            new FileOutputStream(new File(pwd, "stdout.txt"));
        final OutputStream stderrStream =
            new FileOutputStream(new File(pwd, "stderr.txt"));
        int exitCode = -1;
        try {
            Process p = pb.start();
            StreamRedirector.redirect(p.getInputStream(),
                stdoutStream);
            StreamRedirector.redirect(p.getErrorStream(),
                System.err,
                stderrStream,
                this.new JSONStreamParser(runId));
            exitCode = p.waitFor();
        } finally {
            stdoutStream.close();
            stderrStream.close();
        }
        System.out.println("[END] " + pb.directory().getAbsolutePath());
        // END INSERTED //
        
        return exitCode;
    }

    /**
     * TODO Describe JSONStreamParser
     *
     * @author johannes.kiesel@uni-weimar.de
     * @version $Id: AProgram.java,v 1.1 2012/07/17 15:02:33 dogu3912 Exp $
     */
    public final class JSONStreamParser
    extends OutputStream {

        private int bracketDepth;

        private final StringBuilder input;

        private final String run;

        public JSONStreamParser(
            final String runId)
        {
            this.bracketDepth = 0;
            this.input = new StringBuilder();
            this.run = runId;
        }

        @Override
        public void write(
            final int b)
        throws IOException
        {
            if (b == '{') {
                ++this.bracketDepth;
            }
            if (this.bracketDepth > 0) {
                this.input.append((char) b);
            }
            if (b == '}') {
                --this.bracketDepth;
                if (this.bracketDepth == 0) {
                    try {
                        final JSONObject json =
                            new JSONObject(this.input.toString());
                        AProgram.this.updateRun(this.run, json);
                        this.input.delete(0, this.input.length());
                    } catch (JSONException je) {
                        throw new IOException(je);
                    }
                }
            }
        }

    }
}
