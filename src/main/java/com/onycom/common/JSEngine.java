package com.onycom.common;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class JSEngine {
	@SuppressWarnings("restriction")
	ScriptEngine mJsEngine;
	
	@SuppressWarnings("restriction")
	public Object runJavaScript(String script){
		if (mJsEngine == null) mJsEngine = new ScriptEngineManager().getEngineByName("JavaScript");
		Object ret = null;
		try {
			ret = mJsEngine.eval(script);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		return ret;
	}
}
