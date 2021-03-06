package com.cnnic.whois.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cnnic.whois.execption.QueryException;
import com.cnnic.whois.util.DataFormat;
import com.cnnic.whois.util.WhoisUtil;

public class ErrorFilter implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1,
			FilterChain chain) throws IOException, ServletException {
		// TODO Need Test
		
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;
		
		String userAgent = "";
		try{
			userAgent = request.getHeader("user-agent").toLowerCase();
		}
		catch(Exception e){
			userAgent = "";
		}		

		String format = WhoisUtil.getFormatCookie(request);		
		String role = WhoisUtil.getUserRole(request);

		CharSequence ie = "msie";
		CharSequence firefox = "firefox";
		CharSequence chrome = "chrome";
		CharSequence safiri = "safiri";
		CharSequence opera = "opera";   
		if (format == null && (userAgent.contains(ie) || userAgent.contains(firefox) ||
				userAgent.contains(chrome) || userAgent.contains(safiri) || userAgent.contains(opera)))
			format = "application/html";
		if (format == null){
			format = request.getHeader("Accept");	
			if (format == null){
				format = "application/json";
			}

			CharSequence sqhtml = "html";			
			if(format.contains(sqhtml))
				format = "application/html";
		}
		if(format == null || !(format.equals("application/html") || format.equals("application/json") || format.equals("application/xml") || format.endsWith("application/rdap+json")
				|| format.equals("application/text") || format.equals("application/rdap+json;application/json"))){
			format = "application/html";
		}
		
		String queryInfo = "";
		String queryType = "";
		
		String path = request.getRequestURI();
		
		if(!path.equals("")){
			queryInfo = path.substring(request.getContextPath().length() + 1);
			
			if(queryInfo.equals("") && (userAgent.contains(ie) || userAgent.contains(firefox) ||
					userAgent.contains(chrome) || userAgent.contains(safiri) || userAgent.contains(opera))){
				format = "application/html";
				WhoisUtil.clearFormatCookie(request, response);
			}
			if(queryInfo.indexOf("/") != -1){				
				queryType = queryInfo.substring(0, queryInfo.indexOf("/"));
			}else{
				queryType = queryInfo;//domains?name=a*.cn ->domains
			}
		}
		
		displayErrorMessage(request, response, chain, format, queryType, role);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}
	
	
	private boolean isLegalType(String queryType){
		if(queryType.equals(WhoisUtil.FUZZY_DOMAINS) ||
				queryType.equals(WhoisUtil.FUZZY_NAMESERVER) ||
				queryType.equals(WhoisUtil.FUZZY_ENTITIES) ||
				queryType.equals(WhoisUtil.IP) ||
				queryType.equals(WhoisUtil.DMOAIN) ||
				queryType.equals(WhoisUtil.ENTITY) ||
				queryType.equals(WhoisUtil.AUTNUM) ||
				queryType.equals(WhoisUtil.NAMESERVER) ||
				queryType.equals(WhoisUtil.HELP) ||
				
				queryType.equals(WhoisUtil.SEARCHDOMAIN)	//search functions of domain
				){
			return true;
		}
		else{
			return false;
		}
	}
	private void displayErrorMessage(HttpServletRequest request, HttpServletResponse response, FilterChain chain, 
			String format, String queryType, String role) throws IOException, ServletException{
		if(format.equals("application/html")){
			chain.doFilter(request, response);
		}else {
			request.setCharacterEncoding("utf-8");
			response.setCharacterEncoding("utf-8");
			
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			
			try {
				map = WhoisUtil.processError(WhoisUtil.COMMENDRRORCODE, role, format);
			} catch (QueryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			PrintWriter out = response.getWriter();
			request.setAttribute("queryFormat", format);
			response.setHeader("Access-Control-Allow-Origin", "*");
			if(format.equals("application/json") || format.equals("application/rdap+json") || format.equals("application/rdap+json;application/json")){
				if(isLegalType(queryType)){
					chain.doFilter(request, response);
				}else{
					response.setHeader("Content-Type", "application/json");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);//400 or 404
					out.print(DataFormat.getJsonObject(map));
				}
			}else if(format.equals("application/xml")){
				if(isLegalType(queryType)){
					chain.doFilter(request, response);
				}else{
					response.setHeader("Content-Type", "application/xml");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write(DataFormat.getXmlString(map));
				}
			}else{
				if(isLegalType(queryType)){
					chain.doFilter(request, response);
				}else{
					response.setHeader("Content-Type", "text/plain");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write(DataFormat.getPresentation(map));
				}
			}
		}
	}
}
