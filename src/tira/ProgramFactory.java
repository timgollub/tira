package tira;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class ProgramFactory {
    
    public static AProgram createProgram(JSONObject programRecord) throws JSONException, IOException
    {
    	if(programRecord.has(Util.DATABASE))
    	{
    		System.out.println("Remote Program:");
    		return new RemoteProgram(programRecord);
    	}
        return new LocalProgram(programRecord);
    }

}
