package com.scentzone.controller;

import com.scentzone.dao.OrderDAO;
import com.scentzone.dao.PerfumeDAO;
import com.scentzone.model.Brand;
import com.scentzone.model.OrderSummary;
import com.scentzone.model.Perfume;
import com.scentzone.model.User;
import com.scentzone.util.Dialogs;
import com.scentzone.util.Money;
import com.scentzone.util.Navigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.sql.SQLException;

public class AdminController {
    @FXML private Label welcomeLabel, productCountLabel, lowStockLabel, orderCountLabel, revenueLabel, formTitle;
    @FXML private TableView<Perfume> perfumeTable;
    @FXML private TableColumn<Perfume, String> pIdCol, pNameCol, pBrandCol, pPriceCol, pStockCol, pStatusCol;
    @FXML private ComboBox<Brand> brandBox;
    @FXML private TextField nameField, categoryField, sizeField, priceField, stockField;
    @FXML private TextArea descriptionField;
    @FXML private TableView<OrderSummary> orderTable;
    @FXML private TableColumn<OrderSummary, String> oIdCol, oCustomerCol, oDateCol, oTotalCol, oStatusCol, oItemsCol;
    @FXML private ComboBox<String> statusBox;
    @FXML private Label statusLabel;

    private final PerfumeDAO perfumes = new PerfumeDAO();
    private final OrderDAO orders = new OrderDAO();
    private Perfume editing;
    private User user;

    @FXML private void initialize() {
        pIdCol.setCellValueFactory(c -> text(c.getValue().id()));
        pNameCol.setCellValueFactory(c -> text(c.getValue().name() + "  •  " + c.getValue().sizeMl() + "ml"));
        pBrandCol.setCellValueFactory(c -> text(c.getValue().brandName()));
        pPriceCol.setCellValueFactory(c -> text(Money.format(c.getValue().price())));
        pStockCol.setCellValueFactory(c -> text(c.getValue().stock()));
        pStatusCol.setCellValueFactory(c -> text(c.getValue().active() ? "ACTIVE" : "HIDDEN"));
        perfumeTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> populate(selected));

        oIdCol.setCellValueFactory(c -> text("#" + c.getValue().id()));
        oCustomerCol.setCellValueFactory(c -> text(c.getValue().customerName()));
        oDateCol.setCellValueFactory(c -> text(c.getValue().date().toLocalDate().toString()));
        oTotalCol.setCellValueFactory(c -> text(Money.format(c.getValue().total())));
        oStatusCol.setCellValueFactory(c -> text(c.getValue().status()));
        oItemsCol.setCellValueFactory(c -> text(c.getValue().itemCount()));
        statusBox.setItems(FXCollections.observableArrayList(
                "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"));
        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) statusBox.setValue(selected.status());
        });
    }

    private SimpleStringProperty text(Object value) { return new SimpleStringProperty(String.valueOf(value)); }

    public void setUser(User user) {
        if (!user.isAdmin()) throw new SecurityException("Admin dashboard requires an admin account.");
        this.user = user;
        welcomeLabel.setText("Welcome back, " + user.fullName());
        refresh();
    }

    @FXML private void refresh() {
        try {
            perfumeTable.setItems(FXCollections.observableArrayList(perfumes.list(true)));
            brandBox.setItems(FXCollections.observableArrayList(perfumes.listBrands()));
            orderTable.setItems(FXCollections.observableArrayList(orders.listOrders(null)));
            var summary = perfumes.dashboardStats();
            productCountLabel.setText(String.valueOf(summary.totalPerfumes()));
            lowStockLabel.setText(String.valueOf(summary.lowStock()));
            orderCountLabel.setText(String.valueOf(summary.totalOrders()));
            revenueLabel.setText(Money.format(summary.revenue()));
            statusLabel.setText("Everything is up to date.");
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
            Dialogs.error("Could not refresh", e.getMessage());
        }
    }

    private void populate(Perfume perfume) {
        editing = perfume;
        if (perfume == null) return;
        formTitle.setText("EDIT PERFUME  #" + perfume.id());
        nameField.setText(perfume.name());
        categoryField.setText(perfume.category());
        sizeField.setText(String.valueOf(perfume.sizeMl()));
        priceField.setText(perfume.price().toPlainString());
        stockField.setText(String.valueOf(perfume.stock()));
        descriptionField.setText(perfume.description());
        brandBox.getItems().stream().filter(b -> b.id() == perfume.brandId()).findFirst().ifPresent(brandBox::setValue);
    }

    @FXML private void clearForm() {
        perfumeTable.getSelectionModel().clearSelection();
        editing = null;
        formTitle.setText("ADD NEW PERFUME");
        nameField.clear(); categoryField.clear(); sizeField.clear(); priceField.clear();
        stockField.clear(); descriptionField.clear(); brandBox.setValue(null);
    }

    @FXML private void savePerfume() {
        try {
            Brand brand = brandBox.getValue();
            if (brand == null) throw new IllegalArgumentException("Select a brand.");
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            if (name.isEmpty() || name.length() > 150) throw new IllegalArgumentException("Enter a perfume name (up to 150 characters).");
            if (category.isEmpty() || category.length() > 60) throw new IllegalArgumentException("Enter a category (up to 60 characters).");
            int size = Integer.parseInt(sizeField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            if (size <= 0 || stock < 0 || price.signum() < 0 || price.scale() > 2 || price.compareTo(new BigDecimal("99999999.99")) > 0)
                throw new IllegalArgumentException("Size must be positive, stock nonnegative, and price valid (max 2 decimals).");
            String description = descriptionField.getText();
            if (editing == null)
                perfumes.add(brand.id(), name, category, size, price, stock, description);
            else
                perfumes.update(editing.id(), brand.id(), name, category, size, price, stock, description);
            clearForm();
            refresh();
            statusLabel.setText("Perfume saved successfully.");
        } catch (NumberFormatException e) {
            Dialogs.error("Invalid input", "Enter valid numbers for size, stock and price.");
        } catch (IllegalArgumentException | SQLException e) {
            Dialogs.error("Cannot save perfume", e.getMessage());
        }
    }

    @FXML private void toggleVisibility() {
        Perfume selected = perfumeTable.getSelectionModel().getSelectedItem();
        if (selected == null) { Dialogs.info("Select a perfume", "Choose a row in the products table."); return; }
        if (!Dialogs.confirm("Change listing visibility", "Set " + selected.name() + " to "
                + (selected.active() ? "HIDDEN" : "ACTIVE") + "?")) return;
        try {
            perfumes.setActive(selected.id(), !selected.active());
            clearForm();
            refresh();
        } catch (SQLException e) { Dialogs.error("Could not update", e.getMessage()); }
    }

    @FXML private void addBrand() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Scent Zone");
        dialog.setHeaderText("Add a perfume brand");
        dialog.setContentText("Brand name:");
        dialog.showAndWait().ifPresent(name -> {
            String value = name.trim();
            if (value.isEmpty() || value.length() > 100) { Dialogs.error("Invalid brand", "Enter a brand name up to 100 characters."); return; }
            try {
                perfumes.addBrand(value);
                brandBox.setItems(FXCollections.observableArrayList(perfumes.listBrands()));
                brandBox.getItems().stream().filter(b -> b.name().equalsIgnoreCase(value)).findFirst().ifPresent(brandBox::setValue);
            } catch (SQLException e) { Dialogs.error("Could not add brand", e.getMessage()); }
        });
    }

    @FXML private void changeStatus() {
        OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
        String next = statusBox.getValue();
        if (selected == null || next == null) { Dialogs.info("Select an order", "Choose an order and a new status."); return; }
        if (!Dialogs.confirm("Update order #" + selected.id(), "Change from " + selected.status() + " to " + next + "?")) return;
        try {
            orders.updateStatus(selected.id(), next);
            refresh();
            statusLabel.setText("Order status updated.");
        } catch (SQLException e) { Dialogs.error("Cannot change order status", e.getMessage()); }
    }

    @FXML private void viewOrder() {
        OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) { Dialogs.info("Select an order", "Choose an order to view its items."); return; }
        try {
            StringBuilder details = new StringBuilder();
            for (var line : orders.getOrderItems(selected.id()))
                details.append(line.perfumeName()).append("  × ").append(line.quantity())
                        .append("  =  ").append(Money.format(line.subtotal())).append("\n");
            details.append("\nTotal: ").append(Money.format(selected.total()));
            Dialogs.info("Order #" + selected.id(), details.toString());
        } catch (SQLException e) { Dialogs.error("Could not load order", e.getMessage()); }
    }

    @FXML private void logout() { Navigator.login(); }
}
