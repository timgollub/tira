package tira;
//import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer; //If you get a warning here, remove your JRE from the build path and add it back again. Weird but works.

/**
 * @author tim
 */
@Path("/")
public class TiraNode {
    
    //private static JSONObject tiraConfig;
    private static JSONObject system;
    private static TiraApp app;
    private static HttpServer server;
    
    @GET
    public Response getRoot() throws IOException, JSONException {return queryHtml("",null);}   
    
    @GET
    @Path("/programs/{program:[a-zA-Z_0-9\\-\\./]*}")
    @Produces("text/html")
    public Response query(@PathParam("program") String program, @Context UriInfo ui)
            throws JSONException, IOException
    {
        return queryHtml(program,ui);
    }
    
    @GET
    @Path("/programs/{program:[a-zA-Z_0-9\\-\\./]*}.html")
    @Produces("text/html")
    public Response queryHtml(@PathParam("program") String program, @Context UriInfo ui)
            throws JSONException, IOException
    {
        String template = Util.fileToString(new File(system.getString(Util.UI),"tira.html"));
        JSONObject response = new JSONObject().put(Util.RECORD, "undefined")
                                              .put(Util.RESULTS, "undefined")
                                              .put(Util.CONFIG, "undefined")
                                              .put(Util.SCRIPT, "")
                                              .put(Util.INFO, "")//app.getInfo(program))
                                              .put(Util.PNAME, program);
        
        AProgram p = app.getProgram(program);
        if(p!=null){       
	        response.put(Util.RECORD, p.getProgramRecord().toString());
            response.put(Util.INFO, p.getInfo());
	        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
	        if(!queryParams.isEmpty())
	        {
		        JSONObject runConfig = Util.queryParamsToJSON(queryParams);
		        System.out.println(runConfig);
		        response.put(Util.RESULTS, p.readRuns(runConfig).toString());
		        response.put(Util.CONFIG, Util.augment(runConfig, p.getDefaultConfig()).toString());
		        //System.out.println(response.getString(Util.RESULTS));
	        }
	        else{response.put(Util.CONFIG, p.getDefaultConfig().toString());}
	        response.put(Util.SCRIPT, Util.fileToString(new File(system.getString(Util.UI),"tira.js")));
        }
        else {
            String[] programs = app.getProgramsInFolder(program);
            if(programs.length>0)
            {
                StringBuilder html = new StringBuilder("<ul>");
                for(String pname : programs)
                {
                    if(pname.isEmpty()){continue;}
                    //{html.append("<li><a href='/programs/"+pname+"'>/</a></li>");}
                    else
                    {html.append("<li><a href='/programs/"+pname+"'>/"+pname+"</a></li>");}
                }
                html.append("</ul>");
                response.put(Util.INFO, html.toString());
            }
        }
        return Response.status(Status.OK).entity(Util.substitute(template, response)).build();
    }
    
    @GET
    @Path("/programs/{program:[a-zA-Z_0-9\\-\\./]*}.json")
    @Produces("application/json;charset=utf-8")
    public Response queryJson(@PathParam("program") String program, @Context UriInfo ui) throws JSONException
    {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        if(queryParams.isEmpty())
        {
           return Response.status(Status.OK)
                   .entity(app.getProgram(program).getProgramRecord().toString(1))
                   .build(); 
        }
        JSONObject runConfig;
        if(queryParams.containsKey("json")){runConfig = new JSONObject(queryParams.getFirst("json"));}
        else { runConfig = Util.queryParamsToJSON(queryParams);}
        JSONArray searchResult = app.getProgram(program).readRuns(runConfig);
        return Response.status(Status.OK).entity(searchResult.toString(1)).build();
    }
   
    
    @POST
    @Path("/programs/{program:[a-zA-Z_0-9\\-\\./]*}")
    @Produces("text/html")
    public Response executeHtml(@PathParam("program") String program, @Context UriInfo ui,
            MultivaluedMap<String, String> queryParams) throws JSONException, URISyntaxException
    {
        if(!queryParams.isEmpty())
        {
            JSONObject config = Util.queryParamsToJSON(queryParams);
            //System.out.println("EXECUTE: "+config.toString());
            app.getProgram(program).createRuns(config);
        }

        UriBuilder builder = ui.getAbsolutePathBuilder();
        Set<String> keys = queryParams.keySet();
        for(String key: keys)
        {
            List<String> values = queryParams.get(key);
            for(String value : values){builder.queryParam(key, value);}
        }
        System.out.println(builder.build());
        return Response.seeOther(builder.build()).build();
    }
    
    @POST
    @Path("/programs/{program:[a-zA-Z_0-9\\-\\./]*}.json")
    @Produces("application/json;charset=utf-8")
    public Response executeJson(@PathParam("program") String program, @Context UriInfo ui,
            String runConfigString) throws JSONException, URISyntaxException
    {
    	app.getProgram(program).createRuns(new JSONObject(runConfigString));
        return Response.seeOther(ui.getRequestUri()).build();
    }
    
