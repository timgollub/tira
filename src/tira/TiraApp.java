package tira;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * Execution manager.
 * Schedules threaded execution of experiment runs. 
 * 
 * @author tim
 *
 */
public class TiraApp {
    
    //private JSONObject tiraConfig;
    private JSONObject system;
    private Map<String,AProgram> programs = new HashMap<String,AProgram>();
    private BlockingQueue<AProgram> executionQueue = new LinkedBlockingQueue<AProgram>();
    private BlockingQueue<AProgram> waitingQueue = new LinkedBlockingQueue<AProgram>();
    private String programPath;
    private String dataRoot;
    private boolean EXIT = false;
    
    
    public TiraApp(JSONObject systemConfig) throws JSONException
    {
        
        //this.tiraConfig = tiraConfig;
        this.system = systemConfig;
        this.programPath = Util.substitute(system.getString(Util.PROGRAMROOT), system);
        this.dataRoot = Util.substitute(system.getString(Util.DATAROOT), system);
        startWaitingQueue();
        startExecutionQueue(system.getInt(Util.WORKER));        
    }
    
    private void startWaitingQueue() {
        new Thread(new Runnable() {            
            public void run() {
                while(true){try{
                    AProgram program = waitingQueue.take();
                    Thread.sleep(500);
                    executionQueue.put(program);}
                catch (InterruptedException e) {e.printStackTrace();}
            }}}).start();           
    }

    private void startExecutionQueue(int numWorker) throws JSONException {
        final JSONObject pendingRuns = (new JSONObject()).put(Util.STATE, Util.TODO);
        for(int i=0; i<numWorker; ++i)
        {
            new Thread(new Runnable() {
                public void run() {
                    while(true){
                        try {
                            if(EXIT) {break;}
                            AProgram program = executionQueue.take();                            
                            JSONArray runs = program.readRuns(pendingRuns);
                            if(runs.length()==0){waitingQueue.put(program);}
                            else
                            {
                                executionQueue.put(program);
                                for(int i=0; i<runs.length(); ++i)
                                {
                                    program.execute(runs.getJSONObject(i).getString(Util.ID));
                                }
                            }
                        }
                        catch (InterruptedException e) {break;}
                        catch (JSONException e) {e.printStackTrace();}
                        catch (IOException e) {e.printStackTrace();}
                    }
                }

            }).start();
        }       
    }

    public AProgram getProgram(String programName)
    {
        return programs.get(programName);
    }
    
    public String getInfo(String program) {
        try {            
            File file = new File(programPath,program+"/info.html");
            return Util.fileToString(file);
        }
        catch(Exception e) {return "";}
    }
    
    
    public File readData(String filepath)
    {
        File file = new File(dataRoot, filepath);
        if(file.exists()){return file;}
        return null;
    }
    
    public void loadPrograms() throws JSONException, InterruptedException, IOException
    {
        File pDir = new File(programPath);
        loadPrograms(pDir,pDir,new JSONObject());
    }
    
    private void loadPrograms(File programRoot, File programDir, JSONObject baseRecord) throws InterruptedException, JSONException, IOException {
        File recordFile = new File(programDir,"record.json");
        JSONObject programRecord;
        if(recordFile.exists())
        {
            programRecord = new JSONObject(Util.fileToString(recordFile));
            Util.augmentDeep(programRecord,baseRecord);
            if(programRecord.has(Util.MAIN)||programRecord.has(Util.DATABASE))
            {
                String programName = programDir.getPath().replaceFirst(programRoot.getPath(), "").substring(1).replace("\\", "/");
                programRecord.getJSONObject("SYSTEM").put(Util.PNAME, programName);
                AProgram program = ProgramFactory.createProgram(programRecord);                    
                programs.put(programName, program);
                if(programRecord.has(Util.MAIN)){executionQueue.put(program);}
                System.out.println(programName+" loaded.");
            }
            //System.out.println(programRecord.toString(1));
        }
        else {programRecord = new JSONObject(baseRecord.toString());}
              
        for(File dir : programDir.listFiles())
        {
            if(dir.isDirectory()){loadPrograms(programRoot,dir,programRecord);}        
        }      
    }

    public void exit(){EXIT=true;}

    public static void main(String[] args) throws JSONException, IOException, InterruptedException {
        //load tira config.
        JSONObject tiraConfig = new JSONObject(Util.fileToString(new File("programs/record.json")));
        TiraApp app = new TiraApp(tiraConfig);
        //parse record folder and load programs.
        app.loadPrograms();
        String text = java.util.UUID.randomUUID().toString();
        JSONObject runConfig = (new JSONObject("{other:[\"?!\",\"!!\"]}")).put("text", text).put("punct", "$other");
        //JSONObject runConfig = (new JSONObject()).put("dir", "~/code-in-progress/tira/tira-7/mini_corpus").put("dataset", "01");
        app.getProgram("examples/echo").createRuns(runConfig);
        //app.getProgram("gillam12").createRuns(runConfig);

        //TODO: terminate on poisson pill.
        //Thread.sleep(10000);
        //app.exit();
    }
}
