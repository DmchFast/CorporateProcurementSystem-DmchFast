package com.team.corporate.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class UsersController {

    // Поиск и фильтр по роли
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;

    // Кнопки
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    // Таблица
    @FXML private TableView<?> usersTable;
    @FXML private TableColumn<?, ?> numCol;
    @FXML private TableColumn<?, ?> nameCol;
    @FXML private TableColumn<?, ?> roleCol;
    @FXML private TableColumn<?, ?> dateCol;

    @FXML
    public void initialize() {
        // Последняя колонка тянется
        usersTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Наполняем фильтр ролей
        roleFilter.setItems(FXCollections.observableArrayList("Все", "USER", "MANAGER"));
        roleFilter.getSelectionModel().selectFirst();
    }
}