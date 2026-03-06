<style>
textarea {
	border: 1px solid black;
	background: black;
	color: white;
	padding: 10px;
	width: 850px;
	height: 300px;
	overflow: scroll;
	font-size: 12px;
	font-family: Monaco,Andale Mono,Courier New,monospace;
}
	.successText {
		color: dodgerblue;
		font-weight: bold;
		font-size: 18px;
		font-family: Monaco,Andale Mono,Courier New,monospace;
	}

	.errorText {
		color: red;
		font-size: 18px;
		font-weight: bold;
		font-family: Monaco,Andale Mono,Courier New,monospace;
	}
</style>

<div class="ke-page-sidebar">
	<div class="ke-panel-frame">
		${ ui.includeFragment("kenyaui", "widget/panelMenuItem", [ iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back", href: returnUrl ]) }
	</div>
</div>
<div class="ke-page-content">

	<h2>ADX Message for ${ reportName }</h2>
	<fieldset>
		<legend>Reporting Date</legend>
		<br/>
		<b>Start Date:</b>	${ startDate } <br/>
		<b>End Date:</b> &nbsp;${ endDate }
	</fieldset>
	<br/>
	<fieldset>
		<legend>Server Settings</legend>
		<br/>
		<b>IP Address/URL:</b>	<input type="text" readonly="readonly" size="${serverAddressLength + 4}" style="border:2px inset #eee; margin:-2px;" placeholder="IP Address" value="${serverAddress}">&nbsp;&nbsp; <button id="editServerAddress">Edit</button>
	</fieldset>

	<div id="showStatus">
		<span id="msgSpan"></span> &nbsp;&nbsp;<img src="${ ui.resourceLink("kenyaui", "images/loader_small.gif") }"/>
	</div>
	<br/>
	<div id="msg"></div>
	<br/>
	<br/>
	<button id="toggleAdxDiv">Show/Hide Message</button> &nbsp;&nbsp;
	<button id="post">Submit Message</button>
	<p></p>

	<div id="adxMsg">
		<textarea>${ adx }</textarea>
	</div>
	<br/>

</div>

<script type="text/javascript">
	jq = jQuery;

	jq(function() {
		jq("#showStatus").hide();
		jq("#adxMsg").hide();

		function formatDhisResponse(statusMsg) {
			try {
				var data = (typeof statusMsg === "string") ? JSON.parse(statusMsg) : statusMsg;

				var status = data.status || data.httpStatus || "Unknown";
				var message = data.message || "";
				var summary = "";

				if (data.response) {
					var r = data.response;
					var importStatus = r.status || "";
					var description = r.description || "";
					var counts = r.importCount || {};

					summary += "<b>Status:</b> " + importStatus + "<br/>";
					if (description) {
						summary += "<b>Description:</b> " + description + "<br/>";
					}
					summary += "<b>Imported:</b> " + (counts.imported || 0)
							+ " &nbsp;|&nbsp; <b>Updated:</b> " + (counts.updated || 0)
							+ " &nbsp;|&nbsp; <b>Ignored:</b> " + (counts.ignored || 0)
							+ " &nbsp;|&nbsp; <b>Deleted:</b> " + (counts.deleted || 0)
							+ "<br/>";

					if (r.conflicts && r.conflicts.length > 0) {
						summary += "<br/><b>Conflicts (" + r.conflicts.length + "):</b><ul>";
						for (var i = 0; i < r.conflicts.length && i < 10; i++) {
							var c = r.conflicts[i];
							summary += "<li>" + (c.object || "") + ": " + (c.value || "") + "</li>";
						}
						if (r.conflicts.length > 10) {
							summary += "<li>... and " + (r.conflicts.length - 10) + " more</li>";
						}
						summary += "</ul>";
					}
				} else {
					summary = message || statusMsg;
				}

				return summary;
			} catch (e) {
				return statusMsg;
			}
		}

		function isSuccessResponse(statusCode, statusMsg) {
			if (isNaN(statusCode) || statusCode < 200 || statusCode >= 300) {
				return false;
			}

			try {
				var data = (typeof statusMsg === "string") ? JSON.parse(statusMsg) : statusMsg;
				if (data.response && data.response.status) {
					return data.response.status === "SUCCESS";
				}
			} catch (e) {
				// not JSON — fall through
			}

			return true;
		}

		jq('#post').click(function() {
			jq("#msgSpan").text("Sending Message to IL Server .....");
			jq("#showStatus").show();
			jq("#msg").removeClass("successText errorText").html("");

			jq("#post").prop("disabled", true);
			jq.ajax({
				url: '${ ui.actionLink("buildXmlDocument") }',
				type: 'POST',
				dataType: 'json',
				data: {
					'request': '${ reportRequest.id }'
				},
				success: function(data) {
					jq("#showStatus").hide();

					var statusCode = parseInt(data.statusCode, 10);
					var statusMsg = data.statusMsg || "";

					jq("#msg").removeClass("successText errorText");

					var formattedMsg = formatDhisResponse(statusMsg);

					if (isSuccessResponse(statusCode, statusMsg)) {
						jq("#msg").addClass("successText");
						jq("#msg").html(formattedMsg || "Message successfully sent");
						jq("#post").prop("disabled", true);
					} else {
						jq("#msg").addClass("errorText");
						jq("#msg").html(formattedMsg || "There was an error sending the message.");
						jq("#post").prop("disabled", false);
					}
				},
				error: function(xhr, status, err) {
					jq("#showStatus").hide();
					jq("#msg").removeClass("successText errorText").addClass("errorText");

					var response = xhr.responseJSON || {};
					var statusMsg = response.statusMsg || xhr.responseText || err || "There was an error sending the message.";

					jq("#msg").html(formatDhisResponse(statusMsg));
					jq("#post").prop("disabled", false);
				}
			});

		});

		jq('#toggleAdxDiv').click(function() {
			jq("#adxMsg").toggle();
		});

	});
</script>
