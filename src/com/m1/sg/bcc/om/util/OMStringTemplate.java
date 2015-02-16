package com.m1.sg.bcc.om.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OMStringTemplate {
	
    final private Matcher m;
    static final private Pattern keyPattern = 
        Pattern.compile("\\$\\{([a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*)\\}");
    private boolean blanknull=false;

    public OMStringTemplate(String template) { 
        this.m = keyPattern.matcher(template);
    }

    /**
     * @param map substitution map
     * @return substituted string
     */
    public String substitute(Map<String, ? extends Object> map)
    {
        this.m.reset();
        StringBuffer sb = new StringBuffer();
        while (this.m.find())
        {
            String k0 = this.m.group();
            String k = this.m.group(1);
            Object vobj = map.get(k);
            String v = (vobj == null) 
                ? (this.blanknull ? "" : k0)
                : vobj.toString();
            this.m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        this.m.appendTail(sb);
        return sb.toString();       
    }

    public OMStringTemplate setBlankNull()
    {
        this.blanknull=true;
        return this;
    }
    
    public boolean checkPattern(){
    	if(this.m.find()){return true;}
    	else {return false;}
    }
}
