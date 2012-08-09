package sandbox;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TODO Describe JSONStreamParser
 *
 * @author johannes.kiesel@uni-weimar.de
 * @version $Id: JSONStreamParser.java,v 1.1 2012/07/11 19:31:45 dogu3912 Exp $
 */
public final class JSONStreamParser
extends OutputStream {

    private int bracketDepth;

    private final StringBuilder input;

    private final List<JSONObject> objects;

    public JSONStreamParser()
    {
        this.bracketDepth = 0;
        this.input = new StringBuilder();
        this.objects = new ArrayList<JSONObject>();
    }

    public List<JSONObject> popObjects()
    {
        final List<JSONObject> objectsCopy;
        synchronized (this.objects) {
            objectsCopy = new ArrayList<JSONObject>(this.objects);
            this.objects.clear();
        }
        return objectsCopy;
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
                    synchronized (this.objects) {
                        this.objects.add(new JSONObject(this.input.toString()));
                    }
                    this.input.delete(0, this.input.length());
                } catch (JSONException je) {
                    throw new IOException(je);
                }
            }
        }
    }

}