    @PUT
    @Path("/programs/{program:[a-zA-Z_0-9\\-\\./]*}")
    public Response updateRuns(@PathParam("program") String program, @QueryParam("id") String id, String update) throws JSONException
    {
    	if(update==null || update.equals("")){
    		JSONObject runConfig = app.getProgram(program).takeRun(id);
    		return Response.status(Status.OK).entity(runConfig).build();
    	}
    	else {
    		app.getProgram(program).updateRun(id, new JSONObject(update));
    		return Response.status(Status.OK).build();
    	}
    }
    
    @DELETE
    @Path("/programs/{program}")
    public Response deleteRuns()
    {
        return null;
    }
    
    @GET
    @Path("data/{filename:[a-zA-Z_0-9\\-\\./]+}")
    @Produces("text/html")
    public Response getDataFile(@PathParam("filename") String filename) throws JSONException, IOException
    {
        File f = app.readData(filename);
        if(f==null){return Response.status(Status.NOT_FOUND).build();}
        else if(f.isDirectory())
        {
        	String template = Util.fileToString(new File(system.getString(Util.UI),"tira.html"));
       		JSONObject response = new JSONObject().put(Util.RECORD, "undefined")
        	.put(Util.RESULTS, "undefined")
        	.put(Util.CONFIG, "undefined")
        	.put(Util.SCRIPT, "")
        	.put(Util.INFO, "")
        	.put(Util.PNAME, "Run Directory");
       		StringBuilder links = new StringBuilder("<ul>");
        	
        	for(String file : f.list())
        	{
        		String link = "<li><a href='$NODEdata/"+filename+"/"+file+"'>"+file+"</a></li>";
        		link = Util.substitute(link, system);
        		links.append(link);
        	}
        	links.append("</ul>");
        	response.put(Util.INFO, links.toString());
        	return Response.status(Status.OK).entity(Util.substitute(template, response)).build();
        }
        return Response.status(Status.OK).entity(f).type(Util.guessType(f.getName())).build();
    }
    
    @GET
    @Path("ui/{filename:[a-zA-Z_0-9\\-\\./]+}")
    public Response getWebFile(@PathParam("filename") String filename) throws JSONException
    {
        File file = new File(system.getString(Util.UI),filename);
        return Response.status(Status.OK)
                .entity(file)
                .type(Util.guessType(file.getName()))
                .build();
    }
    
//    @POST
//    @Path("/upload/{filename:.+}")
//    @Consumes("application/octet-stream")    
//    public Response upload(InputStream is) throws JSONException
//    {
//        long id = 0;//getUploadID();
//        //String filename = request.getHeader("X-File-Name");
//        JSONObject response = new JSONObject().put("filepath", "$UPLOAD/"+id);
//        try {
//            System.out.println("OUT: "+is.available());
//            FileOutputStream fos = new FileOutputStream(new File(uploadDir,String.valueOf(id)));
//            IOUtils.copy(is, fos);
//            response.put("success", true);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            response.put("success", false);
//        }       
//        return Response.status(Status.OK).entity(response.toString()).type("text/html").build();
//    }
    
    
    public static void init(HttpServer server, JSONObject systemConfig) throws JSONException, InterruptedException, IOException
    {
    	TiraNode.server = server;
        //tiraConfig = tira;
        system = systemConfig;
        app = new TiraApp(system);
        app.loadPrograms();
    }
    
    public TiraApp getApp()
    {
    	return app;
    }
    
    public static void shutdown()
    {
    	app.exit();
    	server.stop(0);
    }

    public static void main(String[] args) throws IllegalArgumentException, IOException,
        URISyntaxException, JSONException, InterruptedException {
        System.out.println("\nName of the OS: " + System.getProperty("os.name"));
        JSONObject systemConfig = 
                new JSONObject(Util.fileToString(new File("system-config.json"))).getJSONObject(Util.SYSTEM);        
        
        //start server.
        ResourceConfig config = new ApplicationAdapter(new Application(){
            @Override
            public Set<Class<?>> getClasses() {
                Set<Class<?>> s = new HashSet<Class<?>>();
                s.add(TiraNode.class); return s;}});
        HttpServer server = 
                HttpServerFactory.create(systemConfig.getString(Util.NODE), config);
        TiraNode.init(server,systemConfig);
        server.start();
        System.out.println("TiraServer started on " + systemConfig.getString(Util.NODE));
        //open browser.
        //if(Desktop.isDesktopSupported()){Desktop.getDesktop().browse(new URI(baseUrl));}
        System.out.println("Enter q to quit.\nTo delete existing program runs in the database, delete respective folders in tira-7/data/ and restart Tira.");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        while ((s = in.readLine()) != null)
        {if(s.equals("q")){System.exit(0);}}
        // An empty line or Ctrl-Z terminates the program
      
    }
}
