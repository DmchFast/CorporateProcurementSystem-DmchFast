package com.team.corporate.fx.controller.dialog;

import com.pavlobu.emojitextflow.EmojiTextFlow;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ConfirmDeleteController {

    @FXML private EmojiTextFlow confirmEmoji;
    @FXML private Label confirmText;
    @FXML private Label confirmWarning;
    @FXML private Button cancelBtn;
    @FXML private Button deleteBtn;

    @FXML
    public void initialize() {
        confirmEmoji.parseAndAppend("⚠");
    }

    /** Текст подтверждения из вызывающего кода */
    public void setEntityName(String entityName) {
        confirmText.setText("Вы уверены, что хотите удалить\n\"" + entityName + "\"?");
    }
}