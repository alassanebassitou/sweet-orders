package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.ExpenseRequest;
import com.devcrafter.Patisserie.App.dto.response.ExpenseResponse;
import com.devcrafter.Patisserie.App.dto.response.FinanceDashboardResponse;
import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.enums.ExpensesCategory;
import com.devcrafter.Patisserie.App.enums.PaymentType;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.Expenses;
import com.devcrafter.Patisserie.App.models.Payments;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.ExpensesRepository;
import com.devcrafter.Patisserie.App.repository.OrderedProductsRepository;
import com.devcrafter.Patisserie.App.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceService {

    private final PaymentsRepository paymentsRepository;
    private final ExpensesRepository expensesRepository;
    private final CommandeRepository commandeRepository;
    private final OrderedProductsRepository orderedProductsRepository;

    // ─── Dashboard ───────────────────────────────────────────────

    public FinanceDashboardResponse getDashboard() {

        LocalDate today     = LocalDate.now();
        LocalDate debutMois = today.withDayOfMonth(1);
        LocalDate finMois   = today.withDayOfMonth(
                today.lengthOfMonth()
        );
        LocalDate debutAnnee = today.withDayOfYear(1);
        LocalDate finAnnee   = today.withDayOfYear(
                today.lengthOfYear()
        );

        FinanceDashboardResponse dashboard = new FinanceDashboardResponse();

        // ── Revenue ──────────────────────────────────────────────
        dashboard.setMonthlyRevenue(
                getRevenuPeriode(debutMois, finMois)
        );
        dashboard.setYearlyRevenue(
                getRevenuPeriode(debutAnnee, finAnnee)
        );
        dashboard.setTotalRevenue(
                paymentsRepository.findAll()
                        .stream()
                        .filter(p ->
                                p.getPaymentType() != PaymentType.AVOIR
                                        && p.getPaymentType() !=
                                        PaymentType.REMBOURSEMENT)
                        .map(Payments::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        // ── Expenses ─────────────────────────────────────────────
        dashboard.setMonthlyExpenses(
                expensesRepository.totalExpensesPeriod(
                        debutMois, finMois)
        );
        dashboard.setYearlyExpenses(
                expensesRepository.totalExpensesPeriod(
                        debutAnnee, finAnnee)
        );

        // ── Net profit ───────────────────────────────────────────
        dashboard.setNetProfitMonthly(
                dashboard.getMonthlyRevenue()
                        .subtract(dashboard.getMonthlyExpenses())
        );
        dashboard.setNetProfitYearly(
                dashboard.getYearlyRevenue()
                        .subtract(dashboard.getYearlyExpenses())
        );

        // ── Orders status ────────────────────────────────────────
        List<Commande> toutesCommandes =
                commandeRepository.findAll();

        dashboard.setTotalOrders(toutesCommandes.size());

        long payees = 0, partiel = 0, impayees = 0;
        BigDecimal totalSoldes = BigDecimal.ZERO;

        for (Commande c : toutesCommandes) {
            if (c.getStatus() == CommandStatus.CANCELLED)
                continue;

            BigDecimal paye = paymentsRepository.totalPaieParCommande(c.getId());
            BigDecimal solde = c.getTotalAmount()
                    .subtract(paye)
                    .max(BigDecimal.ZERO);
            totalSoldes = totalSoldes.add(solde);

            log.info("paye value: {}", paye);

            if (solde.compareTo(BigDecimal.ZERO) == 0) {
                payees++;
            } else if (paye.compareTo(BigDecimal.ZERO) > 0) {
                partiel++;
            } else {
                impayees++;
            }
        }

        dashboard.setPaidOrders(payees);
        dashboard.setPartiallyPaidOrders(partiel);
        dashboard.setUnpaidOrders(impayees);
        dashboard.setTotalRemainingBalances(totalSoldes);
        dashboard.setTopProducts(getTopProduits());

        // ── Last 12 months chart data ─────────────────────────────
        dashboard.setRevenueByMonth(
                getLast12MonthsData()
        );

        // ── Expenses by category ──────────────────────────────────
        dashboard.setExpensesByCategory(
                getDepensesParCategorie(debutAnnee, finAnnee)
        );

        return dashboard;
    }

    // ─── Depenses ────────────────────────────────────────────────

    public ExpenseResponse createExpense(ExpenseRequest request) {
        Expenses expenses = new Expenses();
        expenses.setCategory(request.getCategory());
        expenses.setAmount(request.getAmount());
        expenses.setDescription(request.getDescription());
        expenses.setExpenseDate(request.getExpenseDate());

        if (request.getCommandeId() != null) {
            commandeRepository.findById(request.getCommandeId())
                    .ifPresent(expenses::setCommande);
        }
        return ExpenseResponse.from(expensesRepository.save(expenses));
    }

    public ExpenseResponse modifyExpense(Long id,
                                         ExpenseRequest request) {
        Expenses expenses = expensesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dépense", id
                ));
        if (request.getCategory() != null)
            expenses.setCategory(request.getCategory());
        if (request.getAmount() != null)
            expenses.setAmount(request.getAmount());
        if (request.getDescription() != null)
            expenses.setDescription(request.getDescription());
        if (request.getExpenseDate() != null)
            expenses.setExpenseDate(request.getExpenseDate());
        return ExpenseResponse.from(expensesRepository.save(expenses));
    }

    public void deleteExpenses(Long id) {
        expensesRepository.deleteById(id);
    }

    public List<ExpenseResponse> getExpenses(
            ExpensesCategory category,
            LocalDate start,
            LocalDate end) {

        if (category != null) {
            return expensesRepository
                    .findByCategoryOrderByExpenseDateDesc(category)
                    .stream()
                    .map(ExpenseResponse::from)
                    .toList();
        }

        if (start != null && end != null) {
            return expensesRepository
                    .findByExpenseDateBetweenOrderByExpenseDateDesc(
                            start, end)
                    .stream()
                    .map(ExpenseResponse::from)
                    .toList();
        }

        return expensesRepository.findAll()
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCommande(Long commandeId) {
        return expensesRepository.findAllByCommandeId(commandeId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    // ─── CSV Export ───────────────────────────────────────────────

    public String exportCsv(LocalDate debut, LocalDate fin) {

        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Date,Type,Catégorie,Description," +
                "Montant,Commande,Client\n");

        // Payments
        paymentsRepository.findAll().stream()
                .filter(p -> {
                    if (debut == null || fin == null) return true;
                    return !p.getPaymentDate().isBefore(debut)
                            && !p.getPaymentDate().isAfter(fin);
                })
                .forEach(p -> {
                    csv.append(p.getPaymentDate()).append(",");
                    csv.append("REVENU,");
                    csv.append(p.getPaymentType().name()).append(",");
                    csv.append(p.getNotes() != null
                            ? p.getNotes() : "").append(",");
                    csv.append(p.getAmount()).append(",");
                    csv.append(p.getCommande().getNumero())
                            .append(",");
                    csv.append(
                            p.getCommande().getClient().getEmail()
                    ).append("\n");
                });

        // Expenses
        expensesRepository.findAll().stream()
                .filter(d -> {
                    if (debut == null || fin == null) return true;
                    return !d.getExpenseDate().isBefore(debut)
                            && !d.getExpenseDate().isAfter(fin);
                })
                .forEach(d -> {
                    csv.append(d.getExpenseDate()).append(",");
                    csv.append("EXPENSE,");
                    csv.append(d.getCategory().name()).append(",");
                    csv.append(d.getDescription() != null
                            ? d.getDescription() : "").append(",");
                    csv.append(d.getAmount()).append(",");
                    csv.append(",,\n");
                });

        return csv.toString();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private BigDecimal getRevenuPeriode(LocalDate debut,
                                        LocalDate fin) {
        return paymentsRepository.findAll().stream()
                .filter(p ->
                        p.getPaymentDate() != null
                                && !p.getPaymentDate().isBefore(debut)
                                && !p.getPaymentDate().isAfter(fin)
                                && p.getPaymentType() != PaymentType.AVOIR
                                && p.getPaymentType() !=
                                PaymentType.REMBOURSEMENT)
                .map(Payments::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<FinanceDashboardResponse.MonthlyData>
    getLast12MonthsData() {

        List<FinanceDashboardResponse.MonthlyData> data =
                new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            LocalDate mois = LocalDate.now().minusMonths(i);
            LocalDate debut = mois.withDayOfMonth(1);
            LocalDate fin = mois.withDayOfMonth(
                    mois.lengthOfMonth()
            );

            BigDecimal revenu  = getRevenuPeriode(debut, fin);
            BigDecimal depenses = expensesRepository
                    .totalExpensesPeriod(debut, fin);
            BigDecimal benefice = revenu.subtract(depenses);

            String nomMois = mois.getMonth()
                    .getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            java.util.Locale.FRENCH
                    );

            data.add(new FinanceDashboardResponse.MonthlyData(
                    nomMois, revenu, depenses, benefice
            ));
        }
        return data;
    }

    private Map<String, BigDecimal> getDepensesParCategorie(
            LocalDate debut, LocalDate fin) {

        Map<String, BigDecimal> result = new HashMap<>();
        for (ExpensesCategory cat : ExpensesCategory.values()) {
            BigDecimal total = expensesRepository
                    .findByCategoryOrderByExpenseDateDesc(cat)
                    .stream()
                    .filter(d ->
                            d.getExpenseDate() != null
                                    && !d.getExpenseDate().isBefore(debut)
                                    && !d.getExpenseDate().isAfter(fin))
                    .map(Expenses::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(cat.name(), total);
        }
        return result;
    }

    private List<FinanceDashboardResponse.TopProduitData>
    getTopProduits() {

        // Get top 5 products
        List<Object[]> results = orderedProductsRepository
                .findTopProducts(
                        PageRequest.of(0, 5)
                );

        return results.stream()
                .map(row -> new FinanceDashboardResponse.TopProduitData(
                        (String)     row[0],
                        ((Number)    row[1]).longValue(),
                        row[2] != null
                                ? new BigDecimal(row[2].toString())
                                : BigDecimal.ZERO
                ))
                .toList();
    }
}
