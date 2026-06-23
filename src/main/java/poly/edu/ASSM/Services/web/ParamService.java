package poly.edu.ASSM.Services.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.method.ParameterErrors;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ParamService {
	@Autowired
	HttpServletRequest request;
	
	//String
	public String getString(String name, String defaultValue) {
	    String p = request.getParameter(name);
	    if (p == null) return defaultValue;
	    return p;
	}
	//Integer
  public int getInt(String name,int defaultValue) {
	  String p=request.getParameter(name);
	  if(p==null) return defaultValue;
	  return Integer.parseInt(p);
  }
  //Double
  public double getDouble(String name,double defaultValue) {
	  String p=request.getParameter(name);
	  if(p==null) return defaultValue;
	  return Double.parseDouble(p);
	  
  }
  //Boolean
  public Boolean getBoolean(String name,Boolean defaultValue) {
	  String p=request.getParameter(name);
	  if(p==null) return defaultValue;
	  return Boolean.parseBoolean(p);
  }
  //Date
  public Date getDate(String name,String pattern) throws ParseException, RuntimeException {
	  String p=request.getParameter(name);
	  if(p==null) return null;
	  SimpleDateFormat sf=new SimpleDateFormat();
	  return sf.parse(p);
  }
}
