<%@ page language="java"
         import="org.inmemprofiler.runtime.ProfilerAPI, java.util.Date"
         contentType="text/plain;charset=UTF-8" %>
<% ProfilerAPI.beginPausedProfiling(); %>
<%=new Date().toString()%> : InMemProfiler Profiling started!