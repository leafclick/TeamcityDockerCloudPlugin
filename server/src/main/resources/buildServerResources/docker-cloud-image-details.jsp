<%@ page import="run.var.teamcity.cloud.docker.ContainerInfo" %>
<%@ page import="run.var.teamcity.cloud.docker.DockerInstance" %>
<%@ page import="run.var.teamcity.cloud.docker.util.DockerCloudUtils" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.time.format.FormatStyle" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Optional" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="image" type="run.var.teamcity.cloud.docker.DockerImage" scope="request"/>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='/plugins/docker-cloud/docker-cloud.css'/>");
</script>

<div class="dockerCloudImageDetails">
    <h4>Registered containers:</h4>
    <div style="margin: 5px 10%; width: 90%">
        <table style="width: 80%;">
            <thead>
            <tr>
                <th style="width: 30%;">Container ID</th>
                <th style="width: 20%;">Created</th>
                <th style="width: 20%;">State</th>
                <th style="width: 30%;">Names</th>
            </tr>
            </thead>
            <tbody>
            <%
                DateTimeFormatter dateFmt = DateTimeFormatter.
                        ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).
                        withLocale(Locale.ENGLISH);
                for (DockerInstance instance : image.getInstances()) {
                    Optional<ContainerInfo> optContainerInfo = instance.getContainerInfo();
                    if (!optContainerInfo.isPresent()) {
                        continue;
                    }
                    ContainerInfo containerInfo = optContainerInfo.get();
                    StringBuilder displayName = new StringBuilder();
                    for (String name : containerInfo.getNames()) {
                        if (displayName.length() > 0) {
                            displayName.append(", ");
                        }
                        displayName.append(name);
                    }
            %>
            <tr>
                <td><%= DockerCloudUtils.toShortId(containerInfo.getId()) %>
                </td>
                <td><%= dateFmt.format(containerInfo.getCreationTimestamp().atZone(ZoneId.systemDefault())) %>
                </td>
                <td><%= containerInfo.getState() %>
                </td>
                <td><%= displayName.toString() %>
                </td>
            </tr>
            <%
                }
            %>
            </tbody>
        </table>
    </div>
    <%
        Optional<Instant> lastDockerSyncTime = image.getCloudClient().getLastDockerSyncTime();
        String lastSync;
        //noinspection OptionalIsPresent
        if (lastDockerSyncTime.isPresent()) {
            //noinspection unchecked
            lastSync = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).
                            withLocale(Locale.ENGLISH).
                            format(lastDockerSyncTime.get().atZone(ZoneId.systemDefault()));
        } else {
            lastSync = "not performed yet.";
        }
    %>
    Last sync with docker: <%= lastSync %>
</div>