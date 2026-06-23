package poly.edu.ASSM.Services.web;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Service
public class CookieService {
 @Autowired HttpServletRequest req;
 @Autowired HttpServletResponse resp;
 public Cookie createCookie(String name,String value,int days) {
	 Cookie cookie = new Cookie(name,value);
	 cookie.setMaxAge(days *24*60*60);
	 cookie.setPath("/");
	 resp.addCookie(cookie);
	 return cookie;
 }
 public Cookie getCookie(String name) {
	 Cookie[] cookies=req.getCookies();
	 if(cookies!=null) {
		 for(Cookie cookie:cookies) {
			 if(cookie.getName().equalsIgnoreCase(name)){
				 return cookie;
			 }			 
		 }
		
	 }
	 return null;
 }
 public String getValue(String name) {
	    Cookie cookie = getCookie(name);
	    if (cookie != null) {
	        return cookie.getValue();
	    }
	    return "";
	}
 public void removeCookie(String name) {
	    Cookie cookie = new Cookie(name, "");
	    cookie.setMaxAge(0);
	    cookie.setPath("/");
	    resp.addCookie(cookie);
	}
}
