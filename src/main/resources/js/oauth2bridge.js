// 确保在 AUI 环境下安全运行
AJS.toInit(function($) {
    var $passwordInput = $("#clientSecretId");
    var $toggleBtn = $("#togglePasswordBtn");

    $toggleBtn.on("click", function() {
        var currentType = $passwordInput.attr("type");

        if (currentType === "password") {
            // 切换为明文
            $passwordInput.attr("type", "text");
            // 改变图标样式 (假设使用 AUI 图标，如果有特定的 'view-hidden' 图标可以切换类名)
            // 如果你的 Jira 版本没有 'aui-iconfont-view-hidden'，你可以只改变不透明度或颜色来表示状态
            $(this).addClass("active");
            $(this).attr("title", "Hide Password");
        } else {
            // 切换为密码
            $passwordInput.attr("type", "password");
            $(this).removeClass("active");
            $(this).attr("title", "Show Password");
        }
    });
});