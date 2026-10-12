package com.team.corporate.fx.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ProductDialogController {

    // Заголовок
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;

    // Поля формы
    @FXML private TextField nameField;
    @FXML private Label nameError;

    @FXML private TextField skuField;
    @FXML private Label skuError;

    @FXML private ComboBox<?> categoryCombo;
    @FXML private Label categoryError;

    @FXML private TextField priceField;
    @FXML private Label priceError;

    // Кнопки
    @FXML private Button cancelBtn;
    @FXML private Button saveBtn;

    @FXML
    public void initialize() {
        // Валидация и обработчики кнопок
    }

    // Заголовок (Товар или Редактирование товара)
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    // Ошибка/
    public void showError(String field, boolean show) {
        switch (field) {
            case "name" -> {
                nameError.setVisible(show);
                nameError.setManaged(show);
                toggleFieldError(nameField, show);
            }
            case "sku" -> {
                skuError.setVisible(show);
                skuError.setManaged(show);
                toggleFieldError(skuField, show);
            }
            case "category" -> {
                categoryError.setVisible(show);
                categoryError.setManaged(show);
                toggleFieldError(categoryCombo, show);
            }
            case "price" -> {
                priceError.setVisible(show);
                priceError.setManaged(show);
                toggleFieldError(priceField, show);
            }
        }
    }

    private void toggleFieldError(javafx.scene.Node node, boolean on) {
        if (on) {
            if (!node.getStyleClass().contains("field-error")) {
                node.getStyleClass().add("field-error");
            }
        } else {
            node.getStyleClass().remove("field-error");
        }
    }
}