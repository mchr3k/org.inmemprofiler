<%@ page language="java"
         import="org.inmemprofiler.runtime.Profiler, java.util.Date"
         contentType="text/plain;charset=UTF-8" %>
<% Profiler.pauseProfiling(); %>
<%=new Date().toString()%> : InMemProfiler Profiling started!