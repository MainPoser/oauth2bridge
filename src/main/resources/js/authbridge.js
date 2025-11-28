AJS.toInit(function () {
    var $tableBody = AJS.$('#clientConfigTableBody');

    function createNewRow() {
        return `
            <tr>
                <td>
                    <input required class="text full-width-field" type="text"
                           name="clientIds"
                           placeholder="${window.AuthBridgeI18n.clientIdPlaceholder}"
                           value=""/>
                </td>
                <td>
                    <input required class="text full-width-field" type="text"
                           name="callbacks"
                           placeholder="${window.AuthBridgeI18n.callbackPlaceholder}"
                           value=""/>
                </td>
                <td>
                    <input class="text full-width-field" type="text"
                           name="redirectUrls"
                           placeholder="${window.AuthBridgeI18n.redirectUrlPlaceholder}"
                           value=""/>
                </td>
                <td>
                    <button type="button"
                            class="aui-button aui-button-link delete-row-btn">
                        ${window.AuthBridgeI18n.deleteButton}
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
        if (confirm(window.AuthBridgeI18n.deleteConfirm)) {
            AJS.$(this).closest('tr').remove();
        }
    });
});