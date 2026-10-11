package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class CategoriesController {

    // Поиск
    @FXML private TextField searchField;

    // Кнопки
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    // Сетка категорий
    @FXML private GridPane categoriesGrid;
    @FXML private Label emptyPlaceholder;

    @FXML
    public void initialize() {
    }
}