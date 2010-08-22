<%@ page language="java"
         import="org.inmemprofiler.runtime.Profiler, java.util.Date"
         contentType="text/plain;charset=UTF-8" %>
<% ProfilerAPI.beginPausedProfiling(); %>
<%=new Date().toString()%> : InMemProfiler Profiling started!