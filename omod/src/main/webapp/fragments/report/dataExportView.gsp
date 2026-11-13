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
		font-size: 14px;
		font-family: Monaco,Andale Mono,Courier New,monospace;
	}

	.errorText {
		color: red;
		font-size: 14px;
		font-weight: bold;
		font-family: Monaco,Andale Mono,Courier New,monospace;
	}
	.khis-gate-panel {
		max-width: 850px;
		margin: 12px 0;
		padding: 10px 14px;
		border-left: 6px solid #8d8d8d;
		background: #f4f4f4;
		font-size: 13px;
	}
	.khis-gate-panel.ready { border-color: #24a148; background: #defbe6; }
	.khis-gate-panel.held { border-color: #da1e28; background: #fff1f1; }
	.khis-gate-headline { font-size: 16px; font-weight: bold; margin-bottom: 4px; }
	.khis-gate-panel.ready .khis-gate-headline { color: #0e6027; }
	.khis-gate-panel.held .khis-gate-headline { color: #a2191f; }
	.khis-gate-panel ul { margin: 6px 0 6px 18px; }
	.khis-gate-actions { margin-top: 6px; }
	.khis-gate-group { margin-top: 10px; padding: 8px 10px; background: #ffffff; border-left: 4px solid #8d8d8d; }
	.khis-gate-group.blockers { border-color: #da1e28; }
	.khis-gate-group.warnings { border-color: #f1c21b; }
	.khis-gate-group.tickets { border-color: #0f62fe; }
	.khis-gate-group h4 { margin: 0 0 2px; font-size: 14px; }
	.khis-gate-group .khis-gate-hint { color: #525252; margin: 0 0 4px; }
	.khis-gate-group ul { margin: 4px 0 8px 18px; }
	a.khis-gate-action {
		display: inline-block; padding: 6px 12px; background: #0f62fe; color: #ffffff; text-decoration: none;
		font-weight: bold; border-radius: 2px;
	}
	a.khis-gate-action:hover, a.khis-gate-action:focus { background: #0353e9; color: #ffffff; text-decoration: none; }
	.khis-gate-group.blockers a.khis-gate-action { background: #da1e28; }
	.khis-gate-group.blockers a.khis-gate-action:hover { background: #b81921; }
	.khis-forbidden { vertical-align: -2px; }
	.khis-gate-group li.resolved { color: #525252; }
	.khis-resolved {
		display: inline-block; margin-left: 6px; padding: 0 8px; border-radius: 12px; background: #a7f0ba; color: #0e6027;
		font-size: 12px; font-weight: bold;
	}
</style>

<div class="ke-page-sidebar">
	<div class="ke-panel-frame">
		${ ui.includeFragment("kenyaui", "widget/panelMenuItem", [ iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back", href: returnUrl ]) }
	</div>
</div>
<div class="ke-page-content">

	<h2>${ outputFormat == 'json' ? 'JSON' : 'ADX' } Message for ${ reportName }</h2>
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
	<div id="khisGate" class="khis-gate-panel" style="display: none">
		<div class="khis-gate-headline"><span id="gateIcon"></span> <span id="gateHeadline"></span></div>
		<div id="gateMessage"></div>
		<div id="gateGroups"></div>
		<div class="khis-gate-actions">
			<a id="gateDqLink" class="khis-gate-action" href="#" target="_blank">Open the report's checks on the Data quality page &rarr;</a>
			&nbsp;&nbsp;<button id="runChecks" style="display: none">Run the checks</button>
		</div>
	</div>
	<button id="toggleAdxDiv">Show/Hide Message</button> &nbsp;&nbsp;
	<button id="post">Submit Message</button>
	<p></p>

	<div id="adxMsg">
		<textarea ${ outputFormat == 'json' ? 'style="background: white; color: black;"' : '' }>${ khis }</textarea>
	</div>
	<br/>

</div>

<script type="text/javascript">
	jq = jQuery;

	jq(function() {
		jq("#showStatus").hide();
		jq("#adxMsg").hide();

		// Whether the figures may be sent to KHIS yet; viewing the message is never held back
		var gate = ${ gateJson };
		var gatePoll = null;
		var headlines = {
			READY: "Ready to send to KHIS",
			CHECKING: "Checking data quality... sending is disabled until the checks finish",
			NOT_CHECKED: "Not checked yet: sending is disabled",
			BLOCKED: "Blocked by a data quality check: sending is disabled",
			NEEDS_REVIEW: "Needs review: sending is disabled",
			NOT_LATEST: "Not the latest figures: sending is disabled",
			WRONG_PERIOD: "Not a whole week or month: sending is disabled"
		};

		function renderGate(g) {
			gate = g || gate;
			if (!gate || gate.state === "NOT_CHECKED_HERE") {
				jq("#khisGate").hide();
				return;
			}
			var checking = gate.state === "CHECKING";
			jq("#khisGate").removeClass("ready held").addClass(gate.sendable ? "ready" : (checking ? "" : "held")).show();
			// A tick, an hourglass while checking, or the forbidden sign when sending is held back
			if (gate.sendable || checking) {
				jq("#gateIcon").text(String.fromCharCode(gate.sendable ? 0x2713 : 0x231B));
			} else {
				jq("#gateIcon").html('<svg class="khis-forbidden" viewBox="0 0 16 16" width="14" height="14" aria-hidden="true"><circle cx="8" cy="8" r="6.5" fill="none" stroke="currentColor" stroke-width="2"/><line x1="3.4" y1="3.4" x2="12.6" y2="12.6" stroke="currentColor" stroke-width="2"/></svg>');
			}
			jq("#gateHeadline").text(headlines[gate.state] || gate.label || gate.state);
			jq("#gateMessage").text(gate.message || "");
			// The checks that did not pass, by what each needs, blockers first, each with where to act on it
			var groups = jq("#gateGroups").empty();
			var blockers = gate.blockers || [];
			var warnings = gate.warnings || [];
			var tickets = gate.tickets || [];
			var resolved = gate.resolvedTickets || [];
			var allResolved = tickets.length > 0 && jq.grep(tickets, function(t) { return jq.inArray(t, resolved) < 0; }).length === 0;
			addGroup(groups, "blockers", "Blockers", blockers,
				"Sending stays disabled until these pass. Correct the records, refresh the ETL and generate the report again.",
				gate.reviewUrl, "See what to correct, patient by patient");
			addGroup(groups, "warnings", "Warnings", warnings,
				"Each needs a reason. Give the reasons and approve the report, then come back here to send.",
				gate.reviewUrl, "Give reasons and approve");
			addGroup(groups, "tickets", "Flagged for follow-up (tickets)", tickets,
				allResolved ? "Every ticket has been resolved." : "These do not hold sending; they are followed up as tickets.",
				gate.ticketsUrl, allResolved ? "See the tickets" : "Follow up the tickets", resolved);
			// The general link only when no group has its own
			var grouped = blockers.length + warnings.length + tickets.length > 0;
			jq("#gateDqLink").attr("href", gate.reviewUrl || gate.dqUrl).toggle(!grouped && !!(gate.reviewUrl || gate.dqUrl));
			jq("#runChecks").toggle(gate.state === "NOT_CHECKED");
			jq("#post").prop("disabled", !gate.sendable);

			if (checking && !gatePoll) {
				gatePoll = setInterval(refreshGate, 5000);
			} else if (!checking && gatePoll) {
				clearInterval(gatePoll);
				gatePoll = null;
			}
		}

		// resolved: the items whose ticket has been resolved, tagged Resolved
		function addGroup(container, kind, title, items, hint, url, action, resolved) {
			if (!items.length) {
				return;
			}
			var group = jq("<div/>").addClass("khis-gate-group " + kind);
			var done = jq.grep(items, function(item) { return jq.inArray(item, resolved || []) >= 0; }).length;
			group.append(jq("<h4/>").text(title + " (" + items.length + (done ? ", " + done + " resolved" : "") + ")"));
			group.append(jq("<p/>").addClass("khis-gate-hint").text(hint));
			var list = jq("<ul/>");
			jq.each(items, function(i, item) {
				var li = jq("<li/>").text(item);
				if (jq.inArray(item, resolved || []) >= 0) {
					li.addClass("resolved").append(jq("<span/>").addClass("khis-resolved").text("\u2713 Resolved"));
				}
				list.append(li);
			});
			group.append(list);
			if (url) {
				group.append(jq("<a/>").addClass("khis-gate-action").attr({ href: url, target: "_blank" })
					.text(action + " on the Data quality page \u2192"));
			}
			container.append(group);
		}

		function refreshGate() {
			jq.getJSON('${ ui.actionLink("getSendGate") }', { request: '${ reportRequest.id }' }, renderGate);
		}

		jq("#runChecks").click(function() {
			jq("#runChecks").prop("disabled", true);
			renderGate(jq.extend({}, gate, { state: "CHECKING", sendable: false, message: "Running the checks..." }));
			jq.post('${ ui.actionLink("runChecks") }', { request: '${ reportRequest.id }' }, renderGate, "json")
				.always(function() { jq("#runChecks").prop("disabled", false); });
		});

		renderGate(gate);

		// The report is approved, or its warnings answered, on the Data quality page in another tab:
		// check again on coming back, so the panel and the send button follow without a reload
		var lastRefresh = 0;
		function refreshOnReturn() {
			var now = new Date().getTime();
			if (!gate || gate.state === "NOT_CHECKED_HERE" || gate.state === "CHECKING" || now - lastRefresh < 2000) {
				return;
			}
			lastRefresh = now;
			refreshGate();
		}
		jq(document).on("visibilitychange", function() {
			if (document.visibilityState === "visible") {
				refreshOnReturn();
			}
		});
		jq(window).on("focus", refreshOnReturn);

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
				url: '${ ui.actionLink("buildDocument") }',
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
						if (data.gate) {
							renderGate(data.gate);
						} else {
							jq("#post").prop("disabled", !(gate && gate.sendable !== false));
						}
					}
				},
				error: function(xhr, status, err) {
					jq("#showStatus").hide();
					jq("#msg").removeClass("successText errorText").addClass("errorText");

					var response = xhr.responseJSON || {};
					var statusMsg = response.statusMsg || xhr.responseText || err || "There was an error sending the message.";

					jq("#msg").html(formatDhisResponse(statusMsg));
					jq("#post").prop("disabled", !(gate && gate.sendable !== false));
				}
			});

		});

		jq('#toggleAdxDiv').click(function() {
			jq("#adxMsg").toggle();
		});

	});
</script>
