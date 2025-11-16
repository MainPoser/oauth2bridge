AJS.toInit(function(){
    console.log("oauth2bridge.js loaded");

    const contextPath = AJS.contextPath();
    const restUrl = contextPath + "/rest/oauth2bridge/1.0/settings";

    function loadConfig() {
        AJS.$.ajax({
            url: restUrl,
            type: "GET",
            dataType: "json",
            success: function(config) {
                AJS.$("#client-id").val(config.clientId);
                AJS.$("#client-secret").val(config.clientSecret);
                AJS.$("#authorization-endpoint").val(config.authorizationEndpoint);
                AJS.$("#token-endpoint").val(config.tokenEndpoint);
                AJS.$("#userInfo-endpoint").val(config.userInfoEndpoint);
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log("Error loading config:", errorThrown);
            }
        });
    }

    AJS.$("#save-button").click(function(e){
        console.log("save button clicked");
        e.preventDefault();

        const data = {
            clientId: AJS.$("#client-id").val(),
            clientSecret: AJS.$("#client-secret").val(),
            authorizationEndpoint: AJS.$("#authorization-endpoint").val(),
            tokenEndpoint: AJS.$("#token-endpoint").val(),
            userInfoEndpoint: AJS.$("#userInfo-endpoint").val()
        };

        AJS.$.ajax({
            url: restUrl,
            type: "PUT",
            contentType: "application/json",
            data: JSON.stringify(data),
            headers: { "X-Atlassian-Token": "no-check" }, // 防 CSRF
            success: function() {
                console.log("save success");
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log("save error:", errorThrown);
            }
        });
    });

    loadConfig();
});
