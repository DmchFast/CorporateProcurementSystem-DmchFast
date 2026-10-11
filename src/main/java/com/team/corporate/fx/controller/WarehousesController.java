package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class WarehousesController {

    // Поиск
    @FXML private TextField searchField;

    // Кнопки
    @FXML private Button createWarehouseBtn;
    @FXML private Button addProductBtn;
    @FXML private Button changeQtyBtn;
    @FXML private Button deleteBtn;

    // Склады (левый блок)
    @FXML private VBox warehousesListItems;

    // Остатки (правый блок)
    @FXML private Label inventoryTitleLabel;
    @FXML private TableView<?> inventoryTable;
    @FXML private TableColumn<?, ?> productCol;
    @FXML private TableColumn<?, ?> skuCol;
    @FXML private TableColumn<?, ?> qtyCol;

    @FXML
    public void initialize() {
        // Последняя колонка тянется
        inventoryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }
}