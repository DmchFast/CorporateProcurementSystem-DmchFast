package com.team.corporate.fx.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class WarehouseDialogController {

    // Заголовок
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;

    // Поля формы
    @FXML private TextField nameField;
    @FXML private Label nameError;

    @FXML private TextField locationField;

    // Кнопки
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    @FXML
    public void initialize() {
        // Валидация и обработчики кнопок
    }

    // Заголовок (Склад или Редактирование склада)
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    // Показывает или скрывает сообщение об ошибке под полем
    public void showError(boolean show) {
        nameError.setVisible(show);
        nameError.setManaged(show);
        toggleFieldError(nameField, show);
    }

    private void toggleFieldError(Node node, boolean on) {
        if (on) {
            if (!node.getStyleClass().contains("field-error")) {
                node.getStyleClass().add("field-error");
            }
        } else {
            node.getStyleClass().remove("field-error");
        }
    }
}