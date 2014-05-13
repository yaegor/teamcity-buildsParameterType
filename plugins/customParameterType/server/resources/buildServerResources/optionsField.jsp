<%@include file="/include-internal.jsp"%>
<%--
  ~ Copyright 2000-2014 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="context" scope="request" type="jetbrains.buildServer.controllers.parameters.ParameterRenderContext"/>
<jsp:useBean id="options" scope="request" type="java.util.Collection< customParameterType.server.CustomParameterSelect.KeyValue >"/>

<c:set var="selectedKey" value="${context.parameter.value}"/>
<forms:select name="${context.id}" id="${context.id}" style="width:100%">
  <c:set var="hasSelected" value="${false}"/>
   <c:forEach var="it" items="${options}">
     <c:set var="selected" value="${it.key eq selectedKey}"/>
     <c:if test="${selected}"><c:set var="hasSelected" value="${true}"/></c:if>
     <forms:option value="${it.key}" selected="${selected}"><c:out value="${it.value}"/></forms:option>
   </c:forEach>
</forms:select>
