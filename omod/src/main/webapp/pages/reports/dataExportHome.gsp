<%
	ui.decorateWith("kenyaemr", "standardPage")
%>
${ ui.includeFragment("kenyaemr", "report/dataExportView", [ request: reportRequest.id, returnUrl: returnUrl ]) }