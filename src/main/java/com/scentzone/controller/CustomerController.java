package com.scentzone.controller;

import com.scentzone.dao.OrderDAO;
import com.scentzone.dao.PerfumeDAO;
import com.scentzone.model.CartLine;
import com.scentzone.model.OrderSummary;
import com.scentzone.model.Perfume;
import com.scentzone.model.User;
import com.scentzone.service.OrderService;
import com.scentzone.util.Dialogs;
import com.scentzone.util.Money;
import com.scentzone.util.Navigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerController {
    @FXML private Label welcomeLabel, statusLabel, cartTotalLabel, catalogCountLabel;
    @FXML private TextField searchField, quantityField;
    @FXML private ComboBox<String> brandFilter;
    @FXML private TableView<Perfume> perfumeTable;
    @FXML private TableColumn<Perfume, String> pNameCol, pBrandCol, pCategoryCol, pSizeCol, pPriceCol, pStockCol;
    @FXML private TableView<CartLine> cartTable;
    @FXML private TableColumn<CartLine, String> cNameCol, cQuantityCol, cPriceCol;
    @FXML private TableView<OrderSummary> orderTable;
    @FXML private TableColumn<OrderSummary, String> oIdCol, oDateCol, oTotalCol, oStatusCol, oItemsCol;
    private final PerfumeDAO perfumes = new PerfumeDAO();
    private final OrderDAO orders = new OrderDAO();
    private final OrderService service = new OrderService();
    private final List<CartLine> cart = new ArrayList<>();
    private final ObservableList<Perfume> catalog = FXCollections.observableArrayList();
    private final FilteredList<Perfume> filtered = new FilteredList<>(catalog, p -> true);
    private User user;

    @FXML private void initialize() {
        pNameCol.setCellValueFactory(c -> text(c.getValue().name()));
        pBrandCol.setCellValueFactory(c -> text(c.getValue().brandName()));
        pCategoryCol.setCellValueFactory(c -> text(c.getValue().category()));
        pSizeCol.setCellValueFactory(c -> text(c.getValue().sizeMl() + " ml"));
        pPriceCol.setCellValueFactory(c -> text(Money.format(c.getValue().price())));
        pStockCol.setCellValueFactory(c -> text(c.getValue().stock()));
        perfumeTable.setItems(filtered);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());
        brandFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        cNameCol.setCellValueFactory(c -> text(c.getValue().getPerfume().name()));
        cQuantityCol.setCellValueFactory(c -> text(c.getValue().getQuantity()));
        cPriceCol.setCellValueFactory(c -> text(Money.format(c.getValue().getSubtotal())));
        oIdCol.setCellValueFactory(c -> text("#" + c.getValue().id()));
        oDateCol.setCellValueFactory(c -> text(c.getValue().date().toLocalDate()));
        oTotalCol.setCellValueFactory(c -> text(Money.format(c.getValue().total())));
        oStatusCol.setCellValueFactory(c -> text(c.getValue().status()));
        oItemsCol.setCellValueFactory(c -> text(c.getValue().itemCount()));
        quantityField.setText("1");
        updateCart();
    }

    private SimpleStringProperty text(Object value) { return new SimpleStringProperty(String.valueOf(value)); }

    public void setUser(User user) {
        if (user.isAdmin()) throw new SecurityException("Customer page is for customers only.");
        this.user = user;
        welcomeLabel.setText("Hello, " + user.fullName());
        refresh();
    }

    @FXML private void refresh() {
        try {
            catalog.setAll(perfumes.list(false));
            String previous = brandFilter.getValue();
            var names = FXCollections.<String>observableArrayList("All brands");
            catalog.stream().map(Perfume::brandName).distinct().sorted().forEach(names::add);
            brandFilter.setItems(names);
            brandFilter.setValue(previous != null && names.contains(previous) ? previous : "All brands");
            refreshOrders();
            applyFilters();
            statusLabel.setText("Catalog refreshed. Ready to discover your next scent.");
        } catch (SQLException e) { statusLabel.setText("Database error: " + e.getMessage()); }
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String brand = brandFilter.getValue();
        filtered.setPredicate(p -> (query.isEmpty() || (p.name() + " " + p.brandName() + " " + p.category()).toLowerCase().contains(query))
                && (brand == null || "All brands".equals(brand) || brand.equals(p.brandName())));
        catalogCountLabel.setText(filtered.size() + " fragrances");
    }

    @FXML private void addToCart() {
        Perfume selected = perfumeTable.getSelectionModel().getSelectedItem();
        if (selected == null) { Dialogs.info("Select a perfume", "Choose a perfume from the catalog first."); return; }
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity < 1) throw new NumberFormatException("Quantity must be positive.");
            int existing = cart.stream().filter(c -> c.getPerfume().id() == selected.id())
                    .mapToInt(CartLine::getQuantity).sum();
            if (existing + (long) quantity > selected.stock()) {
                Dialogs.error("Insufficient stock", "Only " + selected.stock() + " bottles are available.");
                return;
            }
            var line = cart.stream().filter(c -> c.getPerfume().id() == selected.id()).findFirst();
            if (line.isPresent()) line.get().increase(quantity);
            else cart.add(new CartLine(selected, quantity));
            updateCart();
            statusLabel.setText(selected.name() + " added to your cart.");
        } catch (NumberFormatException e) { Dialogs.error("Invalid quantity", "Enter a whole number greater than zero."); }
    }

    @FXML private void removeFromCart() {
        CartLine line = cartTable.getSelectionModel().getSelectedItem();
        if (line == null) { Dialogs.info("Select an item", "Select an item to remove."); return; }
        cart.remove(line);
        updateCart();
    }

    @FXML private void clearCart() {
        if (!cart.isEmpty() && Dialogs.confirm("Clear cart", "Remove all perfumes from your cart?")) {
            cart.clear(); updateCart();
        }
    }

    private void updateCart() {
        cartTable.setItems(FXCollections.observableArrayList(cart));
        BigDecimal sum = cart.stream().map(CartLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        cartTotalLabel.setText(Money.format(sum));
    }

    @FXML private void checkout() {
        if (cart.isEmpty()) { Dialogs.info("Empty cart", "Add some fragrances before checking out."); return; }
        BigDecimal estimated = cart.stream().map(CartLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!Dialogs.confirm("Place your order", "Submit " + cart.size() + " product(s)? Estimated total "
                + Money.format(estimated) + ". Final price is verified at checkout.")) return;
        try {
            int orderId = service.placeOrder(user.id(), List.copyOf(cart));
            cart.clear();
            updateCart();
            refresh();
            Dialogs.info("Order placed!", "Your order #" + orderId + " is now PENDING. Thank you for shopping at Scent Zone!");
        } catch (SQLException e) {
            Dialogs.error("Checkout failed", e.getMessage() + "\nNo payment was taken; review your cart and refresh the catalog.");
            refresh();
        }
    }

    private void refreshOrders() throws SQLException {
        orderTable.setItems(FXCollections.observableArrayList(orders.listOrders(user.id())));
    }

    @FXML private void viewOrder() {
        OrderSummary selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) { Dialogs.info("Select an order", "Choose an order from the history table."); return; }
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
