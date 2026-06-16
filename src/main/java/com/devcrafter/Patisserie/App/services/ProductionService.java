package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.DailyPlanning;
import com.devcrafter.Patisserie.App.dto.DailyProductionForm;
import com.devcrafter.Patisserie.App.enums.CommandStatus;
import com.devcrafter.Patisserie.App.enums.NotificationTypes;
import com.devcrafter.Patisserie.App.models.Commande;
import com.devcrafter.Patisserie.App.models.OrderedProducts;
import com.devcrafter.Patisserie.App.models.Settings;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionService {

    private final CommandeRepository commandeRepository;
    private final SettingsRepository settingsRepository;
    private final NotificationService notificationService;

    /**
     * Weekly planning — 7 days from dateDebut.
     */
    public List<DailyPlanning> getPlanning(LocalDate dateDebut) {
        LocalDate fin = dateDebut.plusDays(6);
        int seuil = getSeuil();

        List<DailyPlanning> planning = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate jour = dateDebut.plusDays(i);

            List<Commande> commandes = commandeRepository
                    .findByWishDeliveryDate(jour)
                    .stream()
                    .filter(c -> c.getStatus() !=
                            CommandStatus.CANCELLED)
                    .toList();

            int totalCakes = commandes.stream()
                    .mapToInt(c -> c.getOrderedProducts().stream()
                            .mapToInt(OrderedProducts::getQuantity)
                            .sum())
                    .sum();

            List<DailyPlanning.CommandeResume> resumes =
                    commandes.stream()
                            .map(c -> new DailyPlanning.CommandeResume(
                                    c.getId(),
                                    c.getNumero(),
                                    c.getClient().getLastname()
                                            + " "
                                            + c.getClient().getFirstname(),
                                    c.getStatus().name(),
                                    c.getOrderedProducts().stream()
                                            .mapToInt(
                                                    OrderedProducts::getQuantity)
                                            .sum()
                            ))
                            .toList();

            String jourSemaine = jour.getDayOfWeek()
                    .getDisplayName(
                            java.time.format.TextStyle.FULL,
                            java.util.Locale.FRENCH
                    );

            DailyPlanning planningJour = new DailyPlanning();
            planningJour.setDate(jour);
            planningJour.setDayOfWeek(jourSemaine);
            planningJour.setTotalCakes(totalCakes);
            planningJour.setTotalCommandes(commandes.size());
            planningJour.setOverload(totalCakes >= seuil);
            planningJour.setCommandes(resumes);

            planning.add(planningJour);
        }

        return planning;
    }

    /**
     * Daily production sheet.
     */
    public DailyProductionForm getDailyForm(LocalDate date) {
        int seuil = getSeuil();

        List<Commande> commandes = commandeRepository
                .findByWishDeliveryDate(date)
                .stream()
                .filter(c -> c.getStatus() !=
                        CommandStatus.CANCELLED)
                .toList();

        List<DailyProductionForm.LineProduction> lignes = new ArrayList<>();

        for (Commande c : commandes) {
            for (OrderedProducts cp : c.getOrderedProducts()) {

                // Format personnalisations as readable string
                String perso = "";
                if (cp.getCustomizationJson() != null) {
                    perso = cp.getCustomizationJson()
                            .entrySet().stream()
                            .map(e -> e.getKey()
                                    + ": " + e.getValue())
                            .collect(Collectors.joining(", "));
                }

                lignes.add(
                        new DailyProductionForm.LineProduction(
                                c.getId(),
                                c.getNumero(),
                                c.getClient().getLastname()
                                        + " "
                                        + c.getClient().getFirstname(),
                                c.getClient().getTelephone(),
                                cp.getProducts().getName(),
                                cp.getQuantity(),
                                cp.getCakeMessage(),
                                perso,
                                cp.getIsFinish()
                        )
                );
            }
        }

        /*lignes.sort((a, b) -> {
            boolean aUrgent = a.;
            boolean bUrgent = *//* check commande.isEmergency *//*;
            if (aUrgent && !bUrgent) return -1;
            if (!aUrgent && bUrgent) return 1;
            return 0;
        });*/

        int totalCakes = lignes.stream()
                .mapToInt(
                        DailyProductionForm.LineProduction
                                ::getQuantity)
                .sum();

        DailyProductionForm fiche = new DailyProductionForm();
        fiche.setDate(date);
        fiche.setTotalCakes(totalCakes);
        fiche.setOverload(totalCakes >= seuil);
        fiche.setLines(lignes);

        return fiche;
    }

    /**
     * Mark a product line as done.
     */
    public void markFinished(Long commandeId) {
        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commande", commandeId
                ));

        commande.getOrderedProducts()
                .forEach(cp -> cp.setIsFinish(true));

        commandeRepository.save(commande);

        boolean tousTermines = commande.getOrderedProducts()
                .stream()
                .allMatch(cp -> Boolean.TRUE.equals(cp.getIsFinish()));

        if (tousTermines
                && commande.getStatus() == CommandStatus.IN_PRODUCTION) {

            // Automatically move to PRETE
            commande.setStatus(CommandStatus.READY);
            commandeRepository.save(commande);

            // Notify client via WebSocket
            notificationService.createAndBroadcast(
                    NotificationTypes.ORDER_READY,
                    "Votre commande " + commande.getNumero()
                            + " est prête !",
                    commande,
                    commande.getClient()
            );

            log.info("Order {} automatically moved to PRETE " +
                    "— all products done", commande.getNumero());
        }
    }

    private int getSeuil() {
        return Integer.parseInt(
                settingsRepository
                        .findByCle("seuil_surcharge_production")
                        .map(Settings::getValue)
                        .orElse("5")
        );
    }
}
