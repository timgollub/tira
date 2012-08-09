package tira;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class RemoteProgram extends AProgram{
    
    private WebResource database;
    private JSONObject baseConfig = new JSONObject();
    
    public RemoteProgram(JSONObject programRecord) throws JSONException
    {
        String databaseUrl = programRecord.getString(Util.DATABASE);
        Client c = Client.create();
        database = c.resource(databaseUrl);
    }

    @Override
    public JSONObject getProgramRecord() {        
		try {
			JSONObject record = new JSONObject(database.get(String.class));
			initBaseConfig(record);
			return record;
		} 
		catch (UniformInterfaceException e) {e.printStackTrace();}
		catch (ClientHandlerException e) {e.printStackTrace();}
		catch (JSONException e) {e.printStackTrace();}
		return null;
    }
    
    private void initBaseConfig(JSONObject record) throws JSONException
    {
        @SuppressWarnings("unchecked")
        Iterator<String> keyIter = record.keys();
        while(keyIter.hasNext()) {
            String key = keyIter.next();
            if(record.get(key) instanceof String && key.toUpperCase().equals(key)) 
                {baseConfig.put(key, record.get(key));}
            else if(record.get(key) instanceof JSONObject &&
                     record.getJSONObject(key).has(Util.VALUE))
                {baseConfig.put(key, record.getJSONObject(key).getString(Util.VALUE));}
        }
    }

    @Override
    public JSONObject getDefaultConfig() {
        return baseConfig;
    }

    @Override
    public String[] createRuns(JSONObject runConfig) {
        try {
			database.post(String.class,runConfig.toString());
		}
        catch (UniformInterfaceException e) {e.printStackTrace();}
		catch (ClientHandlerException e) {e.printStackTrace();}
		return null;
    }

    @Override
    public JSONArray readRuns(JSONObject runConfig) {
        try {
			return new JSONArray(database.queryParam("json", runConfig.toString()).get(String.class));
		}
        catch (UniformInterfaceException e) {e.printStackTrace();}
		catch (ClientHandlerException e) {e.printStackTrace();}
		catch (JSONException e) {e.printStackTrace();}		
		return new JSONArray();
    }

    @Override
    public void updateRun(String runId, JSONObject update) {
    	try {
			database.queryParam("id", runId).put(String.class, update.toString());
		} 
        catch (UniformInterfaceException e) {e.printStackTrace();}
		catch (ClientHandlerException e) {e.printStackTrace();}
    }

    @Override
    public void deleteRuns(JSONObject runConfig) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public JSONObject takeRun(String runId) {
    	try {
			return new JSONObject(database.queryParam("id", runId).put(String.class));
		} 
        catch (UniformInterfaceException e) {e.printStackTrace();}
		catch (ClientHandlerException e) {e.printStackTrace();}
		catch (JSONException e) {System.out.println("Run "+runId+" already taken");}		
        return null;
    }

}
