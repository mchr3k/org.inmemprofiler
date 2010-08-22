<%@ page language="java"
         import="org.inmemprofiler.runtime.ProfilerAPI, java.util.Date"
         contentType="text/plain;charset=UTF-8" %>
<% ProfilerAPI.outputData(); %>
<%=new Date().toString()%> : InMemProfiler Data Output!