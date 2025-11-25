AJS.toInit(function () {
    var $tableBody = AJS.$('#clientConfigTableBody');

    function createNewRow() {
        return `
            <tr>
                <td>
                    <input class="text full-width-field" type="text"
                           name="clientIds"
                           placeholder="${window.OAuth2BridgeI18n.clientIdPlaceholder}"
                           value=""/>
                </td>
                <td>
                    <input class="text full-width-field" type="text"
                           name="callbacks"
                           placeholder="${window.OAuth2BridgeI18n.callbackPlaceholder}"
                           value=""/>
                </td>
                <td>
                    <button type="button"
                            class="aui-button aui-button-link delete-row-btn">
                        ${window.OAuth2BridgeI18n.deleteButton}
                    </button>
                </td>
            </tr>
        `;
    }

    // 添加行
    AJS.$('#addRowBtn').on('click', function() {
        $tableBody.append(createNewRow());
    });

    // 删除行
    $tableBody.on('click', '.delete-row-btn', function() {
        if (confirm(window.OAuth2BridgeI18n.deleteConfirm)) {
            AJS.$(this).closest('tr').remove();
        }
    });

    // 默认显示一行
    if ($tableBody.children().length === 0) {
        $tableBody.append(createNewRow());
    }
});