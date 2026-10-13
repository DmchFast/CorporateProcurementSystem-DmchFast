package com.team.corporate.fx.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class InventoryDialogController {

    // Заголовок
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;

    // Поля формы
    @FXML private ComboBox<?> warehouseCombo;
    @FXML private Label warehouseError;

    @FXML private ComboBox<?> productCombo;
    @FXML private Label productError;

    @FXML private TextField quantityField;
    @FXML private Label quantityError;

    // Кнопки
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    @FXML
    public void initialize() {
        // Наполнение ComboBox и валидация
    }

    // Заголовок (Остаток товара или Изменение остатка)
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    // Показывает или скрывает сообщение об ошибке под полем
    public void showError(String field, boolean show) {
        switch (field) {
            case "warehouse" -> {
                warehouseError.setVisible(show);
                warehouseError.setManaged(show);
                toggleFieldError(warehouseCombo, show);
            }
            case "product" -> {
                productError.setVisible(show);
                productError.setManaged(show);
                toggleFieldError(productCombo, show);
            }
            case "quantity" -> {
                quantityError.setVisible(show);
                quantityError.setManaged(show);
                toggleFieldError(quantityField, show);
            }
        }
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