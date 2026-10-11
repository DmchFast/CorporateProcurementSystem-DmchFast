package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ProductsController {

    // Фильтры
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField priceFromField;
    @FXML private TextField priceToField;
    @FXML private ComboBox<String> sortCombo;

    // Кнопки
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    // Таблица
    @FXML private TableView<?> productsTable;
    @FXML private TableColumn<?, ?> numCol;
    @FXML private TableColumn<?, ?> nameCol;
    @FXML private TableColumn<?, ?> skuCol;
    @FXML private TableColumn<?, ?> priceCol;
    @FXML private TableColumn<?, ?> categoryCol;

    @FXML
    public void initialize() {
        // Последняя колонка тянется
        productsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }
}